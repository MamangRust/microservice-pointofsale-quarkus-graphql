package com.sanedge.order.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sanedge.common.test.PostgreSqlResource;
import com.sanedge.common.test.RedisResource;
import com.sanedge.order.entity.Outbox;
import com.sanedge.order.entity.OutboxStatus;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

/**
 * Integration test of the shared {@code outbox} table against a real
 * PostgreSQL (Testcontainers + the module's own Flyway migrations).
 *
 * <p>It verifies the domain discriminator (a publisher only sees the rows of
 * its own domain) and the lease protocol (claim, publish, retry, dead
 * letter).</p>
 */
@QuarkusTest
@QuarkusTestResource(value = PostgreSqlResource.class, restrictToAnnotatedClass = true)
@QuarkusTestResource(value = RedisResource.class, restrictToAnnotatedClass = true)
@TestProfile(OutboxRepositoryIT.NoPublisherProfile.class)
@RunOnVertxContext
class OutboxRepositoryIT {

    public static class NoPublisherProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "outbox.publisher.enabled", "false",
                    "outbox.lease.seconds", "300");
        }
    }

    @Inject
    OutboxRepository repository;

    private Uni<Long> cleanTable() {
        return Panache.withTransaction(() -> repository.deleteAll());
    }

    private Uni<Outbox> findById(Long id) {
        return Panache.withSession(() -> repository.findById(id));
    }

    private Uni<Outbox> persist(String domain, String topic) {
        Outbox outbox = new Outbox();
        outbox.setAggregateType("ORDER");
        outbox.setAggregateId("1");
        outbox.setEventKey("1");
        outbox.setDomain(domain);
        outbox.setTopic(topic);
        outbox.setEventId(UUID.randomUUID().toString());
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setPayload("{\"event_id\":\"x\"}");
        return repository.persistEvent(outbox);
    }

    @Test
    void findDueOnlyReturnsTheRowsOfTheOwnDomain(UniAsserter asserter) {
        asserter.execute(() -> cleanTable());
        asserter.execute(() -> persist("order", "order.event"));
        asserter.execute(() -> persist("transaction", "transactions.event"));

        asserter.assertThat(() -> repository.findDue("order", 50),
                rows -> assertThat(rows).extracting(Outbox::getDomain).containsOnly("order"));
        asserter.assertThat(() -> repository.findDue("transaction", 50),
                rows -> assertThat(rows).extracting(Outbox::getDomain).containsOnly("transaction"));
    }

    @Test
    void claimTakesTheLeaseAndBlocksASecondClaim(UniAsserter asserter) {
        asserter.execute(() -> cleanTable());
        Outbox[] ref = new Outbox[1];
        asserter.execute(() -> persist("order", "order.event"));
        asserter.assertThat(() -> repository.findDue("order", 50), rows -> ref[0] = rows.get(0));

        asserter.assertThat(() -> repository.claim("order", ref[0]), claimed -> {
            assertThat(claimed).isNotNull();
            assertThat(claimed.getClaimToken()).isNotBlank();
            assertThat(claimed.getClaimedAt()).isNotNull();
            assertThat(claimed.getAttempts()).isEqualTo(1);
        });
        asserter.assertThat(() -> repository.claim("order", ref[0]),
                claimed -> assertThat(claimed).isNull());
    }

    @Test
    void markSentMovesTheRowToProcessed(UniAsserter asserter) {
        asserter.execute(() -> cleanTable());
        Outbox[] ref = new Outbox[1];
        asserter.execute(() -> persist("order", "order.event"));
        asserter.assertThat(() -> repository.findDue("order", 50), rows -> ref[0] = rows.get(0));
        asserter.assertThat(() -> repository.claim("order", ref[0]), claimed -> {
            assertThat(claimed).isNotNull();
            ref[0] = claimed;
        });

        asserter.execute(() -> repository.markSent(ref[0]));
        asserter.assertThat(() -> findById(ref[0].getId()), saved -> {
            assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
            assertThat(saved.getClaimedAt()).isNull();
            assertThat(saved.getLastError()).isNull();
        });
        asserter.assertThat(() -> repository.findDue("order", 50),
                rows -> assertThat(rows).noneMatch(o -> o.getId().equals(ref[0].getId())));
    }

    @Test
    void markRetryKeepsTheRowPendingUntilMaxAttempts(UniAsserter asserter) {
        asserter.execute(() -> cleanTable());
        Outbox[] ref = new Outbox[1];
        asserter.execute(() -> persist("order", "order.event"));
        asserter.assertThat(() -> repository.findDue("order", 50), rows -> ref[0] = rows.get(0));
        asserter.assertThat(() -> repository.claim("order", ref[0]), claimed -> {
            assertThat(claimed).isNotNull();
            ref[0] = claimed;
        });

        // attempts = 1 < maxAttempts = 2 → stays PENDING with a backoff
        asserter.execute(() -> repository.markRetry(ref[0], new RuntimeException("broker down"), 60, 2));
        asserter.assertThat(() -> findById(ref[0].getId()), saved -> {
            assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(saved.getNextAttemptAt()).isNotNull();
            assertThat(saved.getLastError()).contains("broker down");
        });

        // the row is not due while the backoff window is open
        asserter.assertThat(() -> repository.findDue("order", 50),
                rows -> assertThat(rows).noneMatch(o -> o.getId().equals(ref[0].getId())));
    }

    @Test
    void markRetryMovesTheRowToFailedWhenMaxAttemptsIsReached(UniAsserter asserter) {
        asserter.execute(() -> cleanTable());
        Outbox[] ref = new Outbox[1];
        asserter.execute(() -> persist("order", "order.event"));
        asserter.assertThat(() -> repository.findDue("order", 50), rows -> ref[0] = rows.get(0));
        asserter.assertThat(() -> repository.claim("order", ref[0]), claimed -> {
            assertThat(claimed).isNotNull();
            ref[0] = claimed;
        });

        // attempts = 1 < maxAttempts = 2 → stays PENDING, retried without delay
        asserter.execute(() -> repository.markRetry(ref[0], new RuntimeException("broker down"), 0, 2));
        asserter.assertThat(() -> findById(ref[0].getId()),
                saved -> assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING));

        // attempts = 2 >= maxAttempts = 2 → dead letter
        asserter.assertThat(() -> repository.claim("order", ref[0]), claimed -> {
            assertThat(claimed).isNotNull();
            ref[0] = claimed;
        });
        asserter.execute(() -> repository.markRetry(ref[0], new RuntimeException("broker down"), 0, 2));
        asserter.assertThat(() -> findById(ref[0].getId()), saved -> {
            assertThat(saved.getStatus()).isEqualTo(OutboxStatus.FAILED);
            assertThat(saved.getProcessedAt()).isNotNull();
        });
    }

    @Test
    void countPendingCountsOnlyTheOwnDomain(UniAsserter asserter) {
        asserter.execute(() -> cleanTable());
        asserter.execute(() -> persist("order", "order.event"));
        asserter.execute(() -> persist("transaction", "transactions.event"));

        asserter.assertThat(() -> repository.countPending("order"),
                count -> assertThat(count).isEqualTo(1L));
        asserter.assertThat(() -> repository.countPending("transaction"),
                count -> assertThat(count).isEqualTo(1L));
    }
}
