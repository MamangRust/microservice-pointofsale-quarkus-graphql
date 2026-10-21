package com.sanedge.transaction.service.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.sanedge.transaction.service.KafkaService;
import com.sanedge.transaction.service.TransactionCommandService;

import com.sanedge.common.adapter.merchant.MerchantQueryPort;
import com.sanedge.common.adapter.model.Order;
import com.sanedge.common.adapter.order.OrderQueryPort;
import com.sanedge.common.adapter.order_item.OrderItemQueryPort;
import com.sanedge.common.event.EventTopics;
import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TransactionCommandServiceImpl implements TransactionCommandService {
        private static final Logger logger = LoggerFactory.getLogger(TransactionCommandServiceImpl.class);

        private final TransactionQueryRepository transactionQueryRepository;
        private final TransactionCommandRepository transactionCommandRepository;
        private final OutboxRepository outboxRepository;
        private final RedisService redisService;
        private final TracingMetrics tracingMetrics;

        @ConfigProperty(name = "notification.transaction.email", defaultValue = "admin@example.com")
        String notificationEmail;

        private final MerchantQueryPort merchantQueryPort;
        private final OrderQueryPort orderQueryPort;
        private final OrderItemQueryPort orderItemQueryPort;
        private final KafkaService kafkaService;

        @Inject
        public TransactionCommandServiceImpl(TransactionQueryRepository transactionQueryRepository,
                        TransactionCommandRepository transactionCommandRepository,
                        OutboxRepository outboxRepository,
                        RedisService redisService,
                        TracingMetrics tracingMetrics,
                        MerchantQueryPort merchantQueryPort,
                        OrderQueryPort orderQueryPort,
                        OrderItemQueryPort orderItemQueryPort,
                        KafkaService kafkaService) {
                this.transactionQueryRepository = transactionQueryRepository;
                this.transactionCommandRepository = transactionCommandRepository;
                this.outboxRepository = outboxRepository;
                this.redisService = redisService;
                this.tracingMetrics = tracingMetrics;
                this.merchantQueryPort = merchantQueryPort;
                this.orderQueryPort = orderQueryPort;
                this.orderItemQueryPort = orderItemQueryPort;
                this.kafkaService = kafkaService;
        }

        private Uni<Void> clearCache(Long transactionId, Long orderId) {
                String idKey = "transaction:id:" + transactionId;
                String orderKey = "transaction:order:" + orderId;

                return Uni.combine().all().unis(
                                redisService.deleteReactive(idKey),
                                redisService.deleteReactive(orderKey)).discardItems();
        }    @Override
    @WithTransaction
    public Uni<ApiResponse<TransactionResponse>> create(CreateTransactionRequest req) {
        logger.info("Creating new transaction | orderId={}, merchantId={}", req.getOrderID(),
                        req.getMerchantID());

        if (req.getMerchantID() == null || req.getOrderID() == null
                        || req.getAmount() == null
                        || req.getPaymentMethod() == null) {
                logger.error("All fields are required");
                return Uni.createFrom().item(new ApiResponse<>("error",
                                "All fields are required",
                                (TransactionResponse) null));
        }

        // Idempotency (Fase 12): a client-sent key makes create replay-safe.
        // An existing active row with the same key short-circuits the whole
        // validation + insert chain and returns the stored transaction.
        String idempotencyKey = req.getIdempotencyKey();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                return transactionQueryRepository.findByIdempotencyKey(idempotencyKey)
                                .chain(existing -> {
                                        if (existing != null) {
                                                logger.info("Idempotent replay | key={} transactionId={}",
                                                                idempotencyKey, existing.getTransactionId());
                                                return Uni.createFrom().item(ApiResponse.success(
                                                                "Transaction already exists (idempotent replay)",
                                                                TransactionResponse.from(existing)));
                                        }
                                        return doCreate(req);
                                });
        }
        return doCreate(req);
    }

    private Uni<ApiResponse<TransactionResponse>> doCreate(CreateTransactionRequest req) {
        Attributes attrs = Attributes.builder()
                        .put("merchantId", req.getMerchantID())
                        .put("orderId", req.getOrderID())
                        .build();

        return runTraced("createTransaction", "create_transaction", attrs,
                        () -> {

                                        return merchantQueryPort.findByMerchantId(req.getMerchantID())
                                                        .onFailure().recoverWithItem(err -> null)
                                                        .onItem().transformToUni(merchant -> {
                                                                if (merchant == null) {
                                                                        logger.error("Merchant not found | merchantId={}",
                                                                                        req.getMerchantID());
                                                                        return Uni.createFrom().item(new ApiResponse<>(
                                                                                        "error", "Merchant not found",
                                                                                        (TransactionResponse) null));
                                                                }

                                                                return orderQueryPort.findById(req.getOrderID())
                                                                                .onFailure().recoverWithItem(err -> null)
                                                                                .onItem().transformToUni(order -> {
                                                                                        if (order == null) {
                                                                                                logger.error("Order not found | orderId={}",
                                                                                                                req.getOrderID());
                                                                                                return Uni.createFrom()
                                                                                                                .item(new ApiResponse<>(
                                                                                                                                "error",
                                                                                                                                "Order not found",
                                                                                                                                (TransactionResponse) null));
                                                                                        }

                                                                                        return orderItemQueryPort
                                                                                                        .findOrderItemByOrder(req.getOrderID())
                                                                                                        .onItem()
                                                                                                        .transformToUni(orderItemsResp -> {
                                                                                                                if (orderItemsResp == null
                                                                                                                                || orderItemsResp
                                                                                                                                .isEmpty()) {
                                                                                                                        logger.error("No order items found | orderId={}",
                                                                                                                                req.getOrderID());
                                                                                                                        return Uni.createFrom()
                                                                                                                                .item(new ApiResponse<>(
                                                                                                                                "error",
                                                                                                                                "No order items found",
                                                                                                                                (TransactionResponse) null));
                                                                                                                }

                                                                                                                int totalAmount = 0;
                                                                                                                for (var item : orderItemsResp) {
                                                                                                                        if (item.quantity() <= 0) {
                                                                                                                                return Uni.createFrom()
                                                                                                                                                .item(new ApiResponse<>(
                                                                                                                                                                "error",
                                                                                                                                                                "Invalid order item quantity",
                                                                                                                                                                (TransactionResponse) null));
                                                                                                                        }
                                                                                                                        totalAmount += item
                                                                                                                                .price()
                                                                                                                                * item.quantity();
                                                                                                                }
                                                                                                                int ppn = totalAmount
                                                                                                                                * 11
                                                                                                                                / 100;
                                                                                                                int totalAmountWithTax = totalAmount
                                                                                                                                + ppn;

                                                                                                                String paymentStatus = req
                                                                                                                                .getAmount() >= totalAmountWithTax
                                                                                                                                                ? "success"
                                                                                                                                                : "failed";
                                                                                                                if ("failed".equals(
                                                                                                                                paymentStatus)) {
                                                                                                                        logger.error("Insufficient payment amount | amount={}, required={}",
                                                                                                                                        req.getAmount(),
                                                                                                                                        totalAmountWithTax);
                                                                                                                        return Uni.createFrom()
                                                                                                                                        .item(new ApiResponse<>(
                                                                                                                                                        "error",
                                                                                                                                                        "Insufficient payment amount",
                                                                                                                                                        (TransactionResponse) null));
                                                                                                                }

                                                                                                                req.setAmount(totalAmountWithTax);
                                                                                                                req.setPaymentStatus(
                                                                                                                                paymentStatus);

                                                                                                                Transaction transaction = new Transaction();
                                                                                                                transaction.setOrderId(
                                                                                                                                req.getOrderID().longValue());
                                                                                                                transaction.setMerchantId(
                                                                                                                                req.getMerchantID()
                                                                                                                                                .longValue());
                                                                                                                transaction.setPaymentMethod(
                                                                                                                                req.getPaymentMethod());
                                                                                                                transaction.setAmount(
                                                                                                                                req.getAmount());
                                                                                                                transaction.setStatus(
                                                                                                                                PaymentStatus.fromValue(
                                                                                                                                                req.getPaymentStatus()));
                                                                                                                transaction.setIdempotencyKey(
                                                                                                                                req.getIdempotencyKey());
                                                                                                                transaction.setCreatedAt(
                                                                                                                                Timestamp.valueOf(
                                                                                                                                                LocalDateTime.now()));
                                                                                                                transaction.setUpdatedAt(
                                                                                                                                Timestamp.valueOf(
                                                                                                                                                LocalDateTime.now()));

                                                                                                                return transactionCommandRepository
                                                                                                                                .persist(transaction)
                                                                                                                                .chain(savedTx -> persistOutboxEvent(savedTx)
                                                                                                                                .chain(() -> persistTransactionStats(savedTx, order))
                                                                                                                                .chain(() -> clearCache(
                                                                                                                                                                savedTx.getTransactionId(),
                                                                                                                                                                savedTx.getOrderId())
                                                                                                                                                                .map(v -> {
                                                                                                                                                                        logger.info("Transaction created successfully | transactionId={}",
                                                                                                                                                                                        savedTx.getTransactionId());
                                                                                                                                                                        return ApiResponse
                                                                                                                                                                                        .success("Transaction created successfully",
                                                                                                                                                                                                        TransactionResponse
                                                                                                                                                                                                                        .from(savedTx));
                                                                                                                                                                })))
                                                                                                                                .onFailure(err -> isIdempotencyConflict(err)
                                                                                                                                                && req.getIdempotencyKey() != null
                                                                                                                                                && !req.getIdempotencyKey().isBlank())
                                                                                                                                .recoverWithUni(err -> transactionQueryRepository
                                                                                                                                                .findByIdempotencyKey(req.getIdempotencyKey())
                                                                                                                                                .map(existing -> {
                                                                                                                                                        if (existing == null) {
                                                                                                                                                                throw new IllegalStateException(
                                                                                                                                                                                "Idempotency conflict but no existing transaction found for key="
                                                                                                                                                                                                + req.getIdempotencyKey());
                                                                                                                                                        }
                                                                                                                                                        logger.info("Idempotent replay after unique-violation | key={} transactionId={}",
                                                                                                                                                                        req.getIdempotencyKey(),
                                                                                                                                                                        existing.getTransactionId());
                                                                                                                                                        return ApiResponse.success(
                                                                                                                                                                        "Transaction already exists (idempotent replay)",
                                                                                                                                                                        TransactionResponse.from(existing));
                                                                                                                                                }));
                                                                                                        });
                                                                                });
                                                        });
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<TransactionResponse>> update(UpdateTransactionRequest req) {
                logger.info("Updating transaction | transactionId={}", req.getTransactionID());
                Attributes attrs = Attributes.builder()
                                .put("transactionId", req.getTransactionID())
                                .build();

                return runTraced("updateTransaction", "update_transaction", attrs,
                                () -> {
                                        if (req.getTransactionID() == null || req.getMerchantID() == null
                                                        || req.getOrderID() == null
                                                        || req.getAmount() == null || req.getPaymentMethod() == null) {
                                                return Uni.createFrom()
                                                                .item(new ApiResponse<>("error",
                                                                                "All fields are required",
                                                                                (TransactionResponse) null));
                                        }

                                        return transactionQueryRepository
                                                        .findByTransactionId(req.getTransactionID().longValue())
                                                        .chain(existingTx -> {
                                                                if (existingTx == null) {
                                                                        logger.error("Transaction not found | transactionId={}",
                                                                                        req.getTransactionID());
                                                                        return Uni.createFrom()
                                                                                        .item(new ApiResponse<>("error",
                                                                                                        "Transaction not found",
                                                                                                        (TransactionResponse) null));
                                                                }

                                                                if (PaymentStatus.SUCCESS.equals(existingTx.getStatus())
                                                                                || PaymentStatus.REFUNDED.equals(
                                                                                                existingTx.getStatus())) {
                                                                        logger.error("Transaction cannot be modified | transactionId={}",
                                                                                        req.getTransactionID());
                                                                        return Uni.createFrom().item(new ApiResponse<>(
                                                                                        "error",
                                                                                        "Transaction cannot be modified",
                                                                                        (TransactionResponse) null));
                                                                }

                                                                return merchantQueryPort.findByMerchantId(req.getMerchantID())
                                                                                .onFailure().recoverWithItem(err -> null)
                                                                                .onItem()
                                                                                .transformToUni(merchant -> {
                                                                                        if (merchant == null) {
                                                                                                logger.error("Merchant not found | merchantId={}",
                                                                                                                req.getMerchantID());
                                                                                                return Uni.createFrom()
                                                                                                                .item(new ApiResponse<>(
                                                                                                                                "error",
                                                                                                                                "Merchant not found",
                                                                                                                                (TransactionResponse) null));
                                                                                        }

                                                                                        return orderQueryPort
                                                                                                        .findById(req.getOrderID())
                                                                                                        .onFailure().recoverWithItem(err -> null)
                                                                                                        .onItem()
                                                                                                        .transformToUni(order -> {
                                                                                                                if (order == null) {
                                                                                                                        logger.error("Order not found | orderId={}",
                                                                                                                                req.getOrderID());
                                                                                                                        return Uni.createFrom()
                                                                                                                                .item(new ApiResponse<>(
                                                                                                                                                "error",
                                                                                                                                                "Order not found",
                                                                                                                                                (TransactionResponse) null));
                                                                                                                }

                                                                                                                return orderItemQueryPort
                                                                                                                                .findOrderItemByOrder(req.getOrderID())
                                                                                                                                .onItem()
                                                                                                                                .transformToUni(orderItemsResp -> {
                                                                                                                                        if (orderItemsResp == null
                                                                                                                                                || orderItemsResp
                                                                                                                                                        .isEmpty()) {
                                                                                                                                                logger.error("No order items found | orderId={}",
                                                                                                                                                        req.getOrderID());
                                                                                                                                                return Uni.createFrom()
                                                                                                                                                                .item(new ApiResponse<>(
                                                                                                                                                                                "error",
                                                                                                                                                                                "No order items found",
                                                                                                                                                                                (TransactionResponse) null));
                                                                                                                                        }

                                                                                                                                        int totalAmount = 0;
                                                                                                                                        for (var item : orderItemsResp) {
                                                                                                                                                if (item.quantity() <= 0) {
                                                                                                                                                        return Uni.createFrom()
                                                                                                                                                                        .item(new ApiResponse<>(
                                                                                                                                                                                        "error",
                                                                                                                                                                                        "Invalid order item quantity",
                                                                                                                                                                                        (TransactionResponse) null));
                                                                                                                                                }
                                                                                                                                                totalAmount += item
                                                                                                                                                        .price()
                                                                                                                                                        * item.quantity();
                                                                                                                                        }
                                                                                                                                        int ppn = totalAmount
                                                                                                                                                        * 11
                                                                                                                                                        / 100;
                                                                                                                                        int totalAmountWithTax = totalAmount
                                                                                                                                                        + ppn;

                                                                                                                                        String paymentStatus = req
                                                                                                                                                        .getAmount() >= totalAmountWithTax
                                                                                                                                                                        ? "success"
                                                                                                                                                                        : "failed";
                                                                                                                                        if ("failed".equals(
                                                                                                                                                        paymentStatus)) {
                                                                                                                                                logger.error("Insufficient payment amount | amount={}, required={}",
                                                                                                                                                                req.getAmount(),
                                                                                                                                                                totalAmountWithTax);
                                                                                                                                                return Uni.createFrom()
                                                                                                                                                                .item(new ApiResponse<>(
                                                                                                                                                                                "error",
                                                                                                                                                                                "Insufficient payment amount",
                                                                                                                                                                                (TransactionResponse) null));
                                                                                                                                        }

                                                                                                                                        req.setAmount(totalAmountWithTax);
                                                                                                                                        req.setPaymentStatus(
                                                                                                                                                        paymentStatus);

                                                                                                                                        existingTx.setOrderId(
                                                                                                                                                        req.getOrderID().longValue());
                                                                                                                                        existingTx.setMerchantId(
                                                                                                                                                        req.getMerchantID()
                                                                                                                                                                        .longValue());
                                                                                                                                        existingTx.setPaymentMethod(
                                                                                                                                                        req.getPaymentMethod());
                                                                                                                                        existingTx.setAmount(
                                                                                                                                                        req.getAmount());
                                                                                                                                        existingTx.setStatus(
                                                                                                                                                        PaymentStatus.fromValue(
                                                                                                                                                                        req.getPaymentStatus()));
                                                                                                                                        existingTx.setUpdatedAt(
                                                                                                                                                        Timestamp.valueOf(
                                                                                                                                                                        LocalDateTime.now()));

                                                                                                                                        return transactionCommandRepository
                                                                                                                                                        .persist(existingTx)
                                                                                                                                                        .chain(savedTx -> clearCache(
                                                                                                                                                                        savedTx.getTransactionId(),
                                                                                                                                                                        savedTx.getOrderId())
                                                                                                                                                                        .map(v -> {
                                                                                                                                                                                logger.info("Transaction updated successfully | transactionId={}",
                                                                                                                                                                                                savedTx.getTransactionId());
                                                                                                                                                                                return ApiResponse
                                                                                                                                                                                                .success("Transaction updated successfully",
                                                                                                                                                                                                                TransactionResponse
                                                                                                                                                                                                                                .from(savedTx));
                                                                                                                                                                        }));
                                                                                                                                });
                                                                                                        });
                                                                                });
                                                        });
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<TransactionResponseDeleteAt>> trash(Integer id) {
                logger.info("Trashing transaction id={}", id);
                Attributes attrs = Attributes.builder()
                                .put("transactionId", id)
                                .build();

                return runTraced("trashTransaction", "trash_transaction", attrs,
                                () -> transactionCommandRepository.trashed(id.longValue())
                                                .chain(transaction -> {
                                                        if (transaction == null) {
                                                                return Uni.createFrom().item(new ApiResponse<>("error",
                                                                                "Transaction not found",
                                                                                (TransactionResponseDeleteAt) null));
                                                        }
                                                        return clearCache(transaction.getTransactionId(),
                                                                        transaction.getOrderId())
                                                                        .map(v -> {
                                                                                logger.info("Transaction trashed successfully for id={}",
                                                                                                id);
                                                                                return ApiResponse.success(
                                                                                                "Transaction trashed successfully",
                                                                                                TransactionResponseDeleteAt
                                                                                                                .from(transaction));
                                                                        });
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to trash transaction id={}: {}", id,
                                                                        e.getMessage(), e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to trash transaction: "
                                                                                        + e.getMessage(),
                                                                        (TransactionResponseDeleteAt) null);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<TransactionResponseDeleteAt>> restore(Integer id) {
                logger.info("Restoring transaction id={}", id);
                Attributes attrs = Attributes.builder()
                                .put("transactionId", id)
                                .build();

                return runTraced("restoreTransaction", "restore_transaction", attrs,
                                () -> transactionCommandRepository.restore(id.longValue())
                                                .chain(transaction -> {
                                                        if (transaction == null) {
                                                                return Uni.createFrom().item(new ApiResponse<>("error",
                                                                                "Transaction not found",
                                                                                (TransactionResponseDeleteAt) null));
                                                        }
                                                        return clearCache(transaction.getTransactionId(),
                                                                        transaction.getOrderId())
                                                                        .map(v -> {
                                                                                logger.info("Transaction restored successfully for id={}",
                                                                                                id);
                                                                                return ApiResponse.success(
                                                                                                "Transaction restored successfully",
                                                                                                TransactionResponseDeleteAt
                                                                                                                .from(transaction));
                                                                        });
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to restore transaction id={}: {}", id,
                                                                        e.getMessage(), e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to restore transaction: "
                                                                                        + e.getMessage(),
                                                                        (TransactionResponseDeleteAt) null);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Boolean>> delete(Integer id) {
                logger.info("Permanently deleting transaction id={}", id);
                Attributes attrs = Attributes.builder()
                                .put("transactionId", id)
                                .build();

                return runTraced("deleteTransaction", "delete_transaction", attrs,
                                () -> transactionCommandRepository.deletePermanent(id.longValue())
                                                .chain(transaction -> {
                                                        if (transaction == null) {
                                                                return Uni.createFrom()
                                                                                .item(new ApiResponse<>("error",
                                                                                                "Transaction not found or not trashed",
                                                                                                false));
                                                        }
                                                        return clearCache(transaction.getTransactionId(),
                                                                        transaction.getOrderId())
                                                                        .map(v -> {
                                                                                logger.info("Transaction permanently deleted for id={}",
                                                                                                id);
                                                                                return ApiResponse.success(
                                                                                                "Transaction permanently deleted",
                                                                                                true);
                                                                        });
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to delete transaction id={}: {}", id,
                                                                        e.getMessage(), e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to delete transaction: "
                                                                                        + e.getMessage(),
                                                                        false);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Boolean>> restoreAll() {
                logger.info("Restoring ALL trashed transactions");

                return runTraced("restoreAllTransactions", "restore_all_transactions", Attributes.empty(),
                                () -> transactionCommandRepository.restoreAllDeleted()
                                                .map(success -> {
                                                        if (!success) {
                                                                throw new ResourceNotFoundException(
                                                                                "No transactions found in trash");
                                                        }

                                                        logger.info("All transactions restored successfully");
                                                        return ApiResponse.success(
                                                                        "All transactions restored successfully",
                                                                        success);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Boolean>> deleteAll() {
                logger.info("Permanently deleting ALL trashed transactions");

                return runTraced("deleteAllTransactions", "delete_all_transactions", Attributes.empty(),
                                () -> transactionCommandRepository.deleteAllDeleted()
                                                .map(success -> {
                                                        if (!success) {
                                                                throw new ResourceNotFoundException(
                                                                                "No transactions found in trash");
                                                        }

                                                        logger.info("All trashed transactions permanently deleted");
                                                        return ApiResponse.success(
                                                                        "All trashed transactions permanently deleted",
                                                                        success);
                                                }));
        }

        /**
         * Writes the notification email event into the outbox table so it is
         * published to Kafka by {@code OutboxPublisher} — reliably, outside the
         * request path (transactional outbox pattern). Runs inside the same DB
         * transaction as the {@code transactions} insert.
         */
        private Uni<Void> persistOutboxEvent(Transaction tx) {
                if (notificationEmail == null || notificationEmail.isBlank()) {
                        logger.warn("notification.transaction.email not configured; skipping outbox event for transactionId={}",
                                        tx.getTransactionId());
                        return Uni.createFrom().voidItem();
                }

                JsonObject payload = new JsonObject()
                                .put("email", notificationEmail)
                                .put("subject", "New Transaction Created")
                                .put("body", String.format(
                                                "A new transaction of <b>%d</b> using <b>%s</b> has been created. Status: <b>%s</b>.",
                                                tx.getAmount(), tx.getPaymentMethod(), tx.getStatus()));

                Outbox outbox = new Outbox();
                outbox.setAggregateType("TRANSACTION");
                outbox.setAggregateId(String.valueOf(tx.getTransactionId()));
                outbox.setEventKey(String.valueOf(tx.getTransactionId()));
                outbox.setDomain(Outbox.DOMAIN_TRANSACTION);
                outbox.setTopic("email-service-topic-transaction-create");
                // Phase 2 (event contract): attach the standard envelope
                // (event_id, schema_version, event_type, occurred_at) before
                // persisting, so the outbox replay keeps a stable event_id.
                outbox.setPayload(com.sanedge.common.event.EventEnvelope
                                .withDefaults(payload, "email-service-topic-transaction-create").encode());

                return outboxRepository.persistEvent(outbox).replaceWithVoid();
        }

        private boolean isIdempotencyConflict(Throwable err) {
                for (Throwable t = err; t != null; t = t.getCause()) {
                        String msg = t.getMessage();
                        if (msg != null && (msg.contains("23505")
                                        || msg.contains("duplicate key")
                                        || msg.contains("idx_transactions_idempotency"))) {
                                return true;
                        }
                }
                return false;
        }

        private <T> Uni<T> runTraced(String operationName, String method, Attributes attributes,
                        java.util.function.Supplier<Uni<T>> supplier) {
                return tracingMetrics.traceAndMeasure(operationName, method, attributes, supplier);
        }

        /**
         * Writes the stats event of a created transaction into the transactional
         * outbox instead of publishing it directly. The row is stored in the
         * same database transaction as the transaction itself, so the event is
         * guaranteed to exist once the transaction was created — even when the
         * broker is unavailable. The {@code OutboxPublisher} relays it to
         * Kafka afterwards.
         */
        Uni<Void> persistTransactionStats(Transaction tx, Order order) {
                String eventId = UUID.randomUUID().toString();
                JsonObject payload = new JsonObject()
                                .put("transaction_id", tx.getTransactionId())
                                .put("order_id", tx.getOrderId())
                                .put("merchant_id", tx.getMerchantId())
                                .put("payment_method", tx.getPaymentMethod())
                                .put("status", tx.getStatus() != null ? tx.getStatus().getValue() : "unknown")
                                .put("amount", tx.getAmount() != null ? tx.getAmount() : 0);
                JsonObject event = new JsonObject()
                                .put("event_id", eventId)
                                .put("event_type", "transaction.created")
                                .put("occurred_at", Instant.now().toString())
                                .put("payload", payload);

                if (outboxRepository == null) {
                        return kafkaService.sendExistingEvent(EventTopics.TRANSACTIONS,
                                        String.valueOf(tx.getOrderId()), event.encode())
                                .onFailure().recoverWithItem((Void) null);
                }

                Outbox outbox = new Outbox();
                outbox.setAggregateType("TRANSACTION");
                outbox.setAggregateId(String.valueOf(tx.getTransactionId()));
                outbox.setEventKey(String.valueOf(tx.getOrderId()));
                outbox.setDomain(Outbox.DOMAIN_TRANSACTION);
                outbox.setTopic(EventTopics.TRANSACTIONS);
                outbox.setEventId(eventId);
                outbox.setPayload(event.encode());
                return outboxRepository.persistEvent(outbox).replaceWithVoid();
        }
}