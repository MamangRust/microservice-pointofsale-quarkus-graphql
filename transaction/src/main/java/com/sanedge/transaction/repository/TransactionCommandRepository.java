package com.sanedge.transaction.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import com.sanedge.transaction.entity.Transaction;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TransactionCommandRepository implements PanacheRepository<Transaction> {

    @WithTransaction
    public Uni<Transaction> trashed(Long transactionId) {
        return find("id", transactionId).firstResult()
                .chain(transaction -> {
                    if (transaction != null && transaction.getDeletedAt() == null) {
                        LocalDateTime date = LocalDateTime.now();
                        transaction.setDeletedAt(Timestamp.valueOf(date));
                        return persist(transaction).map(v -> transaction);
                    }
                    return Uni.createFrom().item(transaction);
                });
    }

    @WithTransaction
    public Uni<Transaction> restore(Long transactionId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", transactionId).firstResult()
                .chain(transaction -> {
                    if (transaction != null) {
                        transaction.setDeletedAt(null);
                        return persist(transaction).map(v -> transaction);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Transaction> deletePermanent(Long transactionId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", transactionId).firstResult()
                .chain(transaction -> {
                    if (transaction != null) {
                        return delete(transaction).map(v -> transaction);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Boolean> restoreAllDeleted() {
        return update("deletedAt = NULL WHERE deletedAt IS NOT NULL")
                .map(updatedCount -> updatedCount > 0);
    }

    @WithTransaction
    public Uni<Boolean> deleteAllDeleted() {
        return delete("deletedAt IS NOT NULL")
                .map(deletedCount -> deletedCount > 0);
    }
}
