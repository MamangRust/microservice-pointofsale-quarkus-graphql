package com.sanedge.cashier.service.impl;

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
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.cashier.domain.requests.FindAllCashierMerchant;
import com.sanedge.cashier.domain.requests.FindAllCashiers;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.cashier.domain.response.CashierResponse;
import com.sanedge.cashier.domain.response.CashierResponseDeleteAt;
import com.sanedge.cashier.repository.CashierQueryRepository;
import com.sanedge.cashier.service.CashierQueryService;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class CashierQueryServiceImpl implements CashierQueryService {
        private static final Logger logger = LoggerFactory.getLogger(CashierQueryServiceImpl.class);

        CashierQueryRepository cashierQueryRepository;
        RedisService redisService;
        ObjectMapper objectMapper;
        TracingMetrics tracingMetrics;

        private static final long LIST_CACHE_TTL_SECONDS = 300;

        @Inject
        public CashierQueryServiceImpl(CashierQueryRepository cashierQueryRepository,
                        RedisService redisService,
                        ObjectMapper objectMapper,
                        TracingMetrics tracingMetrics) {
                this.cashierQueryRepository = cashierQueryRepository;
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
        public Uni<ApiResponsePagination<List<CashierResponse>>> findAll(FindAllCashiers req) {
                String cacheKey = String.format("cashiers:all:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch());

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<CashierResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<CashierResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return runTraced("findAllCashiers", "find_all_cashiers", Attributes.empty(),
                                                        () -> cashierQueryRepository.findAllCashiers(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<CashierResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Cashiers retrieved successfully",
                                                                                                CashierResponse::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} cashiers",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<CashierResponse>> findById(Long cashierId) {
                String cacheKey = "cashier:" + cashierId;

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                CashierResponse cachedCashier = fromJson(cachedJson,
                                                                CashierResponse.class);
                                                return Uni.createFrom().item(ApiResponse.success(
                                                                "Cashier retrieved successfully", cachedCashier));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        Attributes attrs = Attributes.builder()
                                                        .put("cashier.id", cashierId.toString())
                                                        .build();

                                        return runTraced("findCashierById", "find_cashier_by_id", attrs,
                                                        () -> cashierQueryRepository.findByCashierId(cashierId)
                                                                        .chain(cashier -> {
                                                                                if (cashier == null) {
                                                                                        logger.warn("Cashier not found with id: {}",
                                                                                                        cashierId);
                                                                                        throw new NotFoundException(
                                                                                                        "Cashier not found with id: "
                                                                                                                        + cashierId);
                                                                                }

                                                                                CashierResponse cashierResponse = CashierResponse
                                                                                                .from(cashier);

                                                                                return redisService.setReactive(
                                                                                                cacheKey,
                                                                                                toJson(cashierResponse))
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached cashier for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully found cashier with id: {} and name: {}",
                                                                                                                        cashierId,
                                                                                                                        cashier.getName());
                                                                                                        return ApiResponse
                                                                                                                        .success("Cashier retrieved successfully",
                                                                                                                                        cashierResponse);
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponsePagination<List<CashierResponseDeleteAt>>> findByActive(FindAllCashiers req) {
                String cacheKey = String.format("cashiers:active:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch());

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<CashierResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<CashierResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return runTraced("findActiveCashiers", "find_active_cashiers",
                                                        Attributes.empty(),
                                                        () -> cashierQueryRepository.findActiveCashiers(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<CashierResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Active cashiers retrieved successfully",
                                                                                                CashierResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} active cashiers",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponsePagination<List<CashierResponseDeleteAt>>> findByTrashed(FindAllCashiers req) {
                String cacheKey = String.format("cashiers:trashed:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch());

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<CashierResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<CashierResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return runTraced("findTrashedCashiers", "find_trashed_cashiers",
                                                        Attributes.empty(),
                                                        () -> cashierQueryRepository.findTrashedCashiers(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<CashierResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Trashed cashiers retrieved successfully",
                                                                                                CashierResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} trashed cashiers",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        }));
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponsePagination<List<CashierResponse>>> findByMerchant(FindAllCashierMerchant req) {
                String cacheKey = String.format("cashiers:merchant:%d:%d:%d:%s", req.getMerchantId(), req.getPage(),
                                req.getPageSize(), req.getSearch());

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<CashierResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<CashierResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return runTraced("findByMerchantCashiers", "find_by_merchant_cashiers",
                                                        Attributes.empty(),
                                                        () -> cashierQueryRepository.findByMerchants(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<CashierResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                req.getPage(),
                                                                                                req.getPageSize(),
                                                                                                "Cashiers retrieved successfully by merchant",
                                                                                                CashierResponse::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} merchant cashiers",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
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