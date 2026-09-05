package com.sanedge.order.repository;

import com.sanedge.order.domain.requests.FindAllOrderByMerchantRequest;
import com.sanedge.order.domain.requests.FindAllOrderRequest;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.order.entity.Order;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderQueryRepository implements PanacheRepository<Order> {

    public Uni<PagedResult<Order>> findOrders(FindAllOrderRequest req) {
        int pageIndex = req.getPage() != null && req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() != null && req.getPageSize() > 0 ? req.getPageSize() : 10;
        String searchKeyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    CAST(?1 AS string) IS NULL
                    OR CAST(id AS string) LIKE CONCAT('%', ?1, '%')
                    OR CAST(cashierId AS string) LIKE CONCAT('%', ?1, '%')
                    ORDER BY createdAt ASC
                """;

        var panacheQuery = find(query, searchKeyword)
                .page(pageIndex, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Order>> findOrdersByMerchant(FindAllOrderByMerchantRequest req) {
        int pageIndex = req.getPage() != null && req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() != null && req.getPageSize() > 0 ? req.getPageSize() : 10;
        Long merchantId = req.getMerchantId() != null ? req.getMerchantId().longValue() : null;
        String searchKeyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    merchantId = ?1
                    AND (CAST(?2 AS string) IS NULL
                        OR CAST(id AS string) LIKE CONCAT('%', ?2, '%')
                        OR CAST(cashierId AS string) LIKE CONCAT('%', ?2, '%'))
                    ORDER BY createdAt ASC
                """;

        var panacheQuery = find(query, merchantId, searchKeyword)
                .page(pageIndex, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Order>> findActiveOrders(FindAllOrderRequest req) {
        int pageIndex = req.getPage() != null && req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() != null && req.getPageSize() > 0 ? req.getPageSize() : 10;
        String searchKeyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    deletedAt IS NULL
                    AND (CAST(?1 AS string) IS NULL
                        OR CAST(id AS string) LIKE CONCAT('%', ?1, '%')
                        OR CAST(cashierId AS string) LIKE CONCAT('%', ?1, '%'))
                    ORDER BY createdAt ASC
                """;

        var panacheQuery = find(query, searchKeyword)
                .page(pageIndex, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<Order> findOrderById(Long orderId) {
        return find("id = ?1 AND deletedAt IS NULL", orderId).firstResult();
    }

    public Uni<PagedResult<Order>> findTrashedOrders(FindAllOrderRequest req) {
        int pageIndex = req.getPage() != null && req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() != null && req.getPageSize() > 0 ? req.getPageSize() : 10;
        String searchKeyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    deletedAt IS NOT NULL
                    AND (CAST(?1 AS string) IS NULL
                        OR CAST(id AS string) LIKE CONCAT('%', ?1, '%')
                        OR CAST(cashierId AS string) LIKE CONCAT('%', ?1, '%'))
                    ORDER BY deletedAt DESC
                """;

        var panacheQuery = find(query, searchKeyword)
                .page(pageIndex, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }
}
