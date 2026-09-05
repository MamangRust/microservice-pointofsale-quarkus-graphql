package com.sanedge.merchant.repository;

import java.util.List;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.merchant.domain.requests.FindAllMerchants;
import com.sanedge.merchant.entity.Merchant;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MerchantQueryRepository implements PanacheRepository<Merchant> {

    public Uni<Boolean> existsByName(String name) {
        return count("LOWER(name) = LOWER(?1) AND deletedAt IS NULL", name).map(c -> c > 0);
    }

    public Uni<PagedResult<Merchant>> findMerchants(FindAllMerchants req) {
        int pageIndex = req.getPage() > 0 ? req.getPage() - 1 : 0;
        String searchKeyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    CAST(?1 AS string) IS NULL
                    OR LOWER(name) LIKE LOWER(CONCAT('%', ?1, '%'))
                    OR LOWER(apiKey) LIKE LOWER(CONCAT('%', ?1, '%'))
                    OR LOWER(CAST(status AS string)) LIKE LOWER(CONCAT('%', ?1, '%'))
                """;

        var panacheQuery = find(query, Sort.ascending("merchantId"), searchKeyword)
                .page(pageIndex, req.getPageSize());

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Merchant>> findActiveMerchants(FindAllMerchants req) {
        int pageIndex = req.getPage() > 0 ? req.getPage() - 1 : 0;
        String searchKeyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    deletedAt IS NULL
                    AND (
                        CAST(?1 AS string) IS NULL
                        OR LOWER(name) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(apiKey) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(CAST(status AS string)) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.ascending("merchantId"), searchKeyword)
                .page(pageIndex, req.getPageSize());

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Merchant>> findTrashedMerchants(FindAllMerchants req) {
        int pageIndex = req.getPage() > 0 ? req.getPage() - 1 : 0;
        String searchKeyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    deletedAt IS NOT NULL
                    AND (
                        CAST(?1 AS string) IS NULL
                        OR LOWER(name) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(apiKey) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(CAST(status AS string)) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.descending("merchantId"), searchKeyword)
                .page(pageIndex, req.getPageSize());

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<Merchant> findMerchantById(Long merchantId) {
        return find("merchantId = ?1 AND deletedAt IS NULL", merchantId).firstResult();
    }

    public Uni<Merchant> findByApiKey(String apiKey) {
        return find("apiKey = ?1 AND deletedAt IS NULL", apiKey).firstResult();
    }

    public Uni<Merchant> findByName(String name) {
        return find("name = ?1 AND deletedAt IS NULL", name).firstResult();
    }

    public Uni<List<Merchant>> findByUserId(Long userId) {
        if (userId == null) {
            return Uni.createFrom().item(List.of());
        }
        return list("userId = ?1 AND deletedAt IS NULL", userId.intValue());
    }
}
