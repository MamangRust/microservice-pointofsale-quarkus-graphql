package com.sanedge.transaction.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;

import java.util.List;

/**
 * Seeds the transaction domain: transactions.
 * Order = 50.
 */
public class TransactionSeeder implements Seeder {

    @Override
    public String domain() {
        return "transaction";
    }

    @Override
    public int order() {
        return 50;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        ctx.log().info("[seeder] Seeding transaction domain...");

        return seedTransactions(ctx)
                .invoke(() -> ctx.log().info("[seeder] Transaction domain seeded."));
    }

    private Uni<Void> seedTransactions(SeedContext ctx) {
        String sql = """
                INSERT INTO "pos_transaction"."transactions" ("order_id", "merchant_id", "payment_method", "amount", "change_amount", "status")
                VALUES ($1, $2, $3, $4, $5, $6)
                ON CONFLICT DO NOTHING
                """;

        record SeedTxn(int orderId, int merchantId, String method, int amount, int change, String status) {}

        List<SeedTxn> txns = List.of(
                new SeedTxn(1, 1, "cash", 5200000, 50000, "completed"),
                new SeedTxn(2, 1, "card", 15000000, 0, "completed"),
                new SeedTxn(3, 1, "cash", 150000, 0, "completed"),
                new SeedTxn(4, 1, "cash", 130000, 10000, "completed"),
                new SeedTxn(5, 1, "card", 200000, 0, "completed")
        );

        return Multi.createFrom().iterable(txns)
                .onItem().transformToUniAndConcatenate(t ->
                        ctx.pool().preparedQuery(sql)
                                .execute(Tuple.of(t.orderId(), t.merchantId(), t.method(), t.amount(), t.change(), t.status()))
                                .replaceWithVoid())
                .collect().in(java.util.ArrayList::new, java.util.List::add)
                .replaceWithVoid()
                .invoke(() -> ctx.log().infof("[seeder]   Transactions seeded: %d", txns.size()));
    }
}
