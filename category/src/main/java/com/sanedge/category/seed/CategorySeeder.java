package com.sanedge.category.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;

import java.util.List;

/**
 * Seeds the catalog domain: categories.
 * Order = 30.
 */
public class CategorySeeder implements Seeder {

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
        ctx.log().info("[seeder] Seeding catalog domain...");

        return seedCategories(ctx)
                .invoke(() -> ctx.log().info("[seeder] Catalog domain seeded."));
    }

    private Uni<Void> seedCategories(SeedContext ctx) {
        List<String[]> categories = List.of(
                new String[]{"Electronics", "Electronic devices and gadgets", "electronics"},
                new String[]{"Clothing", "Apparel and accessories", "clothing"},
                new String[]{"Food & Beverage", "Edible items and drinks", "food-beverage"},
                new String[]{"Home & Garden", "Home improvement and garden supplies", "home-garden"},
                new String[]{"Sports", "Sports equipment and accessories", "sports"},
                new String[]{"Books", "Books and publications", "books"},
                new String[]{"Toys", "Children's toys and games", "toys"},
                new String[]{"Health", "Health and wellness products", "health"},
                new String[]{"Automotive", "Car parts and accessories", "automotive"},
                new String[]{"Beauty", "Beauty and personal care", "beauty"}
        );

        String sql = """
                INSERT INTO "pos_catalog"."categories" ("name", "description", "slug_category")
                VALUES ($1, $2, $3)
                ON CONFLICT ("slug_category") DO NOTHING
                """;

        return Multi.createFrom().iterable(categories)
                .onItem().transformToUniAndConcatenate(cat ->
                        ctx.pool().preparedQuery(sql)
                                .execute(Tuple.of(cat[0], cat[1], cat[2]))
                                .replaceWithVoid())
                .collect().in(java.util.ArrayList::new, java.util.List::add)
                .replaceWithVoid()
                .invoke(() -> ctx.log().infof("[seeder]   Categories seeded: %d", categories.size()));
    }
}
