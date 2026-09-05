package com.sanedge.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.chaos.ChaosManager;
import com.sanedge.common.chaos.ChaosPolicy;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import io.vertx.kafka.client.producer.RecordMetadata;

@ExtendWith(MockitoExtension.class)
class KafkaServiceTest {

    @Mock KafkaProducer<String, String> producer;
    @Mock ChaosManager chaosManager;

    private KafkaService service;
    private OpenTelemetry otel;

    private static final String TOPIC = "email-service-topic-transaction-create";

    @BeforeEach
    void setUp() {
        otel = OpenTelemetrySdk.builder()
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .setTracerProvider(SdkTracerProvider.builder().build())
                .build();

        service = new KafkaService();
        service.producer = producer;
        service.chaosManager = chaosManager;
        service.tracer = otel.getTracer("test");
        service.propagator = otel.getPropagators().getTextMapPropagator();

        lenient().when(chaosManager.evaluate(eq("kafka"), anyString())).thenReturn(null);
    }

    private JsonObject payload() {
        return new JsonObject().put("email", "a@b.c").put("subject", "s").put("body", "b");
    }

    @Nested
    @DisplayName("normal send")
    class NormalSendTests {

        @Test
        @SuppressWarnings("unchecked")
        void sendsRecordWithTraceparentHeaderWhenSpanActive() {
            when(producer.send(any(KafkaProducerRecord.class)))
                    .thenReturn(Future.succeededFuture(
                            new RecordMetadata(1L, 0, System.currentTimeMillis(), TOPIC)));

            Span parent = otel.getTracer("test").spanBuilder("parent").startSpan();
            try (Scope scope = parent.makeCurrent()) {
                service.sendMessage(TOPIC, "k1", payload()).await().indefinitely();
            } finally {
                parent.end();
            }

            ArgumentCaptor<KafkaProducerRecord<String, String>> captor =
                    ArgumentCaptor.forClass((Class) KafkaProducerRecord.class);
            verify(producer).send(captor.capture());

            KafkaProducerRecord<String, String> record = captor.getValue();
            assertThat(record.topic()).isEqualTo(TOPIC);
            boolean hasTraceparent = record.headers().stream()
                    .anyMatch(h -> "traceparent".equals(h.key()));
            assertThat(hasTraceparent).isTrue();
        }

        @Test
        @SuppressWarnings("unchecked")
        void sendsStandardEventEnvelope() {
            // Phase 2 (event contract): every published record carries
            // event_id, schema_version, event_type and occurred_at.
            when(producer.send(any(KafkaProducerRecord.class)))
                    .thenReturn(Future.succeededFuture(
                            new RecordMetadata(1L, 0, System.currentTimeMillis(), TOPIC)));

            service.sendMessage(TOPIC, "k1", payload()).await().indefinitely();

            ArgumentCaptor<KafkaProducerRecord<String, String>> captor =
                    ArgumentCaptor.forClass((Class) KafkaProducerRecord.class);
            verify(producer).send(captor.capture());

            JsonObject sent = new JsonObject(captor.getValue().value());
            assertThat(sent.getString("event_id")).isNotBlank();
            assertThat(sent.getInteger("schema_version")).isEqualTo(1);
            assertThat(sent.getString("event_type")).isEqualTo(TOPIC);
            assertThat(sent.getString("occurred_at")).isNotBlank();
            assertThat(sent.getString("email")).isEqualTo("a@b.c");
        }

        @Test
        @SuppressWarnings("unchecked")
        void preservesExistingEventId() {
            // Outbox replay / retry must keep a stable event_id.
            when(producer.send(any(KafkaProducerRecord.class)))
                    .thenReturn(Future.succeededFuture(
                            new RecordMetadata(1L, 0, System.currentTimeMillis(), TOPIC)));

            JsonObject withId = payload().put("event_id", "fixed-id");
            service.sendMessage(TOPIC, "k1", withId).await().indefinitely();

            ArgumentCaptor<KafkaProducerRecord<String, String>> captor =
                    ArgumentCaptor.forClass((Class) KafkaProducerRecord.class);
            verify(producer).send(captor.capture());

            JsonObject sent = new JsonObject(captor.getValue().value());
            assertThat(sent.getString("event_id")).isEqualTo("fixed-id");
        }
    }

    @Nested
    @DisplayName("chaos injection")
    class ChaosTests {

        @Test
        void dropMessage_completesWithoutSending() {
            ChaosPolicy policy = ChaosPolicy.builder()
                    .name("drop").type("kafka").target(TOPIC)
                    .enabled(true).errorChance(1.0).dropMessage(true).build();
            when(chaosManager.evaluate("kafka", TOPIC)).thenReturn(policy);

            service.sendMessage(TOPIC, "k1", payload()).await().indefinitely();

            verify(producer, never()).send(any());
        }

        @Test
        void rejectMessage_failsWithPolicyError() {
            ChaosPolicy policy = ChaosPolicy.builder()
                    .name("reject").type("kafka").target(TOPIC)
                    .enabled(true).errorChance(1.0).rejectMessage(true)
                    .errorMessage("Simulated Kafka drop/reject error").build();
            when(chaosManager.evaluate("kafka", TOPIC)).thenReturn(policy);

            assertThatThrownBy(() -> service.sendMessage(TOPIC, "k1", payload()).await().indefinitely())
                    .hasMessageContaining("Simulated Kafka drop/reject error");
            verify(producer, never()).send(any());
        }

        @Test
        void disabledPolicy_sendsNormally() {
            ChaosPolicy policy = ChaosPolicy.builder()
                    .name("disabled").type("kafka").target(TOPIC)
                    .enabled(false).errorChance(1.0).dropMessage(true).build();
            when(chaosManager.evaluate("kafka", TOPIC)).thenReturn(policy);
            when(producer.send(any(KafkaProducerRecord.class)))
                    .thenReturn(Future.succeededFuture(
                            new RecordMetadata(1L, 0, System.currentTimeMillis(), TOPIC)));

            service.sendMessage(TOPIC, "k1", payload()).await().indefinitely();

            verify(producer).send(any(KafkaProducerRecord.class));
        }
    }
}
