package com.sanedge.statsbackfill;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * One-shot backfill job — reads existing orders and transactions from OLTP
 * PostgreSQL and writes them as outbox events so the stats-writer can
 * replay them into ClickHouse.
 */
public class BackfillLifecycle {

    private static final Logger LOG = Logger.getLogger(BackfillLifecycle.class);

    @Inject
    Pool pool;

    void onStart(@Observes StartupEvent event) {
        LOG.info("[backfill] ============================================");
        LOG.info("[backfill] Stats backfill starting...");
        LOG.info("[backfill] ============================================");

        String rawDomains = System.getenv("BACKFILL_DOMAINS");
        String rawFromDate = System.getenv("BACKFILL_FROM");
        final String domains = (rawDomains == null || rawDomains.isBlank()) ? "order,transaction" : rawDomains;
        final String fromDate = (rawFromDate == null || rawFromDate.isBlank()) ? "2024-01-01" : rawFromDate;

        List<String> domainList = Arrays.asList(domains.split(","));
        LOG.infof("[backfill] Domains: %s, From: %s", domains, fromDate);

        Uni<Void> pipeline = Uni.createFrom().voidItem();
        for (String domain : domainList) {
            final String d = domain.trim();
            pipeline = pipeline.chain(() -> backfillDomain(d, fromDate));
        }

        pipeline
                .invoke(() -> {
                    LOG.info("[backfill] ============================================");
                    LOG.info("[backfill] Backfill completed successfully!");
                    LOG.info("[backfill] ============================================");
                    Quarkus.asyncExit(0);
                })
                .onFailure().invoke(failure -> {
                    LOG.error("[backfill] ============================================");
                    LOG.error("[backfill] Backfill FAILED: " + failure.getMessage(), failure);
                    LOG.error("[backfill] ============================================");
                    Quarkus.asyncExit(1);
                })
                .subscribe().with(unused -> {}, failure -> {});
    }

    private Uni<Void> backfillDomain(String domain, String fromDate) {
        LOG.infof("[backfill] Backfilling domain: %s from %s", domain, fromDate);
        return switch (domain) {
            case "order" -> backfillOrders(fromDate);
            case "transaction" -> backfillTransactions(fromDate);
            default -> {
                LOG.warnf("[backfill] Unknown domain: %s — skipping", domain);
                yield Uni.createFrom().voidItem();
            }
        };
    }

    private Uni<Void> backfillOrders(String fromDate) {
        String selectSql = """
                SELECT o."order_id", o."merchant_id", o."cashier_id", o."total_price", o."created_at"
                FROM "pos_order"."orders" o
                WHERE o."created_at" >= $1::timestamp
                ORDER BY o."order_id"
                """;

        String insertSql = """
                INSERT INTO "pos_order"."outbox"
                ("aggregate_type", "aggregate_id", "topic", "payload", "status", "domain", "event_id")
                VALUES ($1, $2, $3, $4, 'PENDING', $5, $6)
                ON CONFLICT DO NOTHING
                """;

        return pool.preparedQuery(selectSql)
                .execute(Tuple.of(fromDate))
                .flatMap(result -> {
                    LOG.infof("[backfill] Found %d orders to backfill", result.size());
                    return Multi.createFrom().iterable(result)
                            .onItem().transformToUniAndConcatenate(row -> {
                                Long orderId = row.getLong("order_id");
                                String eventId = "backfill:order:" + orderId;

                                JsonObject payload = new JsonObject()
                                        .put("event_id", eventId)
                                        .put("order_id", orderId)
                                        .put("merchant_id", row.getLong("merchant_id"))
                                        .put("cashier_id", row.getLong("cashier_id"))
                                        .put("total_amount", row.getLong("total_price"))
                                        .put("status", "created")
                                        .put("occurred_at", row.getLocalDateTime("created_at").toString());

                                return pool.preparedQuery(insertSql)
                                        .execute(Tuple.of(
                                                "Order", String.valueOf(orderId),
                                                "stats.pos.order.event",
                                                com.sanedge.common.event.EventEnvelope
                                                        .withDefaults(payload, "order.created").encode(),
                                                "order", eventId))
                                        .replaceWithVoid();
                            })
                            .collect().in(java.util.ArrayList::new, java.util.List::add)
                            .replaceWithVoid();
                })
                .invoke(() -> LOG.info("[backfill] Orders backfill complete"));
    }

    private Uni<Void> backfillTransactions(String fromDate) {
        String selectSql = """
                SELECT t."transaction_id", t."order_id", t."merchant_id", t."payment_method",
                       t."amount", t."status", t."created_at"
                FROM "pos_transaction"."transactions" t
                WHERE t."created_at" >= $1::timestamp
                ORDER BY t."transaction_id"
                """;

        String insertSql = """
                INSERT INTO "pos_order"."outbox"
                ("aggregate_type", "aggregate_id", "topic", "payload", "status", "domain", "event_id")
                VALUES ($1, $2, $3, $4, 'PENDING', $5, $6)
                ON CONFLICT DO NOTHING
                """;

        return pool.preparedQuery(selectSql)
                .execute(Tuple.of(fromDate))
                .flatMap(result -> {
                    LOG.infof("[backfill] Found %d transactions to backfill", result.size());
                    return Multi.createFrom().iterable(result)
                            .onItem().transformToUniAndConcatenate(row -> {
                                Long txId = row.getLong("transaction_id");
                                String eventId = "backfill:transaction:" + txId;

                                JsonObject payload = new JsonObject()
                                        .put("event_id", eventId)
                                        .put("transaction_id", txId)
                                        .put("order_id", row.getLong("order_id"))
                                        .put("merchant_id", row.getLong("merchant_id"))
                                        .put("payment_method", row.getString("payment_method"))
                                        .put("amount", row.getLong("amount"))
                                        .put("status", row.getString("status"))
                                        .put("occurred_at", row.getLocalDateTime("created_at").toString());

                                return pool.preparedQuery(insertSql)
                                        .execute(Tuple.of(
                                                "Transaction", String.valueOf(txId),
                                                "stats.pos.transaction.event",
                                                com.sanedge.common.event.EventEnvelope
                                                        .withDefaults(payload, "transaction.created").encode(),
                                                "transaction", eventId))
                                        .replaceWithVoid();
                            })
                            .collect().in(java.util.ArrayList::new, java.util.List::add)
                            .replaceWithVoid();
                })
                .invoke(() -> LOG.info("[backfill] Transactions backfill complete"));
    }
}
