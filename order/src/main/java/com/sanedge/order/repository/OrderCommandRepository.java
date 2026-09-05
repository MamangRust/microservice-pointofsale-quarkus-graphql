package com.sanedge.order.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import com.sanedge.order.entity.Order;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderCommandRepository implements PanacheRepository<Order> {

    @WithTransaction
    public Uni<Order> trashed(Long orderId) {
        return find("id", orderId).firstResult()
                .chain(order -> {
                    if (order != null && order.getDeletedAt() == null) {
                        LocalDateTime date = LocalDateTime.now();
                        order.setDeletedAt(Timestamp.valueOf(date));
                        return persist(order).map(v -> order);
                    }
                    return Uni.createFrom().item(order);
                });
    }

    @WithTransaction
    public Uni<Order> restore(Long orderId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", orderId).firstResult()
                .chain(order -> {
                    if (order != null) {
                        order.setDeletedAt(null);
                        return persist(order).map(v -> order);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Order> deletePermanent(Long orderId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", orderId).firstResult()
                .chain(order -> {
                    if (order != null) {
                        return delete(order).map(v -> order);
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
