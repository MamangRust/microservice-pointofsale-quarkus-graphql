package com.sanedge.common.seed;

import io.vertx.mutiny.sqlclient.Pool;
import org.jboss.logging.Logger;

import java.util.function.Function;

/**
 * Reactive seed context passed to each {@link Seeder}.
 *
 * @param pool            Vert.x reactive PG pool (no Quarkus extensions needed)
 * @param log             JBoss Logger for seed output
 * @param passwordHasher  PBKDF2 password hashing function (plain, no CDI)
 */
public record SeedContext(
        Pool pool,
        Logger log,
        Function<String, String> passwordHasher
) {
}
