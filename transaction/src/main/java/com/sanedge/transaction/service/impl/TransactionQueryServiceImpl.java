package com.sanedge.transaction.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.transaction.domain.requests.FindAllTransactionByMerchantRequest;
import com.sanedge.transaction.domain.requests.FindAllTransactionRequest;
import com.sanedge.transaction.domain.response.TransactionResponse;
import com.sanedge.transaction.domain.response.TransactionResponseDeleteAt;
import com.sanedge.transaction.repository.TransactionQueryRepository;
import com.sanedge.transaction.service.TransactionQueryService;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TransactionQueryServiceImpl implements TransactionQueryService {
        private static final Logger logger = LoggerFactory.getLogger(TransactionQueryServiceImpl.class);

        private final TransactionQueryRepository transactionQueryRepository;
        private final RedisService redisService;
        private final ObjectMapper objectMapper;
        private final TracingMetrics tracingMetrics;

        private static final long LIST_CACHE_TTL_SECONDS = 300;

        @Inject
        public TransactionQueryServiceImpl(TransactionQueryRepository transactionQueryRepository,
                        RedisService redisService,
                        ObjectMapper objectMapper,
                        TracingMetrics tracingMetrics) {
                this.transactionQueryRepository = transactionQueryRepository;
                this.redisService = redisService;
                this.objectMapper = objectMapper;
                this.tracingMetrics = tracingMetrics;
        }

        private String toJson(Object obj) {
                try {
                        return objectMapper.writeValueAsString(obj);
                } catch (JsonProcessingException e) {
                        logger.error("Error serializing object to JSON", e);
                        throw new RuntimeException("Failed to serialize object", e);
                }
        }

        private <T> T fromJson(String json, TypeReference<T> typeReference) {
                try {
                        return objectMapper.readValue(json, typeReference);
                } catch (JsonProcessingException e) {
                        logger.error("Error deserializing JSON with TypeReference", e);
                        throw new RuntimeException("Failed to deserialize JSON", e);
                }
        }

        @Override
        @WithTransaction
        public Uni<ApiResponsePagination<List<TransactionResponse>>> findAllTransactions(
                        FindAllTransactionRequest req) {
                int page = req.getPage() > 0 ? req.getPage() : 1;
                int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
                String keyword = req.getSearch() != null ? req.getSearch() : "";

                String cacheKey = String.format("transactions:all:page:%d:size:%d:search:%s", page, pageSize, keyword);

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<TransactionResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<TransactionResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return runTraced("findAllTransactions", "find_all_transactions",
                                                        Attributes.empty(),
                                                        () -> transactionQueryRepository.findTransactions(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<TransactionResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult, page,
                                                                                                pageSize,
                                                                                                "Transactions retrieved successfully",
                                                                                                TransactionResponse::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Successfully retrieved {} transactions",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        })
                                                                        .onFailure().recoverWithItem(e -> {
                                                                                logger.error("Failed to fetch transactions: {}",
                                                                                                e.getMessage(), e);
                                                                                return new ApiResponsePagination<>(
                                                                                                "error",
                                                                                                "Failed to fetch transactions: "
                                                                                                                + e.getMessage(),
                                                                                                Collections.emptyList(),
                                                                                                null);
                                                                        }));
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponsePagination<List<TransactionResponseDeleteAt>>> findByActive(
                        FindAllTransactionRequest req) {
                int page = req.getPage() > 0 ? req.getPage() : 1;
                int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
                String keyword = req.getSearch() != null ? req.getSearch() : "";

                String cacheKey = String.format("transactions:active:page:%d:size:%d:search:%s", page, pageSize,
                                keyword);

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<TransactionResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<TransactionResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return runTraced("findActiveTransactions", "find_active_transactions",
                                                        Attributes.empty(),
                                                        () -> transactionQueryRepository.findActiveTransactions(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<TransactionResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult, page,
                                                                                                pageSize,
                                                                                                "Active transactions retrieved successfully",
                                                                                                TransactionResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Successfully retrieved {} active transactions",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        })
                                                                        .onFailure().recoverWithItem(e -> {
                                                                                logger.error("Failed to fetch active transactions: {}",
                                                                                                e.getMessage(), e);
                                                                                return new ApiResponsePagination<>(
                                                                                                "error",
                                                                                                "Failed to fetch active transactions: "
                                                                                                                + e.getMessage(),
                                                                                                Collections.emptyList(),
                                                                                                null);
                                                                        }));
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponsePagination<List<TransactionResponseDeleteAt>>> findByTrashed(
                        FindAllTransactionRequest req) {
                int page = req.getPage() > 0 ? req.getPage() : 1;
                int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
                String keyword = req.getSearch() != null ? req.getSearch() : "";

                String cacheKey = String.format("transactions:trashed:page:%d:size:%d:search:%s", page, pageSize,
                                keyword);

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<TransactionResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<TransactionResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return runTraced("findTrashedTransactions", "find_trashed_transactions",
                                                        Attributes.empty(),
                                                        () -> transactionQueryRepository.findTrashedTransactions(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<TransactionResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult, page,
                                                                                                pageSize,
                                                                                                "Trashed transactions retrieved successfully",
                                                                                                TransactionResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Successfully retrieved {} trashed transactions",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        })
                                                                        .onFailure().recoverWithItem(e -> {
                                                                                logger.error("Failed to fetch trashed transactions: {}",
                                                                                                e.getMessage(), e);
                                                                                return new ApiResponsePagination<>(
                                                                                                "error",
                                                                                                "Failed to fetch trashed transactions: "
                                                                                                                + e.getMessage(),
                                                                                                Collections.emptyList(),
                                                                                                null);
                                                                        }));
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponsePagination<List<TransactionResponse>>> findByMerchant(
                        FindAllTransactionByMerchantRequest req) {
                int page = req.getPage() != null && req.getPage() > 0 ? req.getPage() : 1;
                int pageSize = req.getPageSize() != null && req.getPageSize() > 0 ? req.getPageSize() : 10;
                String keyword = req.getSearch() != null ? req.getSearch() : "";
                Long merchantId = req.getMerchantId() != null ? req.getMerchantId().longValue() : 0L;

                String cacheKey = String.format("transactions:merchant:%d:page:%d:size:%d:search:%s", merchantId, page,
                                pageSize, keyword);

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<TransactionResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<TransactionResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        Attributes attrs = Attributes.builder()
                                                        .put("merchantId", merchantId)
                                                        .build();

                                        return runTraced("findTransactionsByMerchant", "find_transactions_by_merchant",
                                                        attrs,
                                                        () -> transactionQueryRepository.findTransactionsByMerchant(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<TransactionResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult, page,
                                                                                                pageSize,
                                                                                                "Transactions by merchant retrieved successfully",
                                                                                                TransactionResponse::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Successfully retrieved {} transactions for merchant_id={}",
                                                                                                                        pagedResult.getTotalRecords(),
                                                                                                                        merchantId);
                                                                                                        return response;
                                                                                                });
                                                                        })
                                                                        .onFailure().recoverWithItem(e -> {
                                                                                logger.error("Failed to fetch transactions by merchant_id={}: {}",
                                                                                                merchantId,
                                                                                                e.getMessage(), e);
                                                                                return new ApiResponsePagination<>(
                                                                                                "error",
                                                                                                "Failed to fetch transactions by merchant: "
                                                                                                                + e.getMessage(),
                                                                                                Collections.emptyList(),
                                                                                                null);
                                                                        }));
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<TransactionResponse>> findById(Integer id) {
                String cacheKey = "transaction:id:" + id;

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                TransactionResponse cached = fromJson(cachedJson,
                                                                new TypeReference<TransactionResponse>() {
                                                                });
                                                return Uni.createFrom().item(ApiResponse
                                                                .success("Transaction retrieved successfully", cached));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        Attributes attrs = Attributes.builder()
                                                        .put("transactionId", id)
                                                        .build();

                                        return runTraced("findTransactionById", "find_transaction_by_id", attrs,
                                                        () -> transactionQueryRepository
                                                                        .findByTransactionId(id.longValue())
                                                                        .chain(transaction -> {
                                                                                if (transaction == null) {
                                                                                        logger.warn("Transaction not found with id={}",
                                                                                                        id);
                                                                                        return Uni.createFrom().item(
                                                                                                        new ApiResponse<>(
                                                                                                                        "error",
                                                                                                                        "Transaction not found",
                                                                                                                        (TransactionResponse) null));
                                                                                }

                                                                                TransactionResponse transactionResponse = TransactionResponse
                                                                                                .from(transaction);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(transactionResponse),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Successfully found transaction with id: {}",
                                                                                                                        id);
                                                                                                        return ApiResponse
                                                                                                                        .success("Transaction retrieved successfully",
                                                                                                                                        transactionResponse);
                                                                                                });
                                                                        })
                                                                        .onFailure().recoverWithItem(e -> {
                                                                                logger.error("Failed to fetch transaction by id={}: {}",
                                                                                                id, e.getMessage(), e);
                                                                                return new ApiResponse<>("error",
                                                                                                "Failed to fetch transaction: "
                                                                                                                + e.getMessage(),
                                                                                                (TransactionResponse) null);
                                                                        }));
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<TransactionResponse>> findByOrderId(Integer id) {
                String cacheKey = "transaction:order:" + id;

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                TransactionResponse cached = fromJson(cachedJson,
                                                                new TypeReference<TransactionResponse>() {
                                                                });
                                                return Uni.createFrom().item(ApiResponse
                                                                .success("Transaction retrieved successfully", cached));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        Attributes attrs = Attributes.builder()
                                                        .put("orderId", id)
                                                        .build();

                                        return runTraced("findTransactionByOrderId", "find_transaction_by_order_id",
                                                        attrs,
                                                        () -> transactionQueryRepository.findByOrderId(id.longValue())
                                                                        .chain(transaction -> {
                                                                                if (transaction == null) {
                                                                                        logger.warn("Transaction not found with order_id={}",
                                                                                                        id);
                                                                                        return Uni.createFrom().item(
                                                                                                        new ApiResponse<>(
                                                                                                                        "error",
                                                                                                                        "Transaction not found for order",
                                                                                                                        (TransactionResponse) null));
                                                                                }

                                                                                TransactionResponse transactionResponse = TransactionResponse
                                                                                                .from(transaction);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(transactionResponse),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Successfully found transaction with order_id: {}",
                                                                                                                        id);
                                                                                                        return ApiResponse
                                                                                                                        .success("Transaction retrieved successfully",
                                                                                                                                        transactionResponse);
                                                                                                });
                                                                        })
                                                                        .onFailure().recoverWithItem(e -> {
                                                                                logger.error("Failed to fetch transaction by order_id={}: {}",
                                                                                                id, e.getMessage(), e);
                                                                                return new ApiResponse<>("error",
                                                                                                "Failed to fetch transaction by order: "
                                                                                                                + e.getMessage(),
                                                                                                (TransactionResponse) null);
                                                                        }));
                                });
        }

        private <T, R> ApiResponsePagination<List<R>> buildPaginatedResponse(PagedResult<T> pagedResult,
                        int page,
                        int pageSize,
                        String successMessage,
                        Function<T, R> mapper) {
                List<R> data = pagedResult.getData().stream()
                                .map(mapper)
                                .collect(Collectors.toList());

                int totalRecords = pagedResult.getTotalRecords();
                int size = pageSize > 0 ? pageSize : 1;
                int totalPages = (int) Math.ceil((double) totalRecords / size);

                PaginationMeta pagination = new PaginationMeta(page, size, totalPages, totalRecords);

                return new ApiResponsePagination<>("success", successMessage, data, pagination);
        }

        private <T> Uni<T> runTraced(String operationName, String method, Attributes attributes,
                        Supplier<Uni<T>> supplier) {
                return tracingMetrics.traceAndMeasure(operationName, method, attributes, supplier);
        }
}