package com.sanedge.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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

import com.sanedge.transaction.entity.Outbox;
import com.sanedge.transaction.entity.OutboxStatus;
import com.sanedge.transaction.repository.OutboxRepository;

import io.opentelemetry.api.OpenTelemetry;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    private static final String DOMAIN = "transaction";

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
        outbox.setAggregateType("TRANSACTION");
        outbox.setAggregateId(String.valueOf(id));
        outbox.setEventKey(String.valueOf(id));
        outbox.setDomain(DOMAIN);
        outbox.setTopic(topic);
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setAttempts(0);
        outbox.setClaimToken("token-" + id);
        outbox.setPayload(new JsonObject().put("email", "test@example.com").encode());
        return outbox;
    }

    @Nested
    @DisplayName("drainPending tests")
    class DrainTests {

        @Test
        void noPendingRows_doesNothing() {
            when(outboxRepository.findDue(DOMAIN, 10)).thenReturn(Uni.createFrom().item(List.of()));

            publisher.drainPending().await().indefinitely();

            verify(outboxRepository, never()).markSent(any());
            verify(kafkaService, never()).sendExistingEvent(anyString(), anyString(), anyString());
        }

        @Test
        void claimsTheRowBeforePublishing() {
            Outbox row = pendingOutbox(1L, "email-service-topic-transaction-create");
            when(outboxRepository.findDue(DOMAIN, 10)).thenReturn(Uni.createFrom().item(List.of(row)));
            when(outboxRepository.claim(DOMAIN, row)).thenReturn(Uni.createFrom().item(row));
            when(kafkaService.sendExistingEvent(eq(row.getTopic()), eq("1"), anyString()))
                    .thenReturn(Uni.createFrom().voidItem());
            when(outboxRepository.markSent(row)).thenReturn(Uni.createFrom().voidItem());

            publisher.drainPending().await().indefinitely();

            verify(outboxRepository).claim(DOMAIN, row);
            verify(kafkaService).sendExistingEvent(eq(row.getTopic()), eq("1"), anyString());
            verify(outboxRepository).markSent(row);
            verify(outboxRepository, never()).markRetry(any(), any(), anyLong(), anyInt());
        }

        @Test
        void rowClaimedByAnotherPublisher_isSkipped() {
            Outbox row = pendingOutbox(2L, "email-service-topic-transaction-create");
            when(outboxRepository.findDue(DOMAIN, 10)).thenReturn(Uni.createFrom().item(List.of(row)));
            when(outboxRepository.claim(DOMAIN, row)).thenReturn(Uni.createFrom().nullItem());

            publisher.drainPending().await().indefinitely();

            verify(kafkaService, never()).sendExistingEvent(anyString(), anyString(), anyString());
            verify(outboxRepository, never()).markSent(any());
        }

        @Test
        void publishFailure_marksRetry() {
            Outbox row = pendingOutbox(3L, "email-service-topic-transaction-create");
            row.setAttempts(1); // will become 2 of max 3 → stays PENDING for retry
            when(outboxRepository.findDue(DOMAIN, 10)).thenReturn(Uni.createFrom().item(List.of(row)));
            when(outboxRepository.claim(DOMAIN, row)).thenReturn(Uni.createFrom().item(row));
            when(kafkaService.sendExistingEvent(eq(row.getTopic()), eq("3"), anyString()))
                    .thenReturn(Uni.createFrom().failure(new RuntimeException("broker down")));
            when(outboxRepository.markRetry(eq(row), any(), anyLong(), eq(3)))
                    .thenReturn(Uni.createFrom().voidItem());

            publisher.drainPending().await().indefinitely();

            verify(outboxRepository).markRetry(eq(row), any(), anyLong(), eq(3));
            verify(outboxRepository, never()).markSent(any());
        }

        @Test
        void processesMultipleRowsSequentially() {
            Outbox first = pendingOutbox(1L, "email-service-topic-transaction-create");
            Outbox second = pendingOutbox(2L, "email-service-topic-transaction-create");
            when(outboxRepository.findDue(DOMAIN, 10)).thenReturn(Uni.createFrom().item(List.of(first, second)));
            lenient().when(outboxRepository.claim(eq(DOMAIN), any()))
                    .thenAnswer(inv -> Uni.createFrom().item((Outbox) inv.getArgument(1)));
            lenient().when(kafkaService.sendExistingEvent(anyString(), anyString(), anyString()))
                    .thenReturn(Uni.createFrom().voidItem());
            lenient().when(outboxRepository.markSent(any())).thenReturn(Uni.createFrom().voidItem());

            publisher.drainPending().await().indefinitely();

            verify(outboxRepository).markSent(first);
            verify(outboxRepository).markSent(second);
        }
    }
}
