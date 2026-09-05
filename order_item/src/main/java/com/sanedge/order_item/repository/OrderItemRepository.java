package com.sanedge.order_item.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import com.sanedge.order_item.entity.OrderItem;
import com.sanedge.common.domain.response.PagedResult;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderItemRepository implements PanacheRepository<OrderItem> {

    public Uni<List<OrderItem>> findOrderItemByOrder(Long orderId) {
        return list("orderId = ?1 AND deletedAt IS NULL", orderId);
    }

    public Uni<List<OrderItem>> findOrderItemByOrderIncludingDeleted(Long orderId) {
        return list("orderId = ?1", orderId);
    }

    @WithTransaction
    public Uni<OrderItem> trashed(Long orderItemId) {
        return find("id", orderItemId).firstResult()
                .chain(item -> {
                    if (item != null && item.getDeletedAt() == null) {
                        LocalDateTime date = LocalDateTime.now();
                        item.setDeletedAt(Timestamp.valueOf(date));
                        return persist(item).map(v -> item);
                    }
                    return Uni.createFrom().item(item);
                });
    }

    @WithTransaction
    public Uni<OrderItem> restore(Long orderItemId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", orderItemId).firstResult()
                .chain(item -> {
                    if (item != null) {
                        item.setDeletedAt(null);
                        return persist(item).map(v -> item);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<OrderItem> deletePermanent(Long orderItemId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", orderItemId).firstResult()
                .chain(item -> {
                    if (item != null) {
                        return delete(item).map(v -> item);
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

    public Uni<PagedResult<OrderItem>> findOrderItems(com.sanedge.order_item.domain.requests.FindAllOrderItems req) {
        int pageIndex = req.getPage() > 0 ? req.getPage() - 1 : 0;
        String searchKeyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    CAST(?1 AS string) IS NULL
                    OR CAST(id AS string) LIKE CONCAT('%', ?1, '%')
                    OR CAST(orderId AS string) LIKE CONCAT('%', ?1, '%')
                    OR CAST(productId AS string) LIKE CONCAT('%', ?1, '%')
                    ORDER BY createdAt ASC
                """;

        var panacheQuery = find(query, searchKeyword)
                .page(pageIndex, req.getPageSize());

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<OrderItem>> findActiveOrderItems(com.sanedge.order_item.domain.requests.FindAllOrderItems req) {
        int pageIndex = req.getPage() > 0 ? req.getPage() - 1 : 0;
        String searchKeyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    deletedAt IS NULL
                    AND (CAST(?1 AS string) IS NULL
                        OR CAST(id AS string) LIKE CONCAT('%', ?1, '%')
                        OR CAST(orderId AS string) LIKE CONCAT('%', ?1, '%')
                        OR CAST(productId AS string) LIKE CONCAT('%', ?1, '%'))
                    ORDER BY createdAt ASC
                """;

        var panacheQuery = find(query, searchKeyword)
                .page(pageIndex, req.getPageSize());

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<OrderItem>> findTrashedOrderItems(com.sanedge.order_item.domain.requests.FindAllOrderItems req) {
        int pageIndex = req.getPage() > 0 ? req.getPage() - 1 : 0;
        String searchKeyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    deletedAt IS NOT NULL
                    AND (CAST(?1 AS string) IS NULL
                        OR CAST(id AS string) LIKE CONCAT('%', ?1, '%')
                        OR CAST(orderId AS string) LIKE CONCAT('%', ?1, '%')
                        OR CAST(productId AS string) LIKE CONCAT('%', ?1, '%'))
                    ORDER BY deletedAt DESC
                """;

        var panacheQuery = find(query, searchKeyword)
                .page(pageIndex, req.getPageSize());

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }
}
