package com.sanedge.order.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;

import java.util.List;

/**
 * Seeds the order domain: orders + order_items.
 * Order = 40.
 * <p>
 * Orders are seeded with {@code created_at} in January 2024 for deterministic stats.
 */
public class OrderSeeder implements Seeder {

    @Override
    public String domain() {
        return "order";
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        ctx.log().info("[seeder] Seeding order domain...");

        return seedOrders(ctx)
                .chain(() -> seedOrderItems(ctx))
                .invoke(() -> ctx.log().info("[seeder] Order domain seeded."));
    }

    private Uni<Void> seedOrders(SeedContext ctx) {
        String sql = """
                INSERT INTO "pos_order"."orders" ("merchant_id", "cashier_id", "total_price", "created_at")
                VALUES ($1, $2, $3, $4::timestamp)
                ON CONFLICT DO NOTHING
                """;

        record SeedOrder(int merchantId, int cashierId, long totalPrice, String createdAt) {}

        List<SeedOrder> orders = List.of(
                new SeedOrder(1, 1, 5150000L, "2024-01-05 10:00:00"),
                new SeedOrder(1, 1, 15000000L, "2024-01-10 14:30:00"),
                new SeedOrder(1, 1, 150000L, "2024-01-15 09:15:00"),
                new SeedOrder(1, 1, 120000L, "2024-01-20 16:45:00"),
                new SeedOrder(1, 1, 200000L, "2024-01-25 11:20:00")
        );

        return Multi.createFrom().iterable(orders)
                .onItem().transformToUniAndConcatenate(o ->
                        ctx.pool().preparedQuery(sql)
                                .execute(Tuple.of(o.merchantId(), o.cashierId(), o.totalPrice(), o.createdAt()))
                                .replaceWithVoid())
                .collect().in(java.util.ArrayList::new, java.util.List::add)
                .replaceWithVoid()
                .invoke(() -> ctx.log().infof("[seeder]   Orders seeded: %d", orders.size()));
    }

    private Uni<Void> seedOrderItems(SeedContext ctx) {
        String sql = """
                INSERT INTO "pos_order"."order_items" ("order_id", "product_id", "quantity", "price")
                VALUES ($1, $2, $3, $4)
                ON CONFLICT DO NOTHING
                """;

        record SeedItem(int orderId, int productId, int quantity, int price) {}

        List<SeedItem> items = List.of(
                new SeedItem(1, 1, 1, 5000000),
                new SeedItem(1, 3, 1, 150000),
                new SeedItem(2, 2, 1, 15000000),
                new SeedItem(3, 4, 1, 150000),
                new SeedItem(4, 6, 1, 120000),
                new SeedItem(5, 5, 1, 200000)
        );

        return Multi.createFrom().iterable(items)
                .onItem().transformToUniAndConcatenate(i ->
                        ctx.pool().preparedQuery(sql)
                                .execute(Tuple.of(i.orderId(), i.productId(), i.quantity(), i.price()))
                                .replaceWithVoid())
                .collect().in(java.util.ArrayList::new, java.util.List::add)
                .replaceWithVoid()
                .invoke(() -> ctx.log().infof("[seeder]   Order items seeded: %d", items.size()));
    }
}
