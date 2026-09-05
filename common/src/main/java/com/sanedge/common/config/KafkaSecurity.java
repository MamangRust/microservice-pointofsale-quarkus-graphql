package com.sanedge.common.config;

/**
 * Kafka broker security settings (Fase 14 — K-6).
 *
 * <p>Built from environment variables so the same artifacts run against a
 * dev broker ({@code PLAINTEXT}) or a production broker
 * ({@code SASL_SSL} + SCRAM-SHA-256 + TLS) without code changes:
 *
 * <ul>
 *   <li>{@code KAFKA_SECURITY_PROTOCOL} — {@code PLAINTEXT} (default) or e.g. {@code SASL_SSL}</li>
 *   <li>{@code KAFKA_SASL_MECHANISM} — {@code SCRAM-SHA-256} (default)</li>
 *   <li>{@code KAFKA_SASL_JAAS_CONFIG} — full JAAS config, e.g.
 *       {@code org.apache.kafka.common.security.scram.ScramLoginModule required username="app" password="secret";}</li>
 *   <li>{@code KAFKA_SSL_TRUSTSTORE_LOCATION} / {@code KAFKA_SSL_TRUSTSTORE_PASSWORD} /
 *       {@code KAFKA_SSL_TRUSTSTORE_TYPE} (default {@code JKS})</li>
 *   <li>{@code KAFKA_SSL_KEYSTORE_LOCATION} / {@code KAFKA_SSL_KEYSTORE_PASSWORD} /
 *       {@code KAFKA_SSL_KEY_PASSWORD} — only for mutual TLS</li>
 * </ul>
 *
 * @param protocol          {@code security.protocol} (default {@code PLAINTEXT})
 * @param saslMechanism     {@code sasl.mechanism}
 * @param saslJaasConfig    {@code sasl.jaas.config} (optional; required when protocol contains SASL)
 * @param sslTruststoreLocation truststore path for verifying the broker
 * @param sslTruststorePassword truststore password
 * @param sslTruststoreType truststore type (default {@code JKS})
 * @param sslKeystoreLocation client keystore (mTLS only)
 * @param sslKeystorePassword client keystore password (mTLS only)
 * @param sslKeyPassword    client key password (mTLS only)
 */
public record KafkaSecurity(
        String protocol,
        String saslMechanism,
        String saslJaasConfig,
        String sslTruststoreLocation,
        String sslTruststorePassword,
        String sslTruststoreType,
        String sslKeystoreLocation,
        String sslKeystorePassword,
        String sslKeyPassword) {

    public static final String DEFAULT_PROTOCOL = "PLAINTEXT";
    public static final String DEFAULT_SASL_MECHANISM = "SCRAM-SHA-256";

    /** Unsecured dev broker. */
    public static KafkaSecurity plaintext() {
        return new KafkaSecurity(DEFAULT_PROTOCOL, null, null, null, null, null, null, null, null);
    }

    /** Reads the {@code KAFKA_*} environment variables (production override). */
    public static KafkaSecurity fromEnv() {
        return new KafkaSecurity(
                env("KAFKA_SECURITY_PROTOCOL", DEFAULT_PROTOCOL),
                env("KAFKA_SASL_MECHANISM", DEFAULT_SASL_MECHANISM),
                env("KAFKA_SASL_JAAS_CONFIG", ""),
                env("KAFKA_SSL_TRUSTSTORE_LOCATION", ""),
                env("KAFKA_SSL_TRUSTSTORE_PASSWORD", ""),
                env("KAFKA_SSL_TRUSTSTORE_TYPE", "JKS"),
                env("KAFKA_SSL_KEYSTORE_LOCATION", ""),
                env("KAFKA_SSL_KEYSTORE_PASSWORD", ""),
                env("KAFKA_SSL_KEY_PASSWORD", ""));
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public boolean isPlaintext() {
        return protocol == null || protocol.isBlank() || DEFAULT_PROTOCOL.equalsIgnoreCase(protocol);
    }

    public boolean usesSasl() {
        return !isPlaintext() && protocol.toUpperCase().contains("SASL");
    }

    public boolean usesSsl() {
        return !isPlaintext() && protocol.toUpperCase().contains("SSL");
    }
}
