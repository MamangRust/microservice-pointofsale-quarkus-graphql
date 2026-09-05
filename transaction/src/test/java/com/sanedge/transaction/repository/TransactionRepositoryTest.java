package com.sanedge.transaction.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.sanedge.common.enums.PaymentStatus;
import com.sanedge.transaction.domain.requests.FindAllTransactionByMerchantRequest;
import com.sanedge.transaction.domain.requests.FindAllTransactionRequest;
import com.sanedge.transaction.entity.Transaction;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@QuarkusTest
@RunOnVertxContext
class TransactionRepositoryTest {

    @Inject
    TransactionQueryRepository queryRepo;

    @Inject
    TransactionCommandRepository commandRepo;

    // ---------- helpers ----------
    private Uni<Transaction> persistTransaction(Long merchantId, String paymentMethod, PaymentStatus status) {
        Transaction tx = new Transaction();
        tx.setMerchantId(merchantId);
        tx.setPaymentMethod(paymentMethod);
        tx.setStatus(status);
        tx.setAmount(100000);
        tx.setOrderId(null); // optional
        tx.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        tx.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return queryRepo.persist(tx).map(t -> t);
    }

    private Uni<Transaction> persistTransaction(Long merchantId, String paymentMethod) {
        return persistTransaction(merchantId, paymentMethod, PaymentStatus.PENDING);
    }

    private Uni<Void> clean() {
        return queryRepo.deleteAll().replaceWithVoid();
    }

    private FindAllTransactionRequest findAllReq(int page, int size, String search) {
        FindAllTransactionRequest req = new FindAllTransactionRequest();
        req.setPage(page);
        req.setPageSize(size);
        req.setSearch(search == null ? "" : search);
        return req;
    }

    private FindAllTransactionByMerchantRequest findAllByMerchantReq(Long merchantId, int page, int size, String search) {
        FindAllTransactionByMerchantRequest req = new FindAllTransactionByMerchantRequest();
        req.setMerchantId(merchantId.intValue());
        req.setPage(page);
        req.setPageSize(size);
        req.setSearch(search == null ? "" : search);
        return req;
    }

    // ==================== Basic CRUD ====================

