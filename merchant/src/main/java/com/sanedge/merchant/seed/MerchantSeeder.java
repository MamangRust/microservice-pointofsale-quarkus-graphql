package com.sanedge.merchant.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;

import java.util.List;

/**
 * Seeds the merchant domain: merchants.
 * Order = 20.
 */
public class MerchantSeeder implements Seeder {

    @Override
    public String domain() {
        return "merchant";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        ctx.log().info("[seeder] Seeding merchant domain...");

        return seedMerchants(ctx)
                .invoke(() -> ctx.log().info("[seeder] Merchant domain seeded."));
    }

    private Uni<Void> seedMerchants(SeedContext ctx) {
        String sql = """
                INSERT INTO "pos_merchant"."merchants" ("user_id", "name", "description", "status")
                VALUES ($1, $2, $3, $4)
                ON CONFLICT DO NOTHING
                """;

        return ctx.pool().preparedQuery(sql)
                .execute(Tuple.of(1, "Merchant One", "First merchant store", "ACTIVE"))
                .replaceWithVoid()
                .invoke(() -> ctx.log().info("[seeder]   Merchants seeded: 1"));
    }
}
