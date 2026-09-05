package com.sanedge.cashier.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import com.sanedge.cashier.entity.Cashier;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@WithSession
public class CashierCommandRepository implements PanacheRepository<Cashier> {

    @WithTransaction
    public Uni<Cashier> trashed(Long cashierId) {
        return find("id", cashierId).firstResult()
                .chain(cashier -> {
                    if (cashier != null && cashier.getDeletedAt() == null) {
                        LocalDateTime date = LocalDateTime.now();
                        cashier.setDeletedAt(Timestamp.valueOf(date));
                        return persist(cashier).map(v -> cashier);
                    }
                    return Uni.createFrom().item(cashier);
                });
    }

    @WithTransaction
    public Uni<Cashier> restore(Long cashierId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", cashierId).firstResult()
                .chain(cashier -> {
                    if (cashier != null) {
                        cashier.setDeletedAt(null);
                        return persist(cashier).map(v -> cashier);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Cashier> deletePermanent(Long cashierId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", cashierId).firstResult()
                .chain(cashier -> {
                    if (cashier != null) {
                        return delete(cashier).map(v -> cashier);
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
