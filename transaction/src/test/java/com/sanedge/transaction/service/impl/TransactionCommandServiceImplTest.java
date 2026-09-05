package com.sanedge.transaction.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.enums.PaymentStatus;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.transaction.domain.requests.CreateTransactionRequest;
import com.sanedge.transaction.domain.requests.UpdateTransactionRequest;
import com.sanedge.transaction.domain.response.TransactionResponse;
import com.sanedge.transaction.domain.response.TransactionResponseDeleteAt;
import com.sanedge.transaction.entity.Outbox;
import com.sanedge.transaction.entity.Transaction;
import com.sanedge.transaction.repository.OutboxRepository;
import com.sanedge.transaction.repository.TransactionCommandRepository;
import com.sanedge.transaction.repository.TransactionQueryRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import pb.merchant.Merchant;
import pb.merchant.MutinyMerchantQueryServiceGrpc.MutinyMerchantQueryServiceStub;
import pb.order.MutinyOrderQueryServiceGrpc.MutinyOrderQueryServiceStub;
import pb.order.Order;
import pb.order_item.MutinyOrderItemServiceGrpc.MutinyOrderItemServiceStub;
import pb.order_item.OrderItem;

@ExtendWith(MockitoExtension.class)
class TransactionCommandServiceImplTest {

    @Mock private TransactionQueryRepository transactionQueryRepo;
    @Mock private TransactionCommandRepository transactionCommandRepo;
    @Mock private OutboxRepository outboxRepository;
    @Mock private RedisService redisService;
    @Mock private TracingMetrics tracingMetrics;

    @Mock private MutinyMerchantQueryServiceStub merchantQueryService;
    @Mock private MutinyOrderQueryServiceStub orderQueryService;
    @Mock private MutinyOrderItemServiceStub orderItemQueryService;

    private TransactionCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TransactionCommandServiceImpl(transactionQueryRepo, transactionCommandRepo, outboxRepository, redisService, tracingMetrics);
        service.notificationEmail = "admin@example.com";
        lenient().when(outboxRepository.persist(any(Outbox.class))).thenReturn(Uni.createFrom().nullItem());
        // inject gRPC stubs via field (they are field-injected in the service)
        service.merchantQueryService = merchantQueryService;
        service.orderQueryService = orderQueryService;
        service.orderItemQueryService = orderItemQueryService;

