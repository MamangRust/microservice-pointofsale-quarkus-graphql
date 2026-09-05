package com.sanedge.product.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import com.sanedge.product.entity.Product;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProductCommandRepository implements PanacheRepository<Product> {

    @WithTransaction
    public Uni<Product> trashed(Long productId) {
        return find("id", productId).firstResult()
                .chain(product -> {
                    if (product != null && product.getDeletedAt() == null) {
                        LocalDateTime date = LocalDateTime.now();
                        product.setDeletedAt(Timestamp.valueOf(date));
                        return persist(product).map(v -> product);
                    }
                    return Uni.createFrom().item(product);
                });
    }

    @WithTransaction
    public Uni<Product> restore(Long productId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", productId).firstResult()
                .chain(product -> {
                    if (product != null) {
                        product.setDeletedAt(null);
                        return persist(product).map(v -> product);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Product> deletePermanent(Long productId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", productId).firstResult()
                .chain(product -> {
                    if (product != null) {
                        return delete(product).map(v -> product);
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
