package com.sanedge.migration;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MigrationLifecycle {
    private static final Logger LOGGER = Logger.getLogger(MigrationLifecycle.class);

    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("Migrations completed successfully. Shutting down migration runner.");
        Quarkus.asyncExit();
    }
}
