package com.sanedge.order_item.service.impl;

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
import com.sanedge.order_item.domain.requests.FindAllOrderItems;
import com.sanedge.order_item.domain.response.OrderItemResponse;
import com.sanedge.order_item.domain.response.OrderItemResponseDeleteAt;
import com.sanedge.order_item.repository.OrderItemRepository;
import com.sanedge.order_item.service.OrderItemQueryService;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrderItemQueryServiceImpl implements OrderItemQueryService {
    private static final Logger logger = LoggerFactory.getLogger(OrderItemQueryServiceImpl.class);

    private final OrderItemRepository orderItemRepository;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final TracingMetrics tracingMetrics;

    private static final long CACHE_TTL_SECONDS = 300;

    @Inject
    public OrderItemQueryServiceImpl(OrderItemRepository orderItemRepository,
            RedisService redisService,
            ObjectMapper objectMapper,
            TracingMetrics tracingMetrics) {
        this.orderItemRepository = orderItemRepository;
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
            logger.error("Error deserializing JSON to object", e);
            throw new RuntimeException("Failed to deserialize JSON", e);
        }
    }

    @Override
    @WithTransaction
    public Uni<ApiResponsePagination<List<OrderItemResponse>>> findAll(FindAllOrderItems request) {
        String search = request.getSearch();
        int page = request.getPage();
        int pageSize = request.getPageSize();
        logger.info("Querying all order items: search={}, page={}, pageSize={}", search, page, pageSize);
        String cacheKey = String.format("order_item:all:%s:%d:%d", search != null ? search : "", page, pageSize);
        Attributes attrs = Attributes.builder()
                .put("search", search != null ? search : "null")
                .put("page", page)
                .put("pageSize", pageSize)
                .build();

        return runTraced("findAllOrderItems", "find_all_order_items", attrs,
                () -> redisService.getReactive(cacheKey)
                        .chain(cachedJson -> {
                            if (cachedJson != null) {
                                logger.info("Cache HIT for key: {}", cacheKey);
                                ApiResponsePagination<List<OrderItemResponse>> cached = fromJson(
                                        cachedJson,
                                        new TypeReference<ApiResponsePagination<List<OrderItemResponse>>>() {
                                        });
                                return Uni.createFrom().item(cached);
                            }

                            logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                            return orderItemRepository.findOrderItems(request)
                                    .map(pagedResult -> buildPaginatedResponse(pagedResult, request,
                                            "Order items retrieved successfully", OrderItemResponse::from))
                                    .chain(res -> redisService
                                            .setWithExpirationReactive(cacheKey, toJson(res), CACHE_TTL_SECONDS)
                                            .map(v -> res));
                        })
                        .onFailure().recoverWithItem(e -> {
                            logger.error("Failed to query all order items: {}", e.getMessage(), e);
                            return new ApiResponsePagination<>("error", "Failed to fetch order items: " + e.getMessage(),
                                    null, null);
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponsePagination<List<OrderItemResponseDeleteAt>>> findByActive(FindAllOrderItems request) {
        String search = request.getSearch();
        int page = request.getPage();
        int pageSize = request.getPageSize();
        logger.info("Querying active order items: search={}, page={}, pageSize={}", search, page, pageSize);
        String cacheKey = String.format("order_item:active:%s:%d:%d", search != null ? search : "", page, pageSize);
        Attributes attrs = Attributes.builder()
                .put("search", search != null ? search : "null")
                .put("page", page)
                .put("pageSize", pageSize)
                .build();

        return runTraced("findActiveOrderItems", "find_active_order_items", attrs,
                () -> redisService.getReactive(cacheKey)
                        .chain(cachedJson -> {
                            if (cachedJson != null) {
                                logger.info("Cache HIT for key: {}", cacheKey);
                                ApiResponsePagination<List<OrderItemResponseDeleteAt>> cached = fromJson(
                                        cachedJson,
                                        new TypeReference<ApiResponsePagination<List<OrderItemResponseDeleteAt>>>() {
                                        });
                                return Uni.createFrom().item(cached);
                            }

                            logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                            return orderItemRepository.findActiveOrderItems(request)
                                    .map(pagedResult -> buildPaginatedResponse(pagedResult, request,
                                            "Active order items retrieved successfully",
                                            OrderItemResponseDeleteAt::from))
                                    .chain(res -> redisService
                                            .setWithExpirationReactive(cacheKey, toJson(res), CACHE_TTL_SECONDS)
                                            .map(v -> res));
                        })
                        .onFailure().recoverWithItem(e -> {
                            logger.error("Failed to query active order items: {}", e.getMessage(), e);
                            return new ApiResponsePagination<>("error",
                                    "Failed to fetch active order items: " + e.getMessage(), null, null);
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponsePagination<List<OrderItemResponseDeleteAt>>> findByTrashed(FindAllOrderItems request) {
        String search = request.getSearch();
        int page = request.getPage();
        int pageSize = request.getPageSize();
        logger.info("Querying trashed order items: search={}, page={}, pageSize={}", search, page, pageSize);
        String cacheKey = String.format("order_item:trashed:%s:%d:%d", search != null ? search : "", page, pageSize);
        Attributes attrs = Attributes.builder()
                .put("search", search != null ? search : "null")
                .put("page", page)
                .put("pageSize", pageSize)
                .build();

        return runTraced("findTrashedOrderItems", "find_trashed_order_items", attrs,
                () -> redisService.getReactive(cacheKey)
                        .chain(cachedJson -> {
                            if (cachedJson != null) {
                                logger.info("Cache HIT for key: {}", cacheKey);
                                ApiResponsePagination<List<OrderItemResponseDeleteAt>> cached = fromJson(
                                        cachedJson,
                                        new TypeReference<ApiResponsePagination<List<OrderItemResponseDeleteAt>>>() {
                                        });
                                return Uni.createFrom().item(cached);
                            }

                            logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                            return orderItemRepository.findTrashedOrderItems(request)
                                    .map(pagedResult -> buildPaginatedResponse(pagedResult, request,
                                            "Trashed order items retrieved successfully",
                                            OrderItemResponseDeleteAt::from))
                                    .chain(res -> redisService
                                            .setWithExpirationReactive(cacheKey, toJson(res), CACHE_TTL_SECONDS)
                                            .map(v -> res));
                        })
                        .onFailure().recoverWithItem(e -> {
                            logger.error("Failed to query trashed order items: {}", e.getMessage(), e);
                            return new ApiResponsePagination<>("error",
                                    "Failed to fetch trashed order items: " + e.getMessage(), null, null);
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<List<OrderItemResponse>>> findOrderItemByOrder(Integer orderId) {
        logger.info("Querying order items by order ID={}", orderId);
        String cacheKey = String.format("order_item:by_order:%d", orderId);
        Attributes attrs = Attributes.builder()
                .put("order.id", orderId.toString())
                .build();

        return runTraced("findOrderItemByOrder", "find_order_item_by_order", attrs,
                () -> redisService.getReactive(cacheKey)
                        .chain(cachedJson -> {
                            if (cachedJson != null) {
                                logger.info("Cache HIT for key: {}", cacheKey);
                                ApiResponse<List<OrderItemResponse>> cached = fromJson(
                                        cachedJson,
                                        new TypeReference<ApiResponse<List<OrderItemResponse>>>() {
                                        });
                                return Uni.createFrom().item(cached);
                            }

                            logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                            return orderItemRepository.findOrderItemByOrder(orderId.longValue())
                                    .map(list -> {
                                        List<OrderItemResponse> responses = list.stream()
                                                .map(OrderItemResponse::from)
                                                .collect(Collectors.toList());
                                        return ApiResponse.success("Order items by order retrieved successfully",
                                                responses);
                                    })
                                    .chain(res -> redisService
                                            .setWithExpirationReactive(cacheKey, toJson(res), CACHE_TTL_SECONDS)
                                            .map(v -> res));
                        })
                        .onFailure().recoverWithItem(e -> {
                            logger.error("Failed to query order items by order ID={}: {}", orderId, e.getMessage(), e);
                            return new ApiResponse<>("error", "Failed to fetch order items: " + e.getMessage(), null);
                        }));
    }

    private <T, R> ApiResponsePagination<List<R>> buildPaginatedResponse(
            PagedResult<T> pagedResult,
            FindAllOrderItems request,
            String successMessage,
            Function<T, R> mapper) {
        List<R> data = pagedResult.getData().stream()
                .map(mapper)
                .collect(Collectors.toList());

        int totalRecords = pagedResult.getTotalRecords();
        int size = request.getPageSize() > 0 ? request.getPageSize() : 1;
        int totalPages = (int) Math.ceil((double) totalRecords / size);

        PaginationMeta pagination = new PaginationMeta(request.getPage(), size, totalPages, totalRecords);

        return new ApiResponsePagination<>("success", successMessage, data, pagination);
    }

    private <T> Uni<T> runTraced(String operationName, String method, Attributes attributes,
            Supplier<Uni<T>> supplier) {
        return tracingMetrics.traceAndMeasure(operationName, method, attributes, supplier);
    }
}
