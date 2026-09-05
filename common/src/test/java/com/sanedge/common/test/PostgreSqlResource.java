package com.sanedge.common.test;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.dockerclient.DockerClientProviderStrategy;
import org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy;
import org.testcontainers.utility.DockerImageName;

public class PostgreSqlResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger log = LoggerFactory.getLogger(PostgreSqlResource.class);
    private GenericContainer<?> postgres;

    // Set Docker host BEFORE any Testcontainers code loads (static initializer runs at class load time)
    static {
        System.setProperty("docker.host", "unix:///var/run/docker.sock");
    }

    @Override
    public Map<String, String> start() {

        try {
            // Verify Docker is actually reachable before attempting container start
            DockerClientFactory.instance().client();

            postgres = new GenericContainer<>(DockerImageName.parse("postgres:15-alpine"))
                    .withEnv("POSTGRES_USER", "test")
                    .withEnv("POSTGRES_PASSWORD", "test")
                    .withEnv("POSTGRES_DB", "test")
                    .withExposedPorts(5432)
                    .waitingFor(Wait.forListeningPort());

            postgres.start();

            int port = postgres.getMappedPort(5432);
            String host = postgres.getHost();

            return Map.of(
                    "quarkus.datasource.username", "test",
                    "quarkus.datasource.password", "test",
                    "quarkus.datasource.reactive.url", "postgresql://" + host + ":" + port + "/test",
                    "quarkus.datasource.jdbc.url", "jdbc:postgresql://" + host + ":" + port + "/test"
            );
        } catch (Exception e) {
            log.warn("Docker/Testcontainers tidak tersedia. Skipping test yang bergantung pada PostgreSqlResource. Error: {}", e.getMessage());
            // Return empty config - test harus handle dengan Assumptions.assumeTrue()
            return Map.of(
                    "docker.available", "false"
            );
        }
    }

    @Override
    public void stop() {
        if (postgres != null) {
            try {
                postgres.stop();
            } catch (Exception e) {
                log.warn("Gagal stop container: {}", e.getMessage());
            }
        }
    }

    public static boolean isDockerAvailable() {
        try {
            System.setProperty("docker.host", "unix:///var/run/docker.sock");
            DockerClientFactory.instance().client();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
