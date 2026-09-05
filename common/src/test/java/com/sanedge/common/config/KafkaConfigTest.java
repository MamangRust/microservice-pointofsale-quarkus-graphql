package com.sanedge.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("KafkaConfig (Fase 14 — durability & security)")
class KafkaConfigTest {

    @Nested
    @DisplayName("producer config")
    class ProducerTests {

        @Test
        void producer_setsDurabilityDefaults() {
            Map<String, String> config = KafkaConfig.producer(
                    "localhost:9092", KafkaConfig.DEFAULT_ACKS, true, KafkaSecurity.plaintext());

            assertEquals("localhost:9092", config.get("bootstrap.servers"));
            assertEquals("all", config.get("acks"));
            assertEquals("true", config.get("enable.idempotence"));
            assertEquals("org.apache.kafka.common.serialization.StringSerializer", config.get("key.serializer"));
            assertEquals("org.apache.kafka.common.serialization.StringSerializer", config.get("value.serializer"));
            // plaintext → no security keys
            assertFalse(config.containsKey("security.protocol"));
        }

        @Test
        void producer_idempotenceDisabledWhenFalse() {
            Map<String, String> config = KafkaConfig.producer(
                    "b:9092", "1", false, KafkaSecurity.plaintext());
            assertEquals("false", config.get("enable.idempotence"));
        }
    }

    @Nested
    @DisplayName("consumer config")
    class ConsumerTests {

        @Test
        void consumer_setsGroupAndDeserializers() {
            Map<String, String> config = KafkaConfig.consumer("b:9092", "email-service-group", KafkaSecurity.plaintext());

            assertEquals("b:9092", config.get("bootstrap.servers"));
            assertEquals("email-service-group", config.get("group.id"));
            assertEquals("earliest", config.get("auto.offset.reset"));
            assertEquals("org.apache.kafka.common.serialization.StringDeserializer", config.get("key.deserializer"));
            assertEquals("io.vertx.kafka.client.serialization.JsonObjectDeserializer", config.get("value.deserializer"));
            assertFalse(config.containsKey("security.protocol"));
        }
    }

    @Nested
    @DisplayName("SASL/SCRAM + TLS")
    class SecurityTests {

        @Test
        void applySecurity_saslSsl_appliesAllKeys() {
            KafkaSecurity security = new KafkaSecurity(
                    "SASL_SSL",
                    "SCRAM-SHA-256",
                    "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"app\" password=\"secret\";",
                    "/certs/kafka.truststore.jks",
                    "trustpass",
                    "JKS",
                    "/certs/kafka.keystore.jks",
                    "keypass",
                    "keypass");

            Map<String, String> config = KafkaConfig.producer("b:9092", "all", true, security);

            assertEquals("SASL_SSL", config.get("security.protocol"));
            assertEquals("SCRAM-SHA-256", config.get("sasl.mechanism"));
            assertTrue(config.get("sasl.jaas.config").contains("ScramLoginModule"));
            assertEquals("/certs/kafka.truststore.jks", config.get("ssl.truststore.location"));
            assertEquals("trustpass", config.get("ssl.truststore.password"));
            assertEquals("JKS", config.get("ssl.truststore.type"));
            assertEquals("/certs/kafka.keystore.jks", config.get("ssl.keystore.location"));
            assertEquals("keypass", config.get("ssl.keystore.password"));
            assertEquals("keypass", config.get("ssl.key.password"));
        }

        @Test
        void applySecurity_saslPlaintext_noSslKeys() {
            KafkaSecurity security = new KafkaSecurity(
                    "SASL_PLAINTEXT", "SCRAM-SHA-256",
                    "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"app\" password=\"secret\";",
                    null, null, null, null, null, null);

            Map<String, String> config = KafkaConfig.consumer("b:9092", "g", security);

            assertEquals("SASL_PLAINTEXT", config.get("security.protocol"));
            assertEquals("SCRAM-SHA-256", config.get("sasl.mechanism"));
            assertFalse(config.containsKey("ssl.truststore.location"));
        }

        @Test
        void plaintext_neverAddsSecurity() {
            Map<String, String> config = KafkaConfig.producer("b:9092", "all", true, KafkaSecurity.plaintext());
            assertFalse(config.containsKey("security.protocol"));
            assertFalse(config.containsKey("sasl.mechanism"));
        }

        @Test
        void fromEnv_defaultsToPlaintext() {
            KafkaSecurity security = KafkaSecurity.fromEnv();
            assertEquals("PLAINTEXT", security.protocol());
            assertTrue(security.isPlaintext());
            assertFalse(security.usesSasl());
            assertFalse(security.usesSsl());
        }
    }
}
