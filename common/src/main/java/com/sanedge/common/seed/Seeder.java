package com.sanedge.common.seed;

import io.smallrye.mutiny.Uni;

/**
 * SPI contract for per-domain seeders.
 * <p>
 * Each domain module provides an implementation registered via
 * {@code META-INF/services/com.sanedge.common.seed.Seeder}.
 * The seeder orchestrator ({@code SeederLifecycle}) loads all implementations
 * via {@link java.util.ServiceLoader}, sorts by {@link #order()}, optionally
 * filters by the {@code SEED_DOMAINS} environment variable, and runs them
 * sequentially.
 * <p>
 * Implementations must be plain classes with a no-arg constructor (no CDI).
 */
public interface Seeder {

    /**
     * Domain identifier used for selective seeding via the {@code SEED_DOMAINS}
     * environment variable (comma-separated list).
     */
    String domain();

    /**
     * Execution order — lower values run first.
     * <p>
     * Canonical ordering: identity (10) → merchant (20) → catalog (30) →
     * order (40) → transaction (50).
     */
    default int order() {
        return 100;
    }

    /**
     * Seed the domain. Implementations must be idempotent — running multiple
     * times must not create duplicate data (use {@code ON CONFLICT} clauses).
     *
     * @param ctx reactive seed context (pool, logger, password hasher)
     * @return Uni that completes when seeding is done
     */
    Uni<Void> seed(SeedContext ctx);
}
