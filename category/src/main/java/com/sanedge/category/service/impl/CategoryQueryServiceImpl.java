package com.sanedge.category.service.impl;

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
import com.sanedge.category.domain.requests.FindAllCategory;
import com.sanedge.category.domain.response.CategoryResponse;
import com.sanedge.category.domain.response.CategoryResponseDeleteAt;
import com.sanedge.category.repository.CategoryQueryRepository;
import com.sanedge.category.service.CategoryQueryService;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CategoryQueryServiceImpl implements CategoryQueryService {
        private static final Logger logger = LoggerFactory.getLogger(CategoryQueryServiceImpl.class);

        private final CategoryQueryRepository categoryQueryRepository;
        private final RedisService redisService;
        private final ObjectMapper objectMapper;
        private final TracingMetrics tracingMetrics;

        private static final long LIST_CACHE_TTL_SECONDS = 300;

        @Inject
        public CategoryQueryServiceImpl(CategoryQueryRepository categoryQueryRepository,
                        RedisService redisService,
                        ObjectMapper objectMapper,
                        TracingMetrics tracingMetrics) {
                this.categoryQueryRepository = categoryQueryRepository;
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
        public Uni<ApiResponsePagination<List<CategoryResponse>>> findAll(FindAllCategory req) {
                String cacheKey = String.format("categories:all:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<CategoryResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<CategoryResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return runTraced("findAllCategories", "find_all_categories", Attributes.empty(),
                                                        () -> categoryQueryRepository.findCategories(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<CategoryResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult, req,
                                                                                                "Categories retrieved successfully",
                                                                                                CategoryResponse::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} categories",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        })
                                                                        .onFailure().recoverWithItem(e -> {
                                                                                logger.error("Failed to fetch categories: {}",
                                                                                                e.getMessage(), e);
                                                                                return new ApiResponsePagination<>(
                                                                                                "error",
                                                                                                "Failed to fetch categories: "
                                                                                                                + e.getMessage(),
                                                                                                Collections.emptyList(),
                                                                                                null);
                                                                        }));
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponsePagination<List<CategoryResponseDeleteAt>>> findByActive(FindAllCategory req) {
                String cacheKey = String.format("categories:active:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<CategoryResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<CategoryResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return runTraced("findActiveCategories", "find_active_categories",
                                                        Attributes.empty(),
                                                        () -> categoryQueryRepository.findActiveCategories(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<CategoryResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult, req,
                                                                                                "Active categories retrieved successfully",
                                                                                                CategoryResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} active categories",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        })
                                                                        .onFailure().recoverWithItem(e -> {
                                                                                logger.error("Failed to fetch active categories: {}",
                                                                                                e.getMessage(), e);
                                                                                return new ApiResponsePagination<>(
                                                                                                "error",
                                                                                                "Failed to fetch active categories: "
                                                                                                                + e.getMessage(),
                                                                                                Collections.emptyList(),
                                                                                                null);
                                                                        }));
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponsePagination<List<CategoryResponseDeleteAt>>> findByTrashed(FindAllCategory req) {
                String cacheKey = String.format("categories:trashed:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch() != null ? req.getSearch() : "");

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<CategoryResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<CategoryResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return runTraced("findTrashedCategories", "find_trashed_categories",
                                                        Attributes.empty(),
                                                        () -> categoryQueryRepository.findTrashedCategories(req)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<CategoryResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult, req,
                                                                                                "Trashed categories retrieved successfully",
                                                                                                CategoryResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} trashed categories",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        })
                                                                        .onFailure().recoverWithItem(e -> {
                                                                                logger.error("Failed to fetch trashed categories: {}",
                                                                                                e.getMessage(), e);
                                                                                return new ApiResponsePagination<>(
                                                                                                "error",
                                                                                                "Failed to fetch trashed categories: "
                                                                                                                + e.getMessage(),
                                                                                                Collections.emptyList(),
                                                                                                null);
                                                                        }));
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<CategoryResponse>> findById(Integer categoryId) {
                String cacheKey = "category:" + categoryId;

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                CategoryResponse cachedResponse = fromJson(cachedJson,
                                                                CategoryResponse.class);
                                                return Uni.createFrom().item(ApiResponse.success(
                                                                "Category retrieved successfully", cachedResponse));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        Attributes attrs = Attributes.builder()
                                                        .put("category.id", categoryId.toString())
                                                        .build();

                                        return runTraced("findCategoryById", "find_category_by_id", attrs,
                                                        () -> categoryQueryRepository
                                                                        .findCategoryById(categoryId.longValue())
                                                                        .chain(category -> {
                                                                                if (category == null) {
                                                                                        logger.warn("Category not found with id: {}",
                                                                                                        categoryId);
                                                                                        return Uni.createFrom().item(
                                                                                                        new ApiResponse<>(
                                                                                                                        "error",
                                                                                                                        "Category not found",
                                                                                                                        (CategoryResponse) null));
                                                                                }

                                                                                CategoryResponse categoryResponse = CategoryResponse
                                                                                                .from(category);

                                                                                return redisService.setReactive(
                                                                                                cacheKey,
                                                                                                toJson(categoryResponse))
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached category for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully found category with id: {} and name: {}",
                                                                                                                        categoryId,
                                                                                                                        category.getName());
                                                                                                        return ApiResponse
                                                                                                                        .success("Category retrieved successfully",
                                                                                                                                        categoryResponse);
                                                                                                });
                                                                        })
                                                                        .onFailure().recoverWithItem(e -> {
                                                                                logger.error("Failed to fetch category by id={}: {}",
                                                                                                categoryId,
                                                                                                e.getMessage(), e);
                                                                                return new ApiResponse<>("error",
                                                                                                "Failed to fetch category: "
                                                                                                                + e.getMessage(),
                                                                                                (CategoryResponse) null);
                                                                        }));
                                });
        }

        private <T, R> ApiResponsePagination<List<R>> buildPaginatedResponse(
                        PagedResult<T> pagedResult,
                        FindAllCategory request,
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