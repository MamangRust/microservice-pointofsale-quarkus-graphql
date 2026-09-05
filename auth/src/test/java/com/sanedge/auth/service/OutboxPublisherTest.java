package com.sanedge.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.auth.entity.Outbox;
import com.sanedge.auth.entity.OutboxStatus;
import com.sanedge.auth.repository.OutboxRepository;

import io.opentelemetry.api.OpenTelemetry;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock private OutboxRepository outboxRepository;
    @Mock private KafkaService kafkaService;

    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxPublisher(OpenTelemetry.noop());
        publisher.outboxRepository = outboxRepository;
        publisher.kafkaService = kafkaService;
        publisher.maxAttempts = 3;
        publisher.batchSize = 10;
    }

    private Outbox pendingOutbox(Long id, String topic) {
        Outbox outbox = new Outbox();
        outbox.setId(id);
        outbox.setAggregateType("USER");
        outbox.setAggregateId(String.valueOf(id));
        outbox.setTopic(topic);
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setAttempts(0);
        outbox.setPayload(new JsonObject().put("email", "user@example.com").encode());
        return outbox;
    }

    @Nested
    @DisplayName("drainPending tests")
    class DrainTests {

        @Test
        void noPendingRows_doesNothing() {
            when(outboxRepository.findPending(10)).thenReturn(Uni.createFrom().item(List.of()));

            publisher.drainPending().await().indefinitely();

            verify(outboxRepository, never()).markProcessed(any());
            verify(kafkaService, never()).sendMessage(anyString(), anyString(), any(JsonObject.class));
        }

        @Test
        void publishesAndMarksProcessed() {
            Outbox row = pendingOutbox(1L, "email-service-topic-auth-register");
            when(outboxRepository.findPending(10)).thenReturn(Uni.createFrom().item(List.of(row)));
            when(kafkaService.sendMessage(eq(row.getTopic()), eq("1"), any(JsonObject.class)))
                    .thenReturn(Uni.createFrom().voidItem());
            when(outboxRepository.markProcessed(1L)).thenReturn(Uni.createFrom().voidItem());

            publisher.drainPending().await().indefinitely();

            verify(kafkaService).sendMessage(eq(row.getTopic()), eq("1"), any(JsonObject.class));
            verify(outboxRepository).markProcessed(1L);
            verify(outboxRepository, never()).markFailed(any(), any(), anyInt());
        }

        @Test
        void publishFailure_marksFailedForRetry() {
            Outbox row = pendingOutbox(2L, "email-service-topic-auth-forgot-password");
            row.setAttempts(1); // will become 2 of max 3 → stays PENDING for retry
            when(outboxRepository.findPending(10)).thenReturn(Uni.createFrom().item(List.of(row)));
            when(kafkaService.sendMessage(eq(row.getTopic()), eq("2"), any(JsonObject.class)))
                    .thenReturn(Uni.createFrom().failure(new RuntimeException("broker down")));
            when(outboxRepository.markFailed(2L, "broker down", 3)).thenReturn(Uni.createFrom().voidItem());

            publisher.drainPending().await().indefinitely();

            verify(outboxRepository).markFailed(2L, "broker down", 3);
            verify(outboxRepository, never()).markProcessed(any());
        }
    }
}
