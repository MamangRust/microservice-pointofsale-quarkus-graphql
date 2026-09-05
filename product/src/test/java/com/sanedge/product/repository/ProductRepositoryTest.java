package com.sanedge.product.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.sanedge.product.domain.requests.FindAllProductByMerchantRequest;
import com.sanedge.product.domain.requests.FindAllProductByCategoryRequest;
import com.sanedge.product.domain.requests.FindAllProductRequest;
import com.sanedge.product.entity.Product;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@QuarkusTest
@RunOnVertxContext
class ProductRepositoryTest {

    @Inject
    ProductQueryRepository queryRepo;

    @Inject
    ProductCommandRepository commandRepo;

    // ---------- helpers ----------
    private Uni<Product> persistProduct(String name, Long merchantId, Long categoryId, int price, int stock) {
        Product p = new Product();
        p.setName(name);
        p.setMerchantId(merchantId);
        p.setCategoryId(categoryId);
        p.setPrice(price);
        p.setCountInStock(stock);
        p.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        p.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return queryRepo.persist(p).map(prod -> prod);
    }

    private Uni<Product> persistProduct(String name, Long merchantId, Long categoryId) {
        return persistProduct(name, merchantId, categoryId, 100, 50);
    }

    private Uni<Void> clean() {
        return queryRepo.deleteAll().replaceWithVoid();
    }

    private FindAllProductRequest findAllReq(int page, int size, String search) {
        FindAllProductRequest req = new FindAllProductRequest();
        req.setPage(page);
        req.setPageSize(size);
        req.setSearch(search == null ? "" : search);
        return req;
    }

    private FindAllProductByMerchantRequest findAllByMerchantReq(Long merchantId, int page, int size, String search) {
        FindAllProductByMerchantRequest req = new FindAllProductByMerchantRequest();
        req.setMerchantId(merchantId.intValue());
        req.setPage(page);
        req.setPageSize(size);
        req.setSearch(search);
        return req;
    }

    private FindAllProductByCategoryRequest findAllByCategoryReq(String categoryName, int page, int size, String search) {
        FindAllProductByCategoryRequest req = new FindAllProductByCategoryRequest();
        req.setCategoryName(categoryName);
        req.setPage(page);
        req.setPageSize(size);
        req.setSearch(search);
        return req;
    }

    // ==================== Basic CRUD ====================

    @Test
    @WithTransaction
    Uni<Void> testCreateAndFindById() {
        return clean()
                .chain(() -> persistProduct("Product One", 10L, 100L, 5000, 20))
                .chain(p -> queryRepo.findProductById(p.getProductId()))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getName()).isEqualTo("Product One");
                    assertThat(found.getMerchantId()).isEqualTo(10L);
                    assertThat(found.getCategoryId()).isEqualTo(100L);
                    assertThat(found.getPrice()).isEqualTo(5000);
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByIdReturnsNullWhenNotFound() {
        return clean()
                .chain(() -> queryRepo.findProductById(99999L))
                .invoke(found -> assertThat(found).isNull())
                .replaceWithVoid();
    }

    // ==================== Query - Search & Pagination ====================

