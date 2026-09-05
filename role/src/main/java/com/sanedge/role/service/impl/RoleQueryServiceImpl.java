package com.sanedge.role.service.impl;

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
import com.sanedge.role.domain.requests.FindAllRoles;
import com.sanedge.role.domain.response.RoleResponse;
import com.sanedge.role.domain.response.RoleResponseDeleteAt;
import com.sanedge.role.repository.RoleRepository;
import com.sanedge.role.service.RoleQueryService;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class RoleQueryServiceImpl implements RoleQueryService {
        private static final Logger logger = LoggerFactory.getLogger(RoleQueryServiceImpl.class);

        private final RoleRepository roleRepository;
        private final RedisService redisService;
        private final ObjectMapper objectMapper;
        private final TracingMetrics tracingMetrics;

        private static final long LIST_CACHE_TTL_SECONDS = 300;

        @Inject
        public RoleQueryServiceImpl(RoleRepository roleRepository, RedisService redisService,
                        ObjectMapper objectMapper, TracingMetrics tracingMetrics) {
                this.roleRepository = roleRepository;
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
        public Uni<ApiResponsePagination<List<RoleResponse>>> findAllPaginated(FindAllRoles request) {
                String cacheKey = String.format("roles:all:%d:%d:%s", request.getPage(), request.getPageSize(),
                                request.getSearch());

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<RoleResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<RoleResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return runTraced("findAllRoles", "find_all_roles", Attributes.empty(),
                                                        () -> roleRepository.findRoles(request)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<RoleResponse>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                request,
                                                                                                "Roles retrieved successfully",
                                                                                                RoleResponse::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} roles",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        })
                                                                        .onFailure().recoverWithItem(e -> {
                                                                                logger.error("Failed to fetch roles: {}",
                                                                                                e.getMessage(),
                                                                                                e);
                                                                                return new ApiResponsePagination<>(
                                                                                                "error",
                                                                                                "Failed to fetch roles: "
                                                                                                                + e.getMessage(),
                                                                                                Collections.emptyList(),
                                                                                                null);
                                                                        }));
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponsePagination<List<RoleResponseDeleteAt>>> findActivePaginated(FindAllRoles request) {
                String cacheKey = String.format("roles:active:%d:%d:%s", request.getPage(), request.getPageSize(),
                                request.getSearch());

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<RoleResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<RoleResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return runTraced("findActiveRoles", "find_active_roles", Attributes.empty(),
                                                        () -> roleRepository.findActiveRoles(request)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<RoleResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                request,
                                                                                                "Active roles retrieved successfully",
                                                                                                RoleResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} active roles",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        })
                                                                        .onFailure().recoverWithItem(e -> {
                                                                                logger.error("Failed to fetch active roles: {}",
                                                                                                e.getMessage(),
                                                                                                e);
                                                                                return new ApiResponsePagination<>(
                                                                                                "error",
                                                                                                "Failed to fetch active roles: "
                                                                                                                + e.getMessage(),
                                                                                                Collections.emptyList(),
                                                                                                null);
                                                                        }));
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponsePagination<List<RoleResponseDeleteAt>>> findTrashedPaginated(FindAllRoles request) {
                String cacheKey = String.format("roles:trashed:%d:%d:%s", request.getPage(), request.getPageSize(),
                                request.getSearch());

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<RoleResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<RoleResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        return runTraced("findTrashedRoles", "find_trashed_roles", Attributes.empty(),
                                                        () -> roleRepository.findTrashedRoles(request)
                                                                        .chain(pagedResult -> {
                                                                                ApiResponsePagination<List<RoleResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                pagedResult,
                                                                                                request,
                                                                                                "Trashed roles retrieved successfully",
                                                                                                RoleResponseDeleteAt::from);

                                                                                return redisService
                                                                                                .setWithExpirationReactive(
                                                                                                                cacheKey,
                                                                                                                toJson(response),
                                                                                                                LIST_CACHE_TTL_SECONDS)
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached response for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully retrieved {} trashed roles",
                                                                                                                        pagedResult.getTotalRecords());
                                                                                                        return response;
                                                                                                });
                                                                        })
                                                                        .onFailure().recoverWithItem(e -> {
                                                                                logger.error("Failed to fetch trashed roles: {}",
                                                                                                e.getMessage(),
                                                                                                e);
                                                                                return new ApiResponsePagination<>(
                                                                                                "error",
                                                                                                "Failed to fetch trashed roles: "
                                                                                                                + e.getMessage(),
                                                                                                Collections.emptyList(),
                                                                                                null);
                                                                        }));
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<RoleResponse>> findById(Long id) {
                String cacheKey = "role:" + id;

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                RoleResponse cachedRole = fromJson(cachedJson, RoleResponse.class);
                                                return Uni.createFrom()
                                                                .item(ApiResponse.success("Role found", cachedRole));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        Attributes attrs = Attributes.builder()
                                                        .put("role.id", id.toString())
                                                        .build();

                                        return runTraced("findRoleById", "find_role_by_id", attrs,
                                                        () -> roleRepository.findById(id)
                                                                        .chain(role -> {
                                                                                if (role == null) {
                                                                                        logger.warn("Role not found with id: {}",
                                                                                                        id);
                                                                                        throw new NotFoundException(
                                                                                                        "Role not found with id: "
                                                                                                                        + id);
                                                                                }

                                                                                RoleResponse roleResponse = RoleResponse
                                                                                                .from(role);

                                                                                return redisService.setReactive(
                                                                                                cacheKey,
                                                                                                toJson(roleResponse))
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached role for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully found role with id: {} and name: {}",
                                                                                                                        id,
                                                                                                                        role.getRoleName());
                                                                                                        return ApiResponse
                                                                                                                        .success("Role found",
                                                                                                                                        roleResponse);
                                                                                                });
                                                                        })
                                                                        .onFailure().recoverWithItem(e -> {
                                                                                logger.error("Failed to fetch role by id={}: {}",
                                                                                                id, e.getMessage(), e);
                                                                                return new ApiResponse<>("error",
                                                                                                "Failed to fetch role: "
                                                                                                                + e.getMessage(),
                                                                                                (RoleResponse) null);
                                                                        }));
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<List<RoleResponse>>> findByUserId(Long userId) {
                String cacheKey = "roles:user:" + userId;

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                List<RoleResponse> cachedRoles = fromJson(cachedJson,
                                                                new TypeReference<List<RoleResponse>>() {
                                                                });
                                                return Uni.createFrom()
                                                                .item(ApiResponse.success("Roles found", cachedRoles));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        Attributes attrs = Attributes.builder()
                                                        .put("user.id", userId.toString())
                                                        .build();

                                        return runTraced("findRolesByUserId", "find_roles_by_user_id", attrs,
                                                        () -> roleRepository.findUserRoles(userId)
                                                                        .chain(roles -> {
                                                                                List<RoleResponse> responses = roles
                                                                                                .stream()
                                                                                                .map(RoleResponse::from)
                                                                                                .collect(Collectors
                                                                                                                .toList());

                                                                                return redisService.setReactive(
                                                                                                cacheKey,
                                                                                                toJson(responses))
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached roles for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully found {} roles for user id: {}",
                                                                                                                        responses.size(),
                                                                                                                        userId);
                                                                                                        return ApiResponse
                                                                                                                        .success("Roles found",
                                                                                                                                        responses);
                                                                                                });
                                                                        })
                                                                        .onFailure().recoverWithItem(e -> {
                                                                                logger.error("Failed to fetch roles by user id={}: {}",
                                                                                                userId, e.getMessage(),
                                                                                                e);
                                                                                return new ApiResponse<>("error",
                                                                                                "Failed to fetch roles: "
                                                                                                                + e.getMessage(),
                                                                                                Collections.emptyList());
                                                                        }));
                                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<RoleResponse>> findByName(String name) {
                String cacheKey = "role:name:" + name;

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                RoleResponse cachedRole = fromJson(cachedJson, RoleResponse.class);
                                                return Uni.createFrom()
                                                                .item(ApiResponse.success("Role found", cachedRole));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        Attributes attrs = Attributes.builder()
                                                        .put("role.name", name)
                                                        .build();

                                        return runTraced("findRoleByName", "find_role_by_name", attrs,
                                                        () -> roleRepository.findByRoleName(name)
                                                                        .chain(role -> {
                                                                                if (role == null) {
                                                                                        logger.warn("Role not found with name: {}",
                                                                                                        name);
                                                                                        throw new NotFoundException(
                                                                                                        "Role not found with name: "
                                                                                                                        + name);
                                                                                }

                                                                                RoleResponse roleResponse = RoleResponse
                                                                                                .from(role);

                                                                                return redisService.setReactive(
                                                                                                cacheKey,
                                                                                                toJson(roleResponse))
                                                                                                .map(v -> {
                                                                                                        logger.info("Cached role for key: {}",
                                                                                                                        cacheKey);
                                                                                                        logger.info("Successfully found role with name: {}",
                                                                                                                        role.getRoleName());
                                                                                                        return ApiResponse
                                                                                                                        .success("Role found",
                                                                                                                                        roleResponse);
                                                                                                });
                                                                        })
                                                                        .onFailure().recoverWithItem(e -> {
                                                                                logger.error("Failed to fetch role by name={}: {}",
                                                                                                name, e.getMessage(),
                                                                                                e);
                                                                                return new ApiResponse<>("error",
                                                                                                "Failed to fetch role: "
                                                                                                                + e.getMessage(),
                                                                                                (RoleResponse) null);
                                                                        }));
                                });
        }

        private <T, R> ApiResponsePagination<List<R>> buildPaginatedResponse(
                        PagedResult<T> pagedResult,
                        FindAllRoles request,
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