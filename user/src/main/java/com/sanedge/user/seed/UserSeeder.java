package com.sanedge.user.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;

import java.util.List;

/**
 * Seeds the identity domain: roles, users, and user_roles junction table.
 * Order = 10 (runs first).
 */
public class UserSeeder implements Seeder {

    @Override
    public String domain() {
        return "identity";
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        ctx.log().info("[seeder] Seeding identity domain...");

        return seedRoles(ctx)
                .chain(() -> seedUsers(ctx))
                .chain(() -> seedUserRoles(ctx))
                .invoke(() -> ctx.log().info("[seeder] Identity domain seeded."));
    }

    private Uni<Void> seedRoles(SeedContext ctx) {
        List<String> roles = List.of("admin", "cashier", "staff");

        String sql = """
                INSERT INTO "pos_identity"."roles" ("role_name")
                VALUES ($1)
                ON CONFLICT ("role_name") DO NOTHING
                """;

        return Multi.createFrom().iterable(roles)
                .onItem().transformToUniAndConcatenate(role ->
                        ctx.pool().preparedQuery(sql)
                                .execute(Tuple.of(role))
                                .replaceWithVoid())
                .collect().in(java.util.ArrayList::new, java.util.List::add)
                .replaceWithVoid()
                .invoke(() -> ctx.log().infof("[seeder]   Roles seeded: %d", roles.size()));
    }

    private Uni<Void> seedUsers(SeedContext ctx) {
        String hashedPassword = ctx.passwordHasher().apply("password123");

        String sql = """
                INSERT INTO "pos_identity"."users" ("firstname", "lastname", "username", "email", "password")
                VALUES ($1, $2, $3, $4, $5)
                ON CONFLICT ("username") DO NOTHING
                """;

        record SeedUser(String firstname, String lastname, String username, String email) {}

        List<SeedUser> users = List.of(
                new SeedUser("Admin", "User", "admin", "admin@example.com"),
                new SeedUser("Cashier", "One", "cashier1", "cashier1@example.com"),
                new SeedUser("Staff", "One", "staff1", "staff1@example.com")
        );

        return Multi.createFrom().iterable(users)
                .onItem().transformToUniAndConcatenate(user ->
                        ctx.pool().preparedQuery(sql)
                                .execute(Tuple.tuple(java.util.List.of(user.firstname(), user.lastname(), user.username(), user.email(), hashedPassword)))
                                .replaceWithVoid())
                .collect().in(java.util.ArrayList::new, java.util.List::add)
                .replaceWithVoid()
                .invoke(() -> ctx.log().infof("[seeder]   Users seeded: %d", users.size()));
    }

    private Uni<Void> seedUserRoles(SeedContext ctx) {
        String sql = """
                INSERT INTO "pos_identity"."user_roles" ("user_id", "role_id")
                SELECT u."id", r."id"
                FROM "pos_identity"."users" u, "pos_identity"."roles" r
                WHERE u."username" = $1 AND r."role_name" = $2
                ON CONFLICT ("user_id", "role_id") DO NOTHING
                """;

        record UserRole(String username, String roleName) {}

        List<UserRole> userRoles = List.of(
                new UserRole("admin", "admin"),
                new UserRole("cashier1", "cashier"),
                new UserRole("staff1", "staff")
        );

        return Multi.createFrom().iterable(userRoles)
                .onItem().transformToUniAndConcatenate(ur ->
                        ctx.pool().preparedQuery(sql)
                                .execute(Tuple.of(ur.username(), ur.roleName()))
                                .replaceWithVoid())
                .collect().in(java.util.ArrayList::new, java.util.List::add)
                .replaceWithVoid()
                .invoke(() -> ctx.log().infof("[seeder]   User-roles seeded: %d", userRoles.size()));
    }
}