    @Test
    @WithTransaction
    Uni<Void> testCreateAndFindByTransactionId() {
        return clean()
                .chain(() -> persistTransaction(10L, "CREDIT", PaymentStatus.SUCCESS))
                .chain(tx -> queryRepo.findByTransactionId(tx.getTransactionId()))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getPaymentMethod()).isEqualTo("CREDIT");
                    assertThat(found.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByTransactionIdReturnsNullWhenNotFound() {
        return clean()
                .chain(() -> queryRepo.findByTransactionId(99999L))
                .invoke(found -> assertThat(found).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByOrderId() {
        return clean()
                .chain(() -> {
                    Transaction tx = new Transaction();
                    tx.setMerchantId(1L);
                    tx.setPaymentMethod("DEBIT");
                    tx.setStatus(PaymentStatus.PENDING);
                    tx.setAmount(50000);
                    tx.setOrderId(555L);
                    tx.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                    tx.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
                    return queryRepo.persist(tx);
                })
                .chain(tx -> queryRepo.findByOrderId(555L))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getOrderId()).isEqualTo(555L);
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByOrderIdReturnsNullWhenNotFound() {
        return clean()
                .chain(() -> queryRepo.findByOrderId(99999L))
                .invoke(found -> assertThat(found).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByIdempotencyKey() {
        return clean()
                .chain(() -> {
                    Transaction tx = new Transaction();
                    tx.setMerchantId(1L);
                    tx.setPaymentMethod("CASH");
                    tx.setStatus(PaymentStatus.PENDING);
                    tx.setAmount(50000);
                    tx.setIdempotencyKey("idem-repo-1");
                    tx.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                    tx.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
                    return queryRepo.persist(tx);
                })
                .chain(tx -> queryRepo.findByIdempotencyKey("idem-repo-1"))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getIdempotencyKey()).isEqualTo("idem-repo-1");
                })
                .chain(() -> queryRepo.findByIdempotencyKey("idem-repo-missing"))
                .invoke(notFound -> assertThat(notFound).isNull())
                .replaceWithVoid();
    }

    // ==================== Query - Search & Pagination ====================

    @Test
    @WithTransaction
    Uni<Void> testFindTransactionsWithSearchByPaymentMethod() {
        return clean()
                .chain(() -> persistTransaction(1L, "CREDIT"))
                .chain(() -> persistTransaction(1L, "DEBIT"))
                .chain(() -> persistTransaction(1L, "CASH"))
                .chain(() -> queryRepo.findTransactions(findAllReq(1, 10, "credit")))
                .invoke(result -> {
                    assertThat(result.getTotalRecords()).isEqualTo(1);
                    assertThat(result.getData().get(0).getPaymentMethod()).isEqualTo("CREDIT");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransactionsPagination() {
        return clean()
                .chain(() -> persistTransaction(1L, "A"))
                .chain(() -> persistTransaction(1L, "B"))
                .chain(() -> persistTransaction(1L, "C"))
                .chain(() -> persistTransaction(1L, "D"))
                .chain(() -> persistTransaction(1L, "E"))
                .chain(() -> queryRepo.findTransactions(findAllReq(1, 2, "")))
                .invoke(page1 -> {
                    assertThat(page1.getData()).hasSize(2);
                    assertThat(page1.getTotalRecords()).isEqualTo(5);
                })
                .chain(() -> queryRepo.findTransactions(findAllReq(3, 2, "")))
                .invoke(page3 -> assertThat(page3.getData()).hasSize(1))
                .replaceWithVoid();
    }

    // ==================== Active / Trashed filters ====================

    @Test
    @WithTransaction
    Uni<Void> testFindActiveTransactionsExcludesTrashed() {
        return clean()
                .chain(() -> persistTransaction(1L, "CREDIT"))
                .chain(() -> persistTransaction(1L, "DEBIT").chain(tx -> commandRepo.trashed(tx.getTransactionId()).replaceWithVoid()))
                .chain(() -> queryRepo.findActiveTransactions(findAllReq(1, 10, "")))
                .invoke(result -> assertThat(result.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTrashedTransactionsOnlyTrashed() {
        return clean()
                .chain(() -> persistTransaction(1L, "CREDIT"))
                .chain(() -> persistTransaction(1L, "DEBIT").chain(tx -> commandRepo.trashed(tx.getTransactionId()).replaceWithVoid()))
                .chain(() -> queryRepo.findTrashedTransactions(findAllReq(1, 10, "")))
                .invoke(result -> {
                    assertThat(result.getTotalRecords()).isEqualTo(1);
                    assertThat(result.getData().get(0).getPaymentMethod()).isEqualTo("DEBIT");
                })
                .replaceWithVoid();
    }

    // ==================== findByMerchant ====================

    @Test
    @WithTransaction
    Uni<Void> testFindTransactionsByMerchant() {
        return clean()
                .chain(() -> persistTransaction(100L, "CREDIT"))
                .chain(() -> persistTransaction(100L, "CASH"))
                .chain(() -> persistTransaction(200L, "DEBIT"))
                .chain(() -> queryRepo.findTransactionsByMerchant(findAllByMerchantReq(100L, 1, 10, "")))
                .invoke(result -> {
                    assertThat(result.getTotalRecords()).isEqualTo(2);
                    assertThat(result.getData().stream().allMatch(tx -> tx.getMerchantId().equals(100L))).isTrue();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransactionsByMerchantWithSearch() {
        return clean()
                .chain(() -> persistTransaction(50L, "QRIS", PaymentStatus.SUCCESS))
                .chain(() -> persistTransaction(50L, "BANK_TRANSFER", PaymentStatus.FAILED))
                .chain(() -> persistTransaction(50L, "CASH", PaymentStatus.PENDING))
                .chain(() -> queryRepo.findTransactionsByMerchant(findAllByMerchantReq(50L, 1, 10, "failed")))
                .invoke(result -> {
                    assertThat(result.getTotalRecords()).isEqualTo(1);
                    assertThat(result.getData().get(0).getPaymentMethod()).isEqualTo("BANK_TRANSFER");
                    assertThat(result.getData().get(0).getStatus()).isEqualTo(PaymentStatus.FAILED);
                })
                .replaceWithVoid();
    }

    // ==================== Soft Delete (Trash) ====================

    @Test
    @WithTransaction
    Uni<Void> testTrashTransaction() {
        return clean()
                .chain(() -> persistTransaction(1L, "CREDIT"))
                .chain(tx -> commandRepo.trashed(tx.getTransactionId()))
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
                .chain(() -> persistTransaction(1L, "CREDIT"))
                .chain(tx -> commandRepo.trashed(tx.getTransactionId())
                        .chain(() -> commandRepo.trashed(tx.getTransactionId())))
                .invoke(trashed -> {
                    assertThat(trashed).isNotNull();
                    assertThat(trashed.getDeletedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreTransaction() {
        return clean()
                .chain(() -> persistTransaction(1L, "CREDIT"))
                .chain(tx -> commandRepo.trashed(tx.getTransactionId())
                        .chain(() -> commandRepo.restore(tx.getTransactionId())))
                .invoke(restored -> {
                    assertThat(restored).isNotNull();
                    assertThat(restored.getDeletedAt()).isNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreNonExistentReturnsNull() {
        return clean()
                .chain(() -> commandRepo.restore(99999L))
                .invoke(restored -> assertThat(restored).isNull())
                .replaceWithVoid();
    }

    // ==================== Permanent Delete ====================

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanentAfterTrash() {
        return clean()
                .chain(() -> persistTransaction(1L, "CREDIT"))
                .chain(tx -> commandRepo.trashed(tx.getTransactionId())
                        .chain(() -> commandRepo.deletePermanent(tx.getTransactionId()))
                        .chain(perm -> queryRepo.findByTransactionId(tx.getTransactionId())))
                .invoke(found -> assertThat(found).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanentActiveReturnsNull() {
        return clean()
                .chain(() -> persistTransaction(1L, "CREDIT"))
                .chain(tx -> commandRepo.deletePermanent(tx.getTransactionId()))
                .invoke(result -> assertThat(result).isNull())
                .replaceWithVoid();
    }

    // ==================== Bulk Operations ====================

    @Test
    @WithTransaction
    Uni<Void> testRestoreAllDeleted() {
        return clean()
                .chain(() -> persistTransaction(1L, "A").chain(tx -> commandRepo.trashed(tx.getTransactionId()).replaceWithVoid()))
                .chain(() -> persistTransaction(1L, "B").chain(tx -> commandRepo.trashed(tx.getTransactionId()).replaceWithVoid()))
                .chain(() -> commandRepo.restoreAllDeleted())
                .invoke(result -> assertThat(result).isTrue())
                .chain(() -> queryRepo.findTrashedTransactions(findAllReq(1, 10, "")))
                .invoke(trashed -> assertThat(trashed.getTotalRecords()).isEqualTo(0))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeleteAllDeleted() {
        return clean()
                .chain(() -> persistTransaction(1L, "X").chain(tx -> commandRepo.trashed(tx.getTransactionId()).replaceWithVoid()))
                .chain(() -> persistTransaction(1L, "Y").chain(tx -> commandRepo.trashed(tx.getTransactionId()).replaceWithVoid()))
                .chain(() -> persistTransaction(1L, "Z")) // stays active
                .chain(() -> commandRepo.deleteAllDeleted())
                .invoke(result -> assertThat(result).isTrue())
                .chain(() -> queryRepo.findActiveTransactions(findAllReq(1, 10, "")))
                .invoke(active -> assertThat(active.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    // ==================== Edge Cases ====================

    @Test
    @WithTransaction
    Uni<Void> testEmptyDatabaseReturnsZeroRecords() {
        return clean()
                .chain(() -> queryRepo.findTransactions(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .chain(() -> queryRepo.findActiveTransactions(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .chain(() -> queryRepo.findTrashedTransactions(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testSearchNoMatchReturnsZero() {
        return clean()
                .chain(() -> persistTransaction(1L, "CREDIT"))
                .chain(() -> queryRepo.findTransactions(findAllReq(1, 10, "NOMATCH")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid();
    }
}