package com.sanedge.cashier.repository;

import com.sanedge.cashier.domain.requests.FindAllCashierMerchant;
import com.sanedge.cashier.domain.requests.FindAllCashiers;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.cashier.entity.Cashier;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@WithSession
public class CashierQueryRepository implements PanacheRepository<Cashier> {

    public Uni<PagedResult<Cashier>> findAllCashiers(FindAllCashiers req) {
        int page = req.getPage() != null && req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() != null && req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    CAST(?1 AS string) IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', ?1, '%'))
                    ORDER BY createdAt ASC
                """;

        var panacheQuery = find(query, keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Cashier>> findByMerchants(FindAllCashierMerchant req) {
        int page = req.getPage() != null && req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() != null && req.getPageSize() > 0 ? req.getPageSize() : 10;
        Long merchantId = req.getMerchantId() != null ? req.getMerchantId().longValue() : null;
        String keyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    deletedAt IS NULL
                    AND (CAST(?1 AS long) IS NULL OR merchantId = ?1)
                    AND (CAST(?2 AS string) IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', ?2, '%')))
                    ORDER BY createdAt ASC
                """;

        var panacheQuery = find(query, merchantId, keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Cashier>> findActiveCashiers(FindAllCashiers req) {
        int page = req.getPage() != null && req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() != null && req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    deletedAt IS NULL
                    AND (CAST(?1 AS string) IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', ?1, '%')))
                    ORDER BY createdAt ASC
                """;

        var panacheQuery = find(query, keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Cashier>> findTrashedCashiers(FindAllCashiers req) {
        int page = req.getPage() != null && req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() != null && req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    deletedAt IS NOT NULL
                    AND (CAST(?1 AS string) IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', ?1, '%')))
                    ORDER BY deletedAt DESC
                """;

        var panacheQuery = find(query, keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<Cashier> findByCashierId(Long cashierId) {
        return find("id", cashierId).firstResult();
    }

    public Uni<Cashier> findByNameAndMerchantId(String name, Long merchantId) {
        return find("LOWER(name) = LOWER(?1) AND merchantId = ?2 AND deletedAt IS NULL", name, merchantId).firstResult();
    }
}
