package com.sanedge.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import io.vertx.kafka.client.producer.RecordMetadata;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock ReactiveMailer mailer;
    @Mock EmailDedupGuard dedupGuard;
    @Mock KafkaProducer<String, String> producer;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(OpenTelemetry.noop());
        emailService.mailer = mailer;
        emailService.dedupGuard = dedupGuard;
        emailService.producer = producer;
        emailService.maxAttempts = 3;
        emailService.baseBackoffMs = 30000;

        lenient().when(dedupGuard.claim(anyString()))
                .thenReturn(Uni.createFrom().item(EmailDedupGuard.ClaimResult.CLAIMED));
        lenient().when(dedupGuard.markSent(anyString())).thenReturn(Uni.createFrom().voidItem());
        lenient().when(dedupGuard.release(anyString())).thenReturn(Uni.createFrom().voidItem());
    }

    private JsonObject payload() {
        // Phase 2 (event contract): standard envelope + email fields.
        return new JsonObject()
                .put("event_id", "evt-123")
                .put("schema_version", 1)
                .put("event_type", "email-service-topic-auth-register")
                .put("email", "customer@example.com")
                .put("subject", "Order Confirmation")
                .put("body", "<b>Thanks for your order!</b>");
    }

    @Nested
    @DisplayName("processRecord happy path")
    class HappyPathTests {

        @Test
        void success_claimsDedupAndMarksSent() {
            when(mailer.send(any(Mail.class))).thenReturn(Uni.createFrom().voidItem());

            emailService.processRecord("src-topic", 0, 1L, "k1", payload(), 0)
                    .await().indefinitely();

            verify(mailer).send(any(Mail.class));
            verify(dedupGuard).claim("evt-123");
            verify(dedupGuard).markSent("evt-123");
        }

        @Test
        void duplicate_skipsSendAndMarkSent() {
            when(dedupGuard.claim(anyString()))
                    .thenReturn(Uni.createFrom().item(EmailDedupGuard.ClaimResult.DUPLICATE));

            emailService.processRecord("src-topic", 0, 1L, "k1", payload(), 0)
                    .await().indefinitely();

            verify(mailer, never()).send(any(Mail.class));
            verify(dedupGuard, never()).markSent(anyString());
        }

        @Test
        void busyClaim_doesNotSend() {
            when(dedupGuard.claim(anyString()))
                    .thenReturn(Uni.createFrom().item(EmailDedupGuard.ClaimResult.BUSY));

            try {
                emailService.processRecord("src-topic", 0, 1L, "k1", payload(), 0)
                        .await().indefinitely();
                org.assertj.core.api.Assertions.fail("BUSY claim must fail so the offset is not committed");
            } catch (IllegalStateException e) {
                org.assertj.core.api.Assertions.assertThat(e.getMessage()).contains("claim busy");
            }

            verify(mailer, never()).send(any(Mail.class));
        }

        @Test
        @SuppressWarnings("unchecked")
        void invalidEnvelope_routesToDlqWithoutSending() {
            // Missing event_id / schema_version → invalid envelope: never sent
            // to SMTP, parked in the DLQ (Phase 2 event contract).
            JsonObject bad = new JsonObject().put("email", "x@y.z");
            when(producer.send(any(KafkaProducerRecord.class)))
                    .thenReturn(Future.succeededFuture(
                            new RecordMetadata(1L, 0, System.currentTimeMillis(), EmailService.DLQ_TOPIC)));

            emailService.processRecord("src-topic", 0, 1L, "k1", bad, 0)
                    .await().indefinitely();

            verify(mailer, never()).send(any(Mail.class));
            verify(dedupGuard, never()).claim(anyString());

            ArgumentCaptor<KafkaProducerRecord<String, String>> captor =
                    ArgumentCaptor.forClass((Class) KafkaProducerRecord.class);
            verify(producer).send(captor.capture());

            KafkaProducerRecord<String, String> record = captor.getValue();
            assertThat(record.topic()).isEqualTo(EmailService.DLQ_TOPIC);
            JsonObject dlq = new JsonObject(record.value());
            assertThat(dlq.getString("_reason")).isEqualTo("invalid_event_envelope");
        }
    }

    @Nested
    @DisplayName("retry / DLQ routing")
    class RetryTests {

        @Test
        @SuppressWarnings("unchecked")
        void sendFailure_routesToRetryTopicWithAttempt() {
            when(mailer.send(any(Mail.class)))
                    .thenReturn(Uni.createFrom().failure(new RuntimeException("smtp down")));
            when(producer.send(any(KafkaProducerRecord.class)))
                    .thenReturn(Future.succeededFuture(
                            new RecordMetadata(1L, 0, System.currentTimeMillis(), EmailService.RETRY_TOPIC)));

            emailService.processRecord("src-topic", 0, 1L, "k1", payload(), 0)
                    .await().indefinitely();

            verify(dedupGuard, never()).markSent(anyString());
            verify(dedupGuard).release("evt-123");
            ArgumentCaptor<KafkaProducerRecord<String, String>> captor =
                    ArgumentCaptor.forClass((Class) KafkaProducerRecord.class);
            verify(producer).send(captor.capture());

            KafkaProducerRecord<String, String> record = captor.getValue();
            assertThat(record.topic()).isEqualTo(EmailService.RETRY_TOPIC);
            JsonObject retry = new JsonObject(record.value());
            assertThat(retry.getInteger("_attempt")).isEqualTo(1);
            assertThat(retry.getString("_srcTopic")).isEqualTo("src-topic");
            assertThat(retry.getLong("_srcOffset")).isEqualTo(1L);
        }

        @Test
        @SuppressWarnings("unchecked")
        void sendFailureAtMaxAttempts_routesToDlq() {
            when(mailer.send(any(Mail.class)))
                    .thenReturn(Uni.createFrom().failure(new RuntimeException("smtp down")));
            when(producer.send(any(KafkaProducerRecord.class)))
                    .thenReturn(Future.succeededFuture(
                            new RecordMetadata(1L, 0, System.currentTimeMillis(), EmailService.DLQ_TOPIC)));

            // attempt = 2, maxAttempts = 3 → next attempt 3 hits the limit → DLQ
            emailService.processRecord("src-topic", 0, 1L, "k1", payload(), 2)
                    .await().indefinitely();

            verify(dedupGuard).release("evt-123");
            ArgumentCaptor<KafkaProducerRecord<String, String>> captor =
                    ArgumentCaptor.forClass((Class) KafkaProducerRecord.class);
            verify(producer).send(captor.capture());

            KafkaProducerRecord<String, String> record = captor.getValue();
            assertThat(record.topic()).isEqualTo(EmailService.DLQ_TOPIC);
            JsonObject dlq = new JsonObject(record.value());
            assertThat(dlq.getString("_reason")).isEqualTo("max_retries_exceeded");
            assertThat(dlq.getInteger("_attempts")).isEqualTo(3);
        }
    }
}
