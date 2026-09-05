package com.sanedge.merchant.repository;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.merchant.domain.requests.FindAllMerchantDocuments;
import com.sanedge.merchant.entity.MerchantDocument;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MerchantDocumentQueryRepository implements PanacheRepository<MerchantDocument> {

    public Uni<PagedResult<MerchantDocument>> findDocuments(FindAllMerchantDocuments req) {
        int pageIndex = req.getPage() > 0 ? req.getPage() - 1 : 0;
        String searchKeyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    CAST(?1 AS string) IS NULL
                    OR LOWER(documentType) LIKE LOWER(CONCAT('%', ?1, '%'))
                    OR LOWER(documentUrl) LIKE LOWER(CONCAT('%', ?1, '%'))
                    OR LOWER(status) LIKE LOWER(CONCAT('%', ?1, '%'))
                    OR LOWER(note) LIKE LOWER(CONCAT('%', ?1, '%'))
                """;

        var panacheQuery = find(query, Sort.ascending("documentId"), searchKeyword)
                .page(pageIndex, req.getPageSize());

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<MerchantDocument>> findActiveDocuments(FindAllMerchantDocuments req) {
        int pageIndex = req.getPage() > 0 ? req.getPage() - 1 : 0;
        String searchKeyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    deletedAt IS NULL
                    AND (
                        CAST(?1 AS string) IS NULL
                        OR LOWER(documentType) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(documentUrl) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(status) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(note) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.ascending("documentId"), searchKeyword)
                .page(pageIndex, req.getPageSize());

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<MerchantDocument>> findTrashedDocuments(FindAllMerchantDocuments req) {
        int pageIndex = req.getPage() > 0 ? req.getPage() - 1 : 0;
        String searchKeyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    deletedAt IS NOT NULL
                    AND (
                        CAST(?1 AS string) IS NULL
                        OR LOWER(documentType) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(documentUrl) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(status) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(note) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.descending("documentId"), searchKeyword)
                .page(pageIndex, req.getPageSize());

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<MerchantDocument> findDocumentById(Long documentId) {
        return find("documentId = ?1 AND deletedAt IS NULL", documentId).firstResult();
    }
}
