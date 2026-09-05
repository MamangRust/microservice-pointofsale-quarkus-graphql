package com.sanedge.transaction.repository;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.transaction.domain.requests.FindAllTransactionByMerchantRequest;
import com.sanedge.transaction.domain.requests.FindAllTransactionRequest;
import com.sanedge.transaction.entity.Transaction;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TransactionQueryRepository implements PanacheRepository<Transaction> {

    public Uni<PagedResult<Transaction>> findTransactions(FindAllTransactionRequest req) {
        int pageIndex = req.getPage() != null && req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() != null && req.getPageSize() > 0 ? req.getPageSize() : 10;
        String searchKeyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    CAST(?1 AS string) IS NULL
                    OR CAST(id AS string) LIKE CONCAT('%', ?1, '%')
                    OR LOWER(paymentMethod) LIKE LOWER(CONCAT('%', ?1, '%'))
                    ORDER BY createdAt ASC
                """;

        var panacheQuery = find(query, searchKeyword)
                .page(pageIndex, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Transaction>> findActiveTransactions(FindAllTransactionRequest req) {
        int pageIndex = req.getPage() != null && req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() != null && req.getPageSize() > 0 ? req.getPageSize() : 10;
        String searchKeyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    deletedAt IS NULL
                    AND (CAST(?1 AS string) IS NULL
                        OR CAST(id AS string) LIKE CONCAT('%', ?1, '%')
                        OR LOWER(paymentMethod) LIKE LOWER(CONCAT('%', ?1, '%')))
                    ORDER BY createdAt ASC
                """;

        var panacheQuery = find(query, searchKeyword)
                .page(pageIndex, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Transaction>> findTrashedTransactions(FindAllTransactionRequest req) {
        int pageIndex = req.getPage() != null && req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() != null && req.getPageSize() > 0 ? req.getPageSize() : 10;
        String searchKeyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    deletedAt IS NOT NULL
                    AND (CAST(?1 AS string) IS NULL
                        OR CAST(id AS string) LIKE CONCAT('%', ?1, '%')
                        OR LOWER(paymentMethod) LIKE LOWER(CONCAT('%', ?1, '%')))
                    ORDER BY deletedAt DESC
                """;

        var panacheQuery = find(query, searchKeyword)
                .page(pageIndex, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Transaction>> findTransactionsByMerchant(FindAllTransactionByMerchantRequest req) {
        int pageIndex = req.getPage() != null && req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() != null && req.getPageSize() > 0 ? req.getPageSize() : 10;
        Long merchantId = req.getMerchantId() != null ? req.getMerchantId().longValue() : null;
        String searchKeyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    deletedAt IS NULL
                    AND (CAST(?1 AS string) IS NULL
                        OR LOWER(paymentMethod) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR CAST(status AS string) LIKE LOWER(CONCAT('%', ?1, '%')))
                    AND (CAST(?2 AS long) IS NULL OR merchantId = ?2)
                    ORDER BY createdAt DESC
                """;

        var panacheQuery = find(query, searchKeyword, merchantId)
                .page(pageIndex, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<Transaction> findByTransactionId(Long transactionId) {
        return find("id", transactionId).firstResult();
    }

    public Uni<Transaction> findByOrderId(Long orderId) {
        return find("orderId", orderId).firstResult();
    }

    /**
     * Finds the active (non-trashed) transaction carrying the given client
     * idempotency key — used to replay-safe createTransaction (Fase 12).
     */
    public Uni<Transaction> findByIdempotencyKey(String idempotencyKey) {
        return find("deletedAt IS NULL AND idempotencyKey = ?1", idempotencyKey).firstResult();
    }
}
