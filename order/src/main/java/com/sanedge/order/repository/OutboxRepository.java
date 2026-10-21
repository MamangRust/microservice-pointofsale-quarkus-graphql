package com.sanedge.order.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.sanedge.order.entity.Outbox;
import com.sanedge.order.entity.OutboxStatus;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Access to the shared {@code outbox} table.
 *
 * <p>The table is owned by the order module (Flyway) but used by every module
 * of the sales context: a row belongs to the module named in its
 * {@code domain} column, and only that module's publisher claims it.</p>
 */
@ApplicationScoped
public class OutboxRepository implements PanacheRepository<Outbox> {

    @ConfigProperty(name = "outbox.lease.seconds", defaultValue = "300")
    long leaseSeconds;

    /**
     * Stores a domain event in the outbox. When it is called from a business
     * operation that already runs inside a transaction the nested
     * {@code @WithTransaction} joins that transaction, so the event row and
     * the business row are committed (or rolled back) together.
     */
    @WithTransaction
    public Uni<Outbox> persistEvent(Outbox event) {
        return persist(event);
    }

    @WithTransaction
    public Uni<Long> countPending(String domain) {
        return count("status = ?1 AND domain = ?2", OutboxStatus.PENDING, domain);
    }

    /**
     * Returns the rows of {@code domain} that are ready to be published: not
     * claimed by another publisher, not scheduled for a later retry and still
     * PENDING.
     */
    @WithTransaction
    public Uni<List<Outbox>> findDue(String domain, int limit) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp leaseCutoff = Timestamp.valueOf(LocalDateTime.now().minusSeconds(Math.max(1, leaseSeconds)));
        return find("status = ?1 AND domain = ?2 "
                + "AND (nextAttemptAt IS NULL OR nextAttemptAt <= ?3) "
                + "AND (claimedAt IS NULL OR claimedAt < ?4) ORDER BY createdAt",
                OutboxStatus.PENDING, domain, now, leaseCutoff)
                .page(io.quarkus.panache.common.Page.ofSize(limit))
                .list();
    }

    /**
     * Takes a lease on the row so that only one publisher relays it. The
     * lease expires after {@code outbox.lease.seconds}, which releases the
     * row again when a publisher dies mid-flight.
     */
    @WithTransaction
    public Uni<Outbox> claim(String domain, Outbox event) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp leaseCutoff = Timestamp.valueOf(LocalDateTime.now().minusSeconds(Math.max(1, leaseSeconds)));
        String claimToken = UUID.randomUUID().toString();
        return update("claimedAt = ?1, claimToken = ?2, attempts = attempts + 1 "
                + "WHERE id = ?3 AND domain = ?4 AND status = ?5 "
                + "AND (claimedAt IS NULL OR claimedAt < ?6) "
                + "AND (nextAttemptAt IS NULL OR nextAttemptAt <= ?1)",
                now, claimToken, event.getId(), domain, OutboxStatus.PENDING, leaseCutoff)
                .chain(updated -> updated > 0 ? findById(event.getId()) : Uni.createFrom().nullItem());
    }

    @WithTransaction
    public Uni<Void> markSent(Outbox event) {
        return update("status = ?1, claimedAt = NULL, claimToken = NULL, lastError = NULL, processedAt = ?2 "
                + "WHERE id = ?3 AND status = ?4 AND claimToken = ?5",
                OutboxStatus.PROCESSED, Timestamp.valueOf(LocalDateTime.now()), event.getId(),
                OutboxStatus.PENDING, event.getClaimToken())
                .replaceWithVoid();
    }

    /**
     * Records a delivery failure. The row stays PENDING and is retried after
     * the given delay; once the maximum number of attempts is reached it is
     * moved to FAILED (dead letter) so it is not retried forever.
     */
    @WithTransaction
    public Uni<Void> markRetry(Outbox event, Throwable failure, long delaySeconds, int maxAttempts) {
        boolean dead = event.getAttempts() != null && event.getAttempts() >= maxAttempts;
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        return update("status = ?1, claimedAt = NULL, claimToken = NULL, lastError = ?2, "
                + "nextAttemptAt = ?3, processedAt = ?4 "
                + "WHERE id = ?5 AND status = ?6 AND claimToken = ?7",
                dead ? OutboxStatus.FAILED : OutboxStatus.PENDING,
                failure == null ? "unknown failure" : failure.getMessage(),
                dead ? null : Timestamp.valueOf(LocalDateTime.now().plusSeconds(delaySeconds)),
                dead ? now : null,
                event.getId(), OutboxStatus.PENDING, event.getClaimToken())
                .replaceWithVoid();
    }
}
