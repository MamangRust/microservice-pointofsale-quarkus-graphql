package com.sanedge.order.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import com.sanedge.order.entity.Outbox;
import com.sanedge.order.entity.OutboxStatus;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OutboxRepository implements PanacheRepository<Outbox> {

    public Uni<List<Outbox>> findPending(int limit) {
        return find("status = ?1 ORDER BY createdAt", OutboxStatus.PENDING)
                .page(io.quarkus.panache.common.Page.ofSize(limit))
                .list();
    }

    public Uni<Void> markProcessed(Long id) {
        return findById(id)
                .chain(outbox -> {
                    if (outbox == null) {
                        return Uni.createFrom().voidItem();
                    }
                    outbox.setStatus(OutboxStatus.PROCESSED);
                    outbox.setProcessedAt(Timestamp.valueOf(LocalDateTime.now()));
                    outbox.setLastError(null);
                    return persist(outbox).replaceWithVoid();
                });
    }

    public Uni<Void> markFailed(Long id, String error, int maxAttempts) {
        return findById(id)
                .chain(outbox -> {
                    if (outbox == null) {
                        return Uni.createFrom().voidItem();
                    }
                    int attempts = outbox.getAttempts() + 1;
                    outbox.setAttempts(attempts);
                    outbox.setLastError(error);
                    if (attempts >= maxAttempts) {
                        outbox.setStatus(OutboxStatus.FAILED);
                        outbox.setProcessedAt(Timestamp.valueOf(LocalDateTime.now()));
                    }
                    return persist(outbox).replaceWithVoid();
                });
    }
}
