package com.sanedge.product.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;

import java.util.List;

/**
 * Seeds the catalog domain: products (FK to categories + merchants).
 * Order = 30.
 */
public class ProductSeeder implements Seeder {

    @Override
    public String domain() {
        return "catalog";
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        ctx.log().info("[seeder] Seeding product domain...");

        return seedProducts(ctx)
                .invoke(() -> ctx.log().info("[seeder] Product domain seeded."));
    }

    private Uni<Void> seedProducts(SeedContext ctx) {
        String sql = """
                INSERT INTO "pos_catalog"."products" ("merchant_id", "category_id", "name", "description", "price", "count_in_stock", "slug_product")
                VALUES ($1, $2, $3, $4, $5, $6, $7)
                ON CONFLICT ("slug_product") DO NOTHING
                """;

        record SeedProduct(int merchantId, int categoryId, String name, String desc, int price, int stock, String slug) {}

        List<SeedProduct> products = List.of(
                new SeedProduct(1, 1, "Smartphone X1", "Latest smartphone", 5000000, 50, "smartphone-x1"),
                new SeedProduct(1, 1, "Laptop Pro", "Professional laptop", 15000000, 20, "laptop-pro"),
                new SeedProduct(1, 2, "T-Shirt Basic", "Cotton t-shirt", 150000, 100, "tshirt-basic"),
                new SeedProduct(1, 3, "Coffee Beans 1kg", "Premium coffee beans", 120000, 200, "coffee-beans-1kg"),
                new SeedProduct(1, 4, "Garden Hose 10m", "Durable garden hose", 250000, 30, "garden-hose-10m"),
                new SeedProduct(1, 5, "Yoga Mat", "Non-slip yoga mat", 200000, 40, "yoga-mat"),
                new SeedProduct(1, 6, "Java Programming", "Java programming book", 180000, 25, "java-programming"),
                new SeedProduct(1, 7, "Building Blocks", "Creative building blocks", 95000, 60, "building-blocks"),
                new SeedProduct(1, 8, "Vitamin C 1000mg", "Immune support supplement", 75000, 80, "vitamin-c-1000mg"),
                new SeedProduct(1, 9, "Car Air Freshener", "Long-lasting air freshener", 35000, 150, "car-air-freshener")
        );

        return Multi.createFrom().iterable(products)
                .onItem().transformToUniAndConcatenate(p ->
                        ctx.pool().preparedQuery(sql)
                                .execute(Tuple.tuple(java.util.List.of(p.merchantId(), p.categoryId(), p.name(), p.desc(), p.price(), p.stock(), p.slug())))
                                .replaceWithVoid())
                .collect().in(java.util.ArrayList::new, java.util.List::add)
                .replaceWithVoid()
                .invoke(() -> ctx.log().infof("[seeder]   Products seeded: %d", products.size()));
    }
}
