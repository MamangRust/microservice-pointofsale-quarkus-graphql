package com.sanedge.category.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import com.sanedge.category.entity.Category;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CategoryCommandRepository implements PanacheRepository<Category> {

    @WithTransaction
    public Uni<Category> trashed(Long categoryId) {
        return find("id", categoryId).firstResult()
                .chain(category -> {
                    if (category != null && category.getDeletedAt() == null) {
                        LocalDateTime date = LocalDateTime.now();
                        category.setDeletedAt(Timestamp.valueOf(date));
                        return persist(category).map(v -> category);
                    }
                    return Uni.createFrom().item(category);
                });
    }

    @WithTransaction
    public Uni<Category> restore(Long categoryId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", categoryId).firstResult()
                .chain(category -> {
                    if (category != null) {
                        category.setDeletedAt(null);
                        return persist(category).map(v -> category);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Category> deletePermanent(Long categoryId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", categoryId).firstResult()
                .chain(category -> {
                    if (category != null) {
                        return delete(category).map(v -> category);
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
