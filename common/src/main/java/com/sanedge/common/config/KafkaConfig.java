package com.sanedge.common.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Single source of truth for Kafka client configuration (Fase 14 — K-6, K-7).
 *
 * <p>Centralises the durability and security settings so the domain services
 * ({@code KafkaService} in auth/merchant/order/transaction) and the email
 * worker share identical producer/consumer semantics:
 *
 * <ul>
 *   <li><b>Durability</b> — {@code acks=all} (wait for all in-sync replicas) and
 *       {@code enable.idempotence=true} (no duplicates when the broker restarts).</li>
 *   <li><b>Security</b> — optional SASL/SCRAM + TLS applied only when the
 *       {@code security.protocol} is not {@code PLAINTEXT} (see {@link KafkaSecurity}).</li>
 * </ul>
 */
public final class KafkaConfig {

    public static final String DEFAULT_ACKS = "all";
    public static final String DEFAULT_BOOTSTRAP = "localhost:9092";

    private KafkaConfig() {
    }

    /**
     * Producer config: String key/value serializers + durability
     * ({@code acks=all}, {@code enable.idempotence=true}) + optional security.
     */
    public static Map<String, String> producer(String bootstrapServers, String acks, boolean idempotence,
            KafkaSecurity security) {
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", bootstrapServers);
        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("acks", acks);
        config.put("enable.idempotence", String.valueOf(idempotence));
        applySecurity(config, security);
        return config;
    }

    /**
     * Consumer config: String key / JsonObject value deserializers, group id +
     * earliest offset reset + optional security. Callers add their own group.id.
     */
    public static Map<String, String> consumer(String bootstrapServers, String groupId, KafkaSecurity security) {
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", bootstrapServers);
        config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("value.deserializer", "io.vertx.kafka.client.serialization.JsonObjectDeserializer");
        config.put("group.id", groupId);
        config.put("auto.offset.reset", "earliest");
        applySecurity(config, security);
        return config;
    }

    /**
     * Applies {@code security.protocol}, {@code sasl.*} and {@code ssl.*} keys
     * when the protocol is not {@code PLAINTEXT}. No-op for plaintext brokers.
     */
    public static void applySecurity(Map<String, String> config, KafkaSecurity security) {
        if (security == null || security.isPlaintext()) {
            return;
        }
        config.put("security.protocol", security.protocol());
        if (security.usesSasl()) {
            config.put("sasl.mechanism", security.saslMechanism());
            if (security.saslJaasConfig() != null && !security.saslJaasConfig().isBlank()) {
                config.put("sasl.jaas.config", security.saslJaasConfig());
            }
        }
        if (security.usesSsl()) {
            putIfNotBlank(config, "ssl.truststore.location", security.sslTruststoreLocation());
            putIfNotBlank(config, "ssl.truststore.password", security.sslTruststorePassword());
            putIfNotBlank(config, "ssl.truststore.type", security.sslTruststoreType());
            putIfNotBlank(config, "ssl.keystore.location", security.sslKeystoreLocation());
            putIfNotBlank(config, "ssl.keystore.password", security.sslKeystorePassword());
            putIfNotBlank(config, "ssl.key.password", security.sslKeyPassword());
        }
    }

    private static void putIfNotBlank(Map<String, String> config, String key, String value) {
        if (value != null && !value.isBlank()) {
            config.put(key, value);
        }
    }
}
