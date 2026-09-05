package com.sanedge.seeder;

import com.sanedge.common.seed.PasswordUtil;
import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;
import io.quarkus.runtime.Quarkus;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.event.Observes;
import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.sqlclient.Pool;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * Seeder orchestrator — observes {@link StartupEvent}, loads all {@link Seeder}
 * implementations via {@link ServiceLoader}, filters by {@code SEED_DOMAINS}
 * environment variable, runs them sequentially in order, then exits Quarkus.
 * <p>
 * Clean runner: no gRPC, no Hibernate, no Flyway, no HTTP — just CDI + reactive PG pool.
 */
public class SeederLifecycle {

    private static final Logger LOG = Logger.getLogger(SeederLifecycle.class);

    @Inject
    Pool pool;

    void onStart(@Observes StartupEvent event) {
        LOG.info("[seeder] ============================================");
        LOG.info("[seeder] Seeder orchestrator starting...");
        LOG.info("[seeder] ============================================");

        // Load all seeders via ServiceLoader
        List<Seeder> seeders = ServiceLoader.load(Seeder.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .sorted(Comparator.comparingInt(Seeder::order))
                .collect(Collectors.toList());

        LOG.infof("[seeder] Found %d seeder(s): %s",
                seeders.size(),
                seeders.stream().map(s -> s.domain() + "(" + s.order() + ")").collect(Collectors.joining(", ")));

        // Filter by SEED_DOMAINS if set
        String seedDomains = System.getenv("SEED_DOMAINS");
        if (seedDomains != null && !seedDomains.isBlank()) {
            List<String> allowed = Arrays.asList(seedDomains.split(","));
            seeders = seeders.stream()
                    .filter(s -> allowed.contains(s.domain()))
                    .collect(Collectors.toList());
            LOG.infof("[seeder] Filtered to %d seeder(s) by SEED_DOMAINS=%s: %s",
                    seeders.size(), seedDomains,
                    seeders.stream().map(Seeder::domain).collect(Collectors.joining(", ")));
        }

        if (seeders.isEmpty()) {
            LOG.info("[seeder] No seeders to run. Exiting.");
            Quarkus.asyncExit(0);
            return;
        }

        // Build seed context
        PasswordUtil passwordUtil = new PasswordUtil();
        SeedContext ctx = new SeedContext(pool, LOG, passwordUtil::hashPassword);

        // Run seeders sequentially
        Uni<Void> pipeline = Uni.createFrom().voidItem();
        for (Seeder seeder : seeders) {
            final String domain = seeder.domain();
            final int order = seeder.order();
            pipeline = pipeline.chain(() -> {
                LOG.infof("[seeder] Running seeder: %s (order=%d)", domain, order);
                return seeder.seed(ctx);
            });
        }

        pipeline
                .invoke(() -> {
                    LOG.info("[seeder] ============================================");
                    LOG.info("[seeder] All seeders completed successfully!");
                    LOG.info("[seeder] ============================================");
                    Quarkus.asyncExit(0);
                })
                .onFailure().invoke(failure -> {
                    LOG.error("[seeder] ============================================");
                    LOG.error("[seeder] Seeder FAILED: " + failure.getMessage(), failure);
                    LOG.error("[seeder] ============================================");
                    Quarkus.asyncExit(1);
                })
                .subscribe().with(unused -> {}, failure -> {});
    }
}