    @Test
    @WithTransaction
    Uni<Void> testFindAllProductsWithSearch() {
        return clean()
                .chain(() -> persistProduct("Alpha", 1L, 1L))
                .chain(() -> persistProduct("Beta", 1L, 1L))
                .chain(() -> persistProduct("Gamma", 1L, 1L))
                .chain(() -> queryRepo.findAllProducts(findAllReq(1, 10, "beta")))
                .invoke(result -> {
                    assertThat(result.getTotalRecords()).isEqualTo(1);
                    assertThat(result.getData().get(0).getName()).isEqualTo("Beta");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindAllProductsPagination() {
        return clean()
                .chain(() -> persistProduct("A", 1L, 1L))
                .chain(() -> persistProduct("B", 1L, 1L))
                .chain(() -> persistProduct("C", 1L, 1L))
                .chain(() -> persistProduct("D", 1L, 1L))
                .chain(() -> persistProduct("E", 1L, 1L))
                .chain(() -> queryRepo.findAllProducts(findAllReq(1, 2, "")))
                .invoke(page1 -> {
                    assertThat(page1.getData()).hasSize(2);
                    assertThat(page1.getTotalRecords()).isEqualTo(5);
                })
                .chain(() -> queryRepo.findAllProducts(findAllReq(3, 2, "")))
                .invoke(page3 -> assertThat(page3.getData()).hasSize(1))
                .replaceWithVoid();
    }

    // ==================== Active / Trashed filters ====================

    @Test
    @WithTransaction
    Uni<Void> testFindActiveProductsExcludesTrashed() {
        return clean()
                .chain(() -> persistProduct("Active", 1L, 1L))
                .chain(() -> persistProduct("ToTrash", 1L, 1L)
                        .chain(p -> commandRepo.trashed(p.getProductId()).replaceWithVoid()))
                .chain(() -> queryRepo.findActiveProducts(findAllReq(1, 10, "")))
                .invoke(result -> assertThat(result.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTrashedProductsOnlyTrashed() {
        return clean()
                .chain(() -> persistProduct("Stay", 1L, 1L))
                .chain(() -> persistProduct("TrashMe", 1L, 1L)
                        .chain(p -> commandRepo.trashed(p.getProductId()).replaceWithVoid()))
                .chain(() -> queryRepo.findTrashedProducts(findAllReq(1, 10, "")))
                .invoke(result -> {
                    assertThat(result.getTotalRecords()).isEqualTo(1);
                    assertThat(result.getData().get(0).getName()).isEqualTo("TrashMe");
                })
                .replaceWithVoid();
    }

    // ==================== findByMerchant ====================

    @Test
    @WithTransaction
    Uni<Void> testFindProductsByMerchant() {
        return clean()
                .chain(() -> persistProduct("P1", 100L, 10L))
                .chain(() -> persistProduct("P2", 100L, 20L))
                .chain(() -> persistProduct("P3", 200L, 10L))
                .chain(() -> queryRepo.findProductsByMerchant(findAllByMerchantReq(100L, 1, 10, "")))
                .invoke(result -> {
                    assertThat(result.getTotalRecords()).isEqualTo(2);
                    assertThat(result.getData().stream().allMatch(p -> p.getMerchantId().equals(100L))).isTrue();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindProductsByMerchantWithCategoryAndPriceFilter() {
        return clean()
                .chain(() -> persistProduct("Cheap", 100L, 10L, 50, 100))
                .chain(() -> persistProduct("Mid",   100L, 20L, 200, 100))
                .chain(() -> persistProduct("Exp",   100L, 20L, 500, 100))
                .chain(() -> {
                    FindAllProductByMerchantRequest req = findAllByMerchantReq(100L, 1, 10, "");
                    req.setCategoryId(20);
                    req.setMinPrice(100);
                    req.setMaxPrice(400);
                    return queryRepo.findProductsByMerchant(req);
                })
                .invoke(result -> {
                    assertThat(result.getTotalRecords()).isEqualTo(1);
                    assertThat(result.getData().get(0).getName()).isEqualTo("Mid");
                })
                .replaceWithVoid();
    }

    // ==================== findByCategory ====================

    @Test
    @WithTransaction
    Uni<Void> testFindProductsByCategory() {
        return clean()
                .chain(() -> persistProduct("Phone", 1L, 50L))  // categoryId=50 assumed to be "Electronics"
                .chain(() -> persistProduct("Tablet", 1L, 51L))
                .chain(() -> queryRepo.findProductsByCategory(findAllByCategoryReq("Electronics", 1, 10, "")))
                .invoke(result -> {
                    assertThat(result).isNotNull();
                })
                .replaceWithVoid();
    }

    // ==================== Soft Delete (Trash) ====================

    @Test
    @WithTransaction
    Uni<Void> testTrashProduct() {
        return clean()
                .chain(() -> persistProduct("Trash", 1L, 1L))
                .chain(p -> commandRepo.trashed(p.getProductId()))
                .invoke(trashed -> {
                    assertThat(trashed).isNotNull();
                    assertThat(trashed.getDeletedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashAlreadyTrashedReturnsSame() {
        return clean()
                .chain(() -> persistProduct("Double", 1L, 1L))
                .chain(p -> commandRepo.trashed(p.getProductId())
                        .chain(() -> commandRepo.trashed(p.getProductId())))
                .invoke(second -> {
                    assertThat(second).isNotNull();
                    assertThat(second.getDeletedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreProduct() {
        return clean()
                .chain(() -> persistProduct("Restore", 1L, 1L))
                .chain(p -> commandRepo.trashed(p.getProductId())
                        .chain(() -> commandRepo.restore(p.getProductId())))
                .invoke(restored -> {
                    assertThat(restored).isNotNull();
                    assertThat(restored.getDeletedAt()).isNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreNotTrashedReturnsNull() {
        return clean()
                .chain(() -> persistProduct("ActiveR", 1L, 1L))
                .chain(p -> commandRepo.restore(p.getProductId()))
                .invoke(result -> assertThat(result).isNull())
                .replaceWithVoid();
    }

    // ==================== Permanent Delete ====================

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanentAfterTrash() {
        return clean()
                .chain(() -> persistProduct("DelPerm", 1L, 1L))
                .chain(p -> commandRepo.trashed(p.getProductId())
                        .chain(() -> commandRepo.deletePermanent(p.getProductId()))
                        .chain(perm -> queryRepo.findProductById(p.getProductId())))
                .invoke(found -> assertThat(found).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanentActiveReturnsNull() {
        return clean()
                .chain(() -> persistProduct("ActiveDel", 1L, 1L))
                .chain(p -> commandRepo.deletePermanent(p.getProductId()))
                .invoke(result -> assertThat(result).isNull())
                .replaceWithVoid();
    }

    // ==================== Bulk Operations ====================

    @Test
    @WithTransaction
    Uni<Void> testRestoreAllDeleted() {
        return clean()
                .chain(() -> persistProduct("A1", 1L, 1L).chain(p -> commandRepo.trashed(p.getProductId()).replaceWithVoid()))
                .chain(() -> persistProduct("A2", 1L, 1L).chain(p -> commandRepo.trashed(p.getProductId()).replaceWithVoid()))
                .chain(() -> commandRepo.restoreAllDeleted())
                .invoke(result -> assertThat(result).isTrue())
                .chain(() -> queryRepo.findTrashedProducts(findAllReq(1, 10, "")))
                .invoke(trashed -> assertThat(trashed.getTotalRecords()).isEqualTo(0))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeleteAllDeleted() {
        return clean()
                .chain(() -> persistProduct("D1", 1L, 1L).chain(p -> commandRepo.trashed(p.getProductId()).replaceWithVoid()))
                .chain(() -> persistProduct("D2", 1L, 1L).chain(p -> commandRepo.trashed(p.getProductId()).replaceWithVoid()))
                .chain(() -> persistProduct("Keep", 1L, 1L))
                .chain(() -> commandRepo.deleteAllDeleted())
                .invoke(result -> assertThat(result).isTrue())
                .chain(() -> queryRepo.findActiveProducts(findAllReq(1, 10, "")))
                .invoke(active -> assertThat(active.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    // ==================== Edge Cases ====================

    @Test
    @WithTransaction
    Uni<Void> testEmptyDatabaseReturnsZeroRecords() {
        return clean()
                .chain(() -> queryRepo.findAllProducts(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .chain(() -> queryRepo.findActiveProducts(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .chain(() -> queryRepo.findTrashedProducts(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testSearchNoMatchReturnsZero() {
        return clean()
                .chain(() -> persistProduct("Something", 1L, 1L))
                .chain(() -> queryRepo.findAllProducts(findAllReq(1, 10, "NOMATCH")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid();
    }
}