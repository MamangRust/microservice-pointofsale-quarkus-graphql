package com.sanedge.order.service.impl;

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
import com.sanedge.order.domain.requests.FindAllOrderByMerchantRequest;
import com.sanedge.order.domain.requests.FindAllOrderRequest;
import com.sanedge.order.domain.response.OrderResponse;
import com.sanedge.order.domain.response.OrderResponseDeleteAt;
import com.sanedge.order.repository.OrderQueryRepository;
import com.sanedge.order.service.OrderQueryService;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class OrderQueryServiceImpl implements OrderQueryService {
        private static final Logger logger = LoggerFactory.getLogger(OrderQueryServiceImpl.class);

        private final OrderQueryRepository orderQueryRepository;
        private final RedisService redisService;
        private final ObjectMapper objectMapper;
        private final TracingMetrics tracingMetrics;

        private static final long LIST_CACHE_TTL_SECONDS = 300;

        @Inject
        public OrderQueryServiceImpl(OrderQueryRepository orderQueryRepository,
                        RedisService redisService,
                        ObjectMapper objectMapper,
                        TracingMetrics tracingMetrics) {
                this.orderQueryRepository = orderQueryRepository;
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

        private <T> T fromJson(String json, Class<T> clazz) {
                try {
                        return objectMapper.readValue(json, clazz);
                } catch (JsonProcessingException e) {
                        logger.error("Error deserializing JSON to object", e);
                        throw new RuntimeException("Failed to deserialize JSON", e);
                }
        }

        private <T> T fromJson(String json, TypeReference<T> typeReference) {
                try {
                        return objectMapper.readValue(json, typeReference);
                } catch (JsonProcessingException e) {
                        logger.error("Error deserializing JSON to object with TypeReference", e);
                        throw new RuntimeException("Failed to deserialize JSON", e);
                }
        }

        @Override
        @WithTransaction
        public Uni<ApiResponsePagination<List<OrderResponse>>> findAll(FindAllOrderRequest req) {
                String cacheKey = String.format("orders:all:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<OrderResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<OrderResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return runTraced("findAllOrders", "find_all_orders", Attributes.empty(),
                                                        () -> {
                                                                return orderQueryRepository.findOrders(req)
                                                                                .chain(pagedResult -> {
                                                                                        ApiResponsePagination<List<OrderResponse>> response = buildPaginatedResponse(
                                                                                                        pagedResult,
                                                                                                        req.getPage(),
                                                                                                        req.getPageSize(),
                                                                                                        "Orders retrieved successfully",
                                                                                                        OrderResponse::from);

                                                                                        return redisService
                                                                                                        .setWithExpirationReactive(
                                                                                                                        cacheKey,
                                                                                                                        toJson(response),
                                                                                                                        LIST_CACHE_TTL_SECONDS)
                                                                                                        .map(v -> {
                                                                                                                logger.info("Cached response for key: {}",
                                                                                                                                cacheKey);
                                                                                                                logger.info("Successfully retrieved {} orders",
                                                                                                                                pagedResult.getTotalRecords());
                                                                                                                return response;
                                                                                                        });
                                                                                })
                                                                                .onFailure().recoverWithItem(e -> {
                                                                                        logger.error("Failed to fetch orders: {}",
                                                                                                        e.getMessage(),
                                                                                                        e);
                                                                                        return new ApiResponsePagination<>(
                                                                                                        "error",
                                                                                                        "Failed to fetch orders: "
                                                                                                                        + e.getMessage(),
                                                                                                        Collections.emptyList(),
                                                                                                        null);
                                                                                });
                                                        });
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponsePagination<List<OrderResponseDeleteAt>>> findByActive(FindAllOrderRequest req) {
                String cacheKey = String.format("orders:active:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<OrderResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<OrderResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return runTraced("findActiveOrders", "find_active_orders", Attributes.empty(),
									() -> {
										return orderQueryRepository.findActiveOrders(req)
                                                                                .chain(pagedResult -> {
                                                                                        ApiResponsePagination<List<OrderResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                        pagedResult,
                                                                                                        req.getPage(),
                                                                                                        req.getPageSize(),
                                                                                                        "Active orders retrieved successfully",
                                                                                                        OrderResponseDeleteAt::from);

                                                                                        return redisService
                                                                                                        .setWithExpirationReactive(
                                                                                                                        cacheKey,
                                                                                                                        toJson(response),
                                                                                                                        LIST_CACHE_TTL_SECONDS)
                                                                                                        .map(v -> {
                                                                                                                logger.info("Cached response for key: {}",
                                                                                                                                cacheKey);
                                                                                                                logger.info("Successfully retrieved {} active orders",
                                                                                                                                pagedResult.getTotalRecords());
                                                                                                                return response;
                                                                                                        });
                                                                                })
                                                                                .onFailure().recoverWithItem(e -> {
                                                                                        logger.error("Failed to fetch active orders: {}",
                                                                                                        e.getMessage(),
                                                                                                        e);
                                                                                        return new ApiResponsePagination<>(
                                                                                                        "error",
                                                                                                        "Failed to fetch active orders: "
                                                                                                                        + e.getMessage(),
                                                                                                        Collections.emptyList(),
                                                                                                        null);
                                                                                });
                                                        });
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponsePagination<List<OrderResponseDeleteAt>>> findByTrashed(FindAllOrderRequest req) {
                String cacheKey = String.format("orders:trashed:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<OrderResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<OrderResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return runTraced("findTrashedOrders", "find_trashed_orders", Attributes.empty(),
									() -> {
										return orderQueryRepository.findTrashedOrders(req)
                                                                                .chain(pagedResult -> {
                                                                                        ApiResponsePagination<List<OrderResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                        pagedResult,
                                                                                                        req.getPage(),
                                                                                                        req.getPageSize(),
                                                                                                        "Trashed orders retrieved successfully",
                                                                                                        OrderResponseDeleteAt::from);

                                                                                        return redisService
                                                                                                        .setWithExpirationReactive(
                                                                                                                        cacheKey,
                                                                                                                        toJson(response),
                                                                                                                        LIST_CACHE_TTL_SECONDS)
                                                                                                        .map(v -> {
                                                                                                                logger.info("Cached response for key: {}",
                                                                                                                                cacheKey);
                                                                                                                logger.info("Successfully retrieved {} trashed orders",
                                                                                                                                pagedResult.getTotalRecords());
                                                                                                                return response;
                                                                                                        });
                                                                                })
                                                                                .onFailure().recoverWithItem(e -> {
                                                                                        logger.error("Failed to fetch trashed orders: {}",
                                                                                                        e.getMessage(),
                                                                                                        e);
                                                                                        return new ApiResponsePagination<>(
                                                                                                        "error",
                                                                                                        "Failed to fetch trashed orders: "
                                                                                                                        + e.getMessage(),
                                                                                                        Collections.emptyList(),
                                                                                                        null);
                                                                                });
                                                        });
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponsePagination<List<OrderResponse>>> findByMerchantId(FindAllOrderByMerchantRequest req) {
                String cacheKey = String.format("orders:merchant:%d:%d:%d:%s",
                                req.getMerchantId(),
                                req.getPage() != null ? req.getPage() : 1,
                                req.getPageSize() != null ? req.getPageSize() : 10,
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<OrderResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<OrderResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        Attributes attrs = Attributes.builder()
                                                        .put("merchant.id",
                                                                        req.getMerchantId() != null
                                                                                        ? req.getMerchantId().toString()
                                                                                        : "null")
                                                        .build();

                                        return runTraced("findOrdersByMerchant", "find_orders_by_merchant", attrs,
                                                        () -> orderQueryRepository.findOrdersByMerchant(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<OrderResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Orders retrieved successfully",
                                                                                                OrderResponse::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} merchant orders",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        })
                                                                        .onFailure().recoverWithItem(e -> {
                                                                                logger.error("Failed to fetch orders for merchantId={}: {}",
                                                                                                req.getMerchantId(),
                                                                                                e.getMessage(), e);
                                                                                return new ApiResponsePagination<>(
                                                                                                "error",
                                                                                                "Failed to fetch orders for merchant: "
                                                                                                                + e.getMessage(),
                                                                                                Collections.emptyList(),
                                                                                                null);
                                                                        }));
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<OrderResponse>> findById(Integer id) {
                String cacheKey = "order:" + id;

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                OrderResponse response = fromJson(cachedJson, OrderResponse.class);
                                                return Uni.createFrom().item(ApiResponse
                                                                .success("Order retrieved successfully", response));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        Attributes attrs = Attributes.builder()
                                                        .put("order.id", id.toString())
                                                        .build();

                                        return runTraced("findOrderById", "find_order_by_id", attrs,
                                                        () -> orderQueryRepository.findOrderById(id.longValue())
                                                                        .chain(order -> {
                                                                                if (order == null) {
                                                                                        logger.warn("Order not found with id={}",
                                                                                                        id);
                                                                                        throw new NotFoundException(
                                                                                                        "Order not found with id: "
                                                                                                                        + id);
                                                                                }

                                                                                OrderResponse response = OrderResponse
                                                                                                .from(order);

                                                                                return redisService.setReactive(
                                                                                                cacheKey,
                                                                                                toJson(response))
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached order for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully found order with id: {}",
                                                                                                                        id);
                                                                                                        return ApiResponse
                                                                                                                        .success("Order retrieved successfully",
                                                                                                                                        response);
                                                                                                });
                                                                        })
                                                                        .onFailure().recoverWithItem(e -> {
                                                                                logger.error("Failed to fetch order by id={}: {}",
                                                                                                id, e.getMessage(), e);
                                                                                return new ApiResponse<>("error",
                                                                                                "Failed to fetch order: "
                                                                                                                + e.getMessage(),
                                                                                                null);
                                                                        }));
                                });
        }

        private <T, R> ApiResponsePagination<List<R>> buildPaginatedResponse(
                        PagedResult<T> pagedResult,
                        Integer page,
                        Integer pageSize,
                        String successMessage,
                        Function<T, R> mapper) {

                List<R> data = pagedResult.getData().stream()
                                .map(mapper)
                                .collect(Collectors.toList());

                int totalRecords = pagedResult.getTotalRecords();
                int size = pageSize != null && pageSize > 0 ? pageSize : 1;
                int totalPages = (int) Math.ceil((double) totalRecords / size);

                PaginationMeta pagination = new PaginationMeta(page != null ? page : 1, size, totalPages, totalRecords);

                return new ApiResponsePagination<>("success", successMessage, data, pagination);
        }

        private <T> Uni<T> runTraced(String operationName, String method, Attributes attributes,
                        Supplier<Uni<T>> supplier) {
                return tracingMetrics.traceAndMeasure(operationName, method, attributes, supplier);
        }
}