        // Lenient stub for traceAndMeasure
        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(3);
            return supplier.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());

        lenient().when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());
        lenient().when(transactionCommandRepo.deleteAllDeleted()).thenReturn(Uni.createFrom().item(true));
        lenient().when(transactionCommandRepo.restoreAllDeleted()).thenReturn(Uni.createFrom().item(true));

        // common gRPC stubs
        lenient().when(merchantQueryService.findByIdMerchant(any()))
                .thenReturn(Uni.createFrom().item(Merchant.ApiResponseMerchant.newBuilder()
                        .setStatus("success")
                        .setData(Merchant.MerchantResponse.newBuilder().setId(10).build())
                        .build()));
        lenient().when(orderQueryService.findById(any()))
                .thenReturn(Uni.createFrom().item(Order.ApiResponseOrder.newBuilder()
                        .setStatus("success")
                        .setData(Order.OrderResponse.newBuilder().setId(100).build())
                        .build()));
        // order items for total calculation
        lenient().when(orderItemQueryService.findOrderItemByOrder(any()))
                .thenReturn(Uni.createFrom().item(OrderItem.ApiResponsesOrderItem.newBuilder()
                        .setStatus("success")
                        .addData(OrderItem.OrderItemResponse.newBuilder().setQuantity(2).setPrice(5000).build())
                        .addData(OrderItem.OrderItemResponse.newBuilder().setQuantity(1).setPrice(10000).build())
                        .build()));
    }

    private Transaction createMockTransaction(Long id) {
        Transaction tx = new Transaction();
        tx.setTransactionId(id);
        tx.setOrderId(100L);
        tx.setMerchantId(10L);
        tx.setPaymentMethod("CREDIT");
        tx.setAmount(22200); // total 20000 + PPN 11% = 22200
        tx.setStatus(PaymentStatus.SUCCESS);
        tx.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        tx.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return tx;
    }

    private CreateTransactionRequest createReq() {
        CreateTransactionRequest req = new CreateTransactionRequest();
        req.setMerchantID(10);
        req.setOrderID(100);
        req.setAmount(22200);
        req.setPaymentMethod("CREDIT");
        return req;
    }

    private UpdateTransactionRequest updateReq() {
        UpdateTransactionRequest req = new UpdateTransactionRequest();
        req.setTransactionID(1);
        req.setMerchantID(10);
        req.setOrderID(100);
        req.setAmount(22200);
        req.setPaymentMethod("DEBIT");
        return req;
    }

    // ---------- create ----------
    @Nested
    @DisplayName("create tests")
    class CreateTests {
        @Test void success() {
            CreateTransactionRequest req = createReq();
            Transaction saved = createMockTransaction(1L);
            when(transactionCommandRepo.persist(any(Transaction.class))).thenReturn(Uni.createFrom().item(saved));
            ApiResponse<TransactionResponse> resp = service.create(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getAmount()).isEqualTo(22200);
        }

        @Test void insufficientAmount_returnsError() {
            CreateTransactionRequest req = createReq();
            req.setAmount(10000); // less than total 22000
            ApiResponse<TransactionResponse> resp = service.create(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Insufficient payment amount");
        }

        @Test void nullFields_returnsError() {
            CreateTransactionRequest req = new CreateTransactionRequest();
            ApiResponse<TransactionResponse> resp = service.create(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("All fields are required");
        }

        @Test void success_persistsOutboxEventInSameTx() {
            CreateTransactionRequest req = createReq();
            Transaction saved = createMockTransaction(1L);
            when(transactionCommandRepo.persist(any(Transaction.class))).thenReturn(Uni.createFrom().item(saved));

            ApiResponse<TransactionResponse> resp = service.create(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");

            ArgumentCaptor<Outbox> outboxCaptor = ArgumentCaptor.forClass(Outbox.class);
            verify(outboxRepository, org.mockito.Mockito.atLeastOnce()).persist(outboxCaptor.capture());
            List<Outbox> captured = outboxCaptor.getAllValues();
            assertThat(captured).hasSizeGreaterThanOrEqualTo(2);
            // First persist: email notification event
            Outbox emailOutbox = captured.get(0);
            assertThat(emailOutbox.getTopic()).isEqualTo("email-service-topic-transaction-create");
            assertThat(emailOutbox.getAggregateType()).isEqualTo("TRANSACTION");
            assertThat(emailOutbox.getAggregateId()).isEqualTo("1");
            assertThat(emailOutbox.getPayload()).contains("\"email\"");
            // Second persist: stats pipeline event
            Outbox statsOutbox = captured.get(1);
            assertThat(statsOutbox.getTopic()).isEqualTo("stats.pos.transaction.event");
            assertThat(statsOutbox.getDomain()).isEqualTo("transaction");
        }

        @Test void replayWithSameIdempotencyKey_returnsExistingWithoutInsert() {
            CreateTransactionRequest req = createReq();
            req.setIdempotencyKey("idem-abc-1");
            Transaction existing = createMockTransaction(7L);
            when(transactionQueryRepo.findByIdempotencyKey("idem-abc-1"))
                    .thenReturn(Uni.createFrom().item(existing));

            ApiResponse<TransactionResponse> resp = service.create(req).await().indefinitely();

            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.message()).contains("idempotent replay");
            assertThat(resp.data().getId()).isEqualTo(7);
            verify(transactionCommandRepo, never()).persist(any(Transaction.class));
            verify(outboxRepository, never()).persist(any(Outbox.class));
            verify(merchantQueryService, never()).findByIdMerchant(any());
        }

        @Test void create_persistsIdempotencyKey() {
            CreateTransactionRequest req = createReq();
            req.setIdempotencyKey("idem-abc-2");
            Transaction saved = createMockTransaction(1L);
            lenient().when(transactionQueryRepo.findByIdempotencyKey(anyString()))
                    .thenReturn(Uni.createFrom().nullItem());
            when(transactionCommandRepo.persist(any(Transaction.class)))
                    .thenReturn(Uni.createFrom().item(saved));

            ApiResponse<TransactionResponse> resp = service.create(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");

            ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionCommandRepo).persist(txCaptor.capture());
            assertThat(txCaptor.getValue().getIdempotencyKey()).isEqualTo("idem-abc-2");
        }

        @Test void invalidItemQuantity_doesNotPersistOutbox() {
            CreateTransactionRequest req = createReq();
            when(orderItemQueryService.findOrderItemByOrder(any()))
                    .thenReturn(Uni.createFrom().item(OrderItem.ApiResponsesOrderItem.newBuilder()
                            .setStatus("success")
                            .addData(OrderItem.OrderItemResponse.newBuilder().setQuantity(0).setPrice(5000).build())
                            .build()));

            ApiResponse<TransactionResponse> resp = service.create(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            verify(outboxRepository, never()).persist(any(Outbox.class));
        }
    }

    // ---------- update ----------
    @Nested
    @DisplayName("update tests")
    class UpdateTests {
        @Test void success() {
            UpdateTransactionRequest req = updateReq();
            Transaction existing = createMockTransaction(1L);
            existing.setStatus(PaymentStatus.PENDING);
            Transaction saved = createMockTransaction(1L);
            saved.setStatus(PaymentStatus.PENDING);
            saved.setPaymentMethod("DEBIT");
            when(transactionQueryRepo.findByTransactionId(anyLong())).thenReturn(Uni.createFrom().item(existing));
            when(transactionCommandRepo.persist(any(Transaction.class))).thenReturn(Uni.createFrom().item(saved));

            ApiResponse<TransactionResponse> resp = service.update(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
        }

        @Test void transactionNotFound_returnsError() {
            UpdateTransactionRequest req = updateReq();
            when(transactionQueryRepo.findByTransactionId(anyLong())).thenReturn(Uni.createFrom().nullItem());

            ApiResponse<TransactionResponse> resp = service.update(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Transaction not found");
        }

        @Test void insufficientAmount_returnsError() {
            UpdateTransactionRequest req = updateReq();
            req.setAmount(5000);
            Transaction tx = createMockTransaction(1L);
            tx.setStatus(PaymentStatus.PENDING);
            when(transactionQueryRepo.findByTransactionId(anyLong())).thenReturn(Uni.createFrom().item(tx));
            ApiResponse<TransactionResponse> resp = service.update(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Insufficient payment amount");
        }
    }

    // ---------- trash ----------
    @Nested
    @DisplayName("trash tests")
    class TrashTests {
        @Test void success() {
            Integer id = 1;
            when(transactionCommandRepo.trashed(anyLong())).thenReturn(Uni.createFrom().item(createMockTransaction(1L)));
            ApiResponse<TransactionResponseDeleteAt> resp = service.trash(id).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
        }

        @Test void notFound_returnsError() {
            when(transactionCommandRepo.trashed(anyLong())).thenReturn(Uni.createFrom().nullItem());
            ApiResponse<TransactionResponseDeleteAt> resp = service.trash(1).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Transaction not found");
        }
    }

    // ---------- restore ----------
    @Nested
    @DisplayName("restore tests")
    class RestoreTests {
        @Test void success() {
            Integer id = 1;
            when(transactionCommandRepo.restore(anyLong())).thenReturn(Uni.createFrom().item(createMockTransaction(1L)));
            ApiResponse<TransactionResponseDeleteAt> resp = service.restore(id).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
        }

        @Test void notFound_returnsError() {
            when(transactionCommandRepo.restore(anyLong())).thenReturn(Uni.createFrom().nullItem());
            ApiResponse<TransactionResponseDeleteAt> resp = service.restore(1).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Transaction not found");
        }
    }

    // ---------- delete ----------
    @Nested
    @DisplayName("delete tests")
    class DeleteTests {
        @Test void success() {
            Integer id = 1;
            when(transactionCommandRepo.deletePermanent(anyLong())).thenReturn(Uni.createFrom().item(createMockTransaction(1L)));
            ApiResponse<Boolean> resp = service.delete(id).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data()).isTrue();
        }

        @Test void notFound_returnsError() {
            when(transactionCommandRepo.deletePermanent(anyLong())).thenReturn(Uni.createFrom().nullItem());
            ApiResponse<Boolean> resp = service.delete(1).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Transaction not found");
        }
    }

    // ---------- restoreAll ----------
    @Nested
    @DisplayName("restoreAll tests")
    class RestoreAllTests {
        @Test void success() {
            ApiResponse<Boolean> resp = service.restoreAll().await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data()).isTrue();
        }

        @Test void noTrashed_throwsException() {
            when(transactionCommandRepo.restoreAllDeleted()).thenReturn(Uni.createFrom().item(false));
            assertThatThrownBy(() -> service.restoreAll().await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No transactions found in trash");
        }
    }

    // ---------- deleteAll ----------
    @Nested
    @DisplayName("deleteAll tests")
    class DeleteAllTests {
        @Test void success() {
            ApiResponse<Boolean> resp = service.deleteAll().await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data()).isTrue();
        }

        @Test void noTrashed_throwsException() {
            when(transactionCommandRepo.deleteAllDeleted()).thenReturn(Uni.createFrom().item(false));
            assertThatThrownBy(() -> service.deleteAll().await().indefinitely())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No transactions found in trash");
        }
    }
}