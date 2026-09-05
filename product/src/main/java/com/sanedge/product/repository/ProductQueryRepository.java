package com.sanedge.product.repository;

import com.sanedge.product.domain.requests.FindAllProductByMerchantRequest;
import com.sanedge.product.domain.requests.FindAllProductRequest;
import com.sanedge.product.domain.requests.FindAllProductByCategoryRequest;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.product.entity.Product;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProductQueryRepository implements PanacheRepository<Product> {

    public Uni<PagedResult<Product>> findAllProducts(FindAllProductRequest req) {
        int pageIndex = req.getPage() > 0 ? req.getPage() - 1 : 0;
        String searchKeyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    CAST(?1 AS string) IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', ?1, '%'))
                    ORDER BY createdAt ASC
                """;

        var panacheQuery = find(query, searchKeyword)
                .page(pageIndex, req.getPageSize());

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Product>> findActiveProducts(FindAllProductRequest req) {
        int pageIndex = req.getPage() > 0 ? req.getPage() - 1 : 0;
        String searchKeyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    deletedAt IS NULL
                    AND (CAST(?1 AS string) IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', ?1, '%')))
                    ORDER BY createdAt ASC
                """;

        var panacheQuery = find(query, searchKeyword)
                .page(pageIndex, req.getPageSize());

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Product>> findTrashedProducts(FindAllProductRequest req) {
        int pageIndex = req.getPage() > 0 ? req.getPage() - 1 : 0;
        String searchKeyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    deletedAt IS NOT NULL
                    AND (CAST(?1 AS string) IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', ?1, '%')))
                    ORDER BY deletedAt DESC
                """;

        var panacheQuery = find(query, searchKeyword)
                .page(pageIndex, req.getPageSize());

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<Product> findProductById(Long productId) {
        return find("id", productId).firstResult();
    }

    public Uni<PagedResult<Product>> findProductsByMerchant(FindAllProductByMerchantRequest req) {
        int pageIndex = req.getPage() != null && req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() != null && req.getPageSize() > 0 ? req.getPageSize() : 10;
        Long merchantId = req.getMerchantId() != null ? req.getMerchantId().longValue() : null;
        String searchKeyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;
        Long categoryId = (req.getCategoryId() != null && req.getCategoryId() > 0) ? req.getCategoryId().longValue() : null;
        Integer minPrice = req.getMinPrice();
        Integer maxPrice = req.getMaxPrice();

        var query = """
                    deletedAt IS NULL
                    AND merchantId = ?1
                    AND (CAST(?2 AS string) IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', ?2, '%')))
                    AND (CAST(?3 AS long) IS NULL OR categoryId = ?3)
                    AND (CAST(?4 AS big_decimal) IS NULL OR price >= ?4)
                    AND (CAST(?5 AS big_decimal) IS NULL OR price <= ?5)
                    ORDER BY createdAt ASC
                """;

        var panacheQuery = find(query, merchantId, searchKeyword, categoryId, minPrice, maxPrice)
                .page(pageIndex, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Product>> findProductsByCategory(FindAllProductByCategoryRequest req) {
        int pageIndex = req.getPage() > 0 ? req.getPage() - 1 : 0;
        String searchKeyword = (req.getSearch() != null && !req.getSearch().trim().isEmpty()) ? req.getSearch().trim() : null;

        var query = """
                    SELECT p FROM Product p, Category c
                    WHERE p.categoryId = c.id
                    AND p.deletedAt IS NULL
                    AND LOWER(c.name) = LOWER(?1)
                    AND (CAST(?2 AS string) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', ?2, '%')))
                    AND (CAST(?3 AS big_decimal) IS NULL OR p.price >= ?3)
                    AND (CAST(?4 AS big_decimal) IS NULL OR p.price <= ?4)
                    ORDER BY p.createdAt ASC
                """;

        var panacheQuery = find(query, req.getCategoryName(), searchKeyword, req.getMinPrice(), req.getMaxPrice())
                .page(pageIndex, req.getPageSize());

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }
}
