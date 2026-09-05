package com.sanedge.user.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import com.sanedge.user.domain.requests.FindAllUsers;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.user.entity.User;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    public Uni<PagedResult<User>> findUsers(FindAllUsers req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;

        var query = """
                    deletedAt IS NULL
                    AND (
                        CAST(?1 AS string) IS NULL
                        OR LOWER(firstname) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(lastname)  LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(email)     LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<User>> findActiveUsers(FindAllUsers req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;

        var query = """
                    deletedAt IS NULL
                    AND (
                        CAST(?1 AS string) IS NULL
                        OR LOWER(firstname) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(lastname)  LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(email)     LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<User>> findTrashedUsers(FindAllUsers req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;

        var query = """
                    deletedAt IS NOT NULL
                    AND (
                        CAST(?1 AS string) IS NULL
                        OR LOWER(firstname) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(lastname)  LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(email)     LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<User> findById(Integer id) {
        return find("id", Long.valueOf(id)).firstResult();
    }

    public Uni<User> findByUsername(String username) {
        return find("username", username).firstResult();
    }

    public Uni<User> findByEmail(String email) {
        return find("email", email).firstResult();
    }

    public Uni<Boolean> existsByUsername(String username) {
        return count("username = ?1", username).map(c -> c > 0);
    }

    public Uni<Boolean> existsByEmail(String email) {
        return count("email = ?1", email).map(c -> c > 0);
    }

    @WithTransaction
    public Uni<User> trash(Long userId) {
        return findById(userId)
                .chain(user -> {
                    if (user != null && user.getDeletedAt() == null) {
                        LocalDateTime date = LocalDateTime.now();
                        user.setDeletedAt(Timestamp.valueOf(date));
                        return persist(user).map(v -> user);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<User> restore(Long userId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", userId).firstResult()
                .chain(user -> {
                    if (user != null) {
                        user.setDeletedAt(null);
                        return persist(user).map(v -> user);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<User> deletePermanent(Long userId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", userId).firstResult()
                .chain(user -> {
                    if (user != null) {
                        return delete(user).map(v -> user);
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
