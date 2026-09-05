package com.sanedge.common.clickhouse;

import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * CDI producer for {@link ClickHouseClient}.
 * Reads ClickHouse connection properties from Quarkus config.
 */
@ApplicationScoped
public class ClickHouseProducer {

    @ConfigProperty(name = "clickhouse.host", defaultValue = "localhost")
    String host;

    @ConfigProperty(name = "clickhouse.http-port", defaultValue = "8123")
    int httpPort;

    @ConfigProperty(name = "clickhouse.database", defaultValue = "pos_stats")
    String database;

    @ConfigProperty(name = "clickhouse.username", defaultValue = "default")
    String username;

    @ConfigProperty(name = "clickhouse.password", defaultValue = "none")
    String password;

    @Produces
    @ApplicationScoped
    public ClickHouseClient produce(Vertx vertx) {
        return new ClickHouseClient(vertx, host, httpPort, database, username, password);
    }
}
