package com.sanedge.cashier.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;

import java.util.List;

/**
 * Seeds the cashier domain: cashiers (FK to users + merchants).
 * Order = 20.
 */
public class CashierSeeder implements Seeder {

    @Override
    public String domain() {
        return "cashier";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        ctx.log().info("[seeder] Seeding cashier domain...");

        return seedCashiers(ctx)
                .invoke(() -> ctx.log().info("[seeder] Cashier domain seeded."));
    }

    private Uni<Void> seedCashiers(SeedContext ctx) {
        String sql = """
                INSERT INTO "pos_merchant"."cashiers" ("merchant_id", "user_id", "name")
                VALUES ($1, $2, $3)
                ON CONFLICT DO NOTHING
                """;

        return ctx.pool().preparedQuery(sql)
                .execute(Tuple.of(1, 2, "Cashier One"))
                .replaceWithVoid()
                .invoke(() -> ctx.log().info("[seeder]   Cashiers seeded: 1"));
    }
}
