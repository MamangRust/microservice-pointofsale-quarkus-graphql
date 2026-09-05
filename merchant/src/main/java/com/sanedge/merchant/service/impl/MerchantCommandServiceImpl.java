package com.sanedge.merchant.service.impl;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.enums.Status;
import com.sanedge.common.exception.ResourceAlreadyExistsException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.common.utils.ApiKeyGenerator;
import com.sanedge.merchant.domain.requests.CreateMerchantRequest;
import com.sanedge.merchant.domain.requests.UpdateMerchantRequest;
import com.sanedge.merchant.domain.response.MerchantResponse;
import com.sanedge.merchant.domain.response.MerchantResponseDeleteAt;
import com.sanedge.merchant.entity.Merchant;
import com.sanedge.merchant.repository.MerchantCommandRepository;
import com.sanedge.merchant.repository.MerchantQueryRepository;
import com.sanedge.merchant.service.MerchantCommandService;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MerchantCommandServiceImpl implements MerchantCommandService {
        private static final Logger logger = LoggerFactory.getLogger(MerchantCommandServiceImpl.class);

        @GrpcClient("user")
        pb.user.MutinyUserQueryServiceGrpc.MutinyUserQueryServiceStub userQueryService;

        private final MerchantQueryRepository merchantQueryRepository;
        private final MerchantCommandRepository merchantCommandRepository;
        private final RedisService redisService;
        private final TracingMetrics tracingMetrics;

        @Inject
        public MerchantCommandServiceImpl(
                        MerchantQueryRepository merchantQueryRepository,
                        MerchantCommandRepository merchantCommandRepository,
                        RedisService redisService,
                        TracingMetrics tracingMetrics) {
                this.merchantQueryRepository = merchantQueryRepository;
                this.merchantCommandRepository = merchantCommandRepository;
                this.redisService = redisService;
                this.tracingMetrics = tracingMetrics;
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<MerchantResponse>> createMerchant(CreateMerchantRequest req) {
                logger.info("Creating merchant | Name: {}, UserId: {}", req.getName(), req.getUserId());
                Attributes attrs = Attributes.builder()
                                .put("merchant.name", req.getName())
                                .put("user.id", req.getUserId().toString())
                                .build();

                return runTraced("createMerchant", "create_merchant", attrs,
                                () -> userQueryService
                                                .findById(pb.user.User.FindByIdUserRequest.newBuilder()
                                                                .setId(req.getUserId().intValue()).build())
                                                .chain(response -> {
                                                        if (response == null || !response.hasData()) {
                                                                logger.error("User not found with id {}",
                                                                                req.getUserId());
                                                                throw new ResourceNotFoundException("User not found");
                                                        }
                                                        return merchantQueryRepository.existsByName(req.getName());
                                                })
                                                .chain(nameExists -> {
                                                        if (nameExists) {
                                                                logger.error("Merchant name already taken | Name: {}",
                                                                                req.getName());
                                                                throw new ResourceAlreadyExistsException(
                                                                                "Merchant name already taken");
                                                        }

                                                        String apiKey = ApiKeyGenerator.generateApiKey();
                                                        UUID merchantNo = UUID.randomUUID();

                                                        Merchant merchant = new Merchant();
                                                        merchant.setName(req.getName());
                                                        merchant.setMerchantNo(merchantNo);
                                                        merchant.setUserId(req.getUserId().intValue());
                                                        merchant.setApiKey(apiKey);
                                                        merchant.setStatus(Status.PENDING);

                                                        return merchantCommandRepository.persist(merchant)
                                                                        .chain(savedMerchant -> {
                                                                                logger.info("Merchant created successfully | Id: {}, ApiKey: {}",
                                                                                                merchant.getMerchantId(),
                                                                                                apiKey);
                                                                                return Uni.createFrom().item(ApiResponse
                                                                                                .success("Merchant created successfully",
                                                                                                                MerchantResponse.from(
                                                                                                                                merchant)));
                                                                        });
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to create merchant | Name: {}, UserId: {}",
                                                                        req.getName(), req.getUserId(), e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to create merchant: " + e.getMessage(),
                                                                        (MerchantResponse) null);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<MerchantResponse>> updateMerchant(UpdateMerchantRequest req) {
                logger.info("Updating merchant | Id: {}", req.getMerchantId());
                Attributes attrs = Attributes.builder()
                                .put("merchant.id", req.getMerchantId().toString())
                                .build();

                return runTraced("updateMerchant", "update_merchant", attrs,
                                () -> merchantQueryRepository.findMerchantById(req.getMerchantId())
                                                .chain(merchant -> {
                                                        if (merchant == null) {
                                                                logger.error("Merchant not found with id {}",
                                                                                req.getMerchantId());
                                                                throw new ResourceNotFoundException(
                                                                                "Merchant not found");
                                                        }

                                                        Uni<Void> userCheckUni = Uni.createFrom().nullItem();
                                                        if (req.getUserId() != null) {
                                                                userCheckUni = userQueryService.findById(
                                                                                pb.user.User.FindByIdUserRequest
                                                                                                .newBuilder()
                                                                                                .setId(req.getUserId()
                                                                                                                .intValue())
                                                                                                .build())
                                                                                .chain(response -> {
                                                                                        if (response == null
                                                                                                        || !response.hasData()) {
                                                                                                logger.error("User not found with id {}",
                                                                                                                req.getUserId());
                                                                                                throw new ResourceNotFoundException(
                                                                                                                "User not found");
                                                                                        }
                                                                                        merchant.setUserId(req
                                                                                                        .getUserId()
                                                                                                        .intValue());
                                                                                        return Uni.createFrom()
                                                                                                        .nullItem();
                                                                                });
                                                        }

                                                        return userCheckUni.chain(v -> {
                                                                merchant.setName(req.getName());
                                                                merchant.setStatus(Status.valueOf(
                                                                                req.getStatus().toUpperCase()));

                                                                return merchantCommandRepository.persist(merchant)
                                                                                .chain(savedMerchant -> {
                                                                                        String cacheIdKey = "merchant:id:"
                                                                                                        + req.getMerchantId();
                                                                                        String cacheApiKey = "merchant:apikey:"
                                                                                                        + merchant.getApiKey();
                                                                                        String cacheUserKey = "merchant:user:"
                                                                                                        + merchant.getUserId();

                                                                                        return Uni.combine().all().unis(
                                                                                                        redisService.deleteReactive(
                                                                                                                        cacheIdKey),
                                                                                                        redisService.deleteReactive(
                                                                                                                        cacheApiKey),
                                                                                                        redisService.deleteReactive(
                                                                                                                        cacheUserKey))
                                                                                                        .asTuple()
                                                                                                        .map(v2 -> {
                                                                                                                logger.info("Merchant updated successfully | Id: {}",
                                                                                                                                req.getMerchantId());
                                                                                                                return ApiResponse
                                                                                                                                .success("Merchant updated successfully",
                                                                                                                                                MerchantResponse.from(
                                                                                                                                                                merchant));
                                                                                                        });
                                                                                });
                                                        });
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to update merchant | Id: {}",
                                                                        req.getMerchantId(), e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to update merchant: " + e.getMessage(),
                                                                        (MerchantResponse) null);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<MerchantResponseDeleteAt>> trashMerchant(Long id) {
                logger.info("Trashing merchant id={}", id);
                Attributes attrs = Attributes.builder()
                                .put("merchant.id", id.toString())
                                .build();

                return runTraced("trashMerchant", "trash_merchant", attrs,
                                () -> merchantCommandRepository.trashed(id)
                                                .chain(merchant -> {
                                                        if (merchant == null) {
                                                                logger.error("Merchant not found with id {}", id);
                                                                throw new ResourceNotFoundException(
                                                                                "Merchant not found");
                                                        }

                                                        String cacheIdKey = "merchant:id:" + id;
                                                        String cacheApiKey = "merchant:apikey:" + merchant.getApiKey();
                                                        String cacheUserKey = "merchant:user:" + merchant.getUserId();

                                                        return Uni.combine().all().unis(
                                                                        redisService.deleteReactive(cacheIdKey),
                                                                        redisService.deleteReactive(cacheApiKey),
                                                                        redisService.deleteReactive(cacheUserKey))
                                                                        .asTuple().map(v2 -> {
                                                                                logger.info("Merchant trashed successfully | Id: {}",
                                                                                                id);
                                                                                return ApiResponse.success(
                                                                                                "Merchant trashed successfully",
                                                                                                MerchantResponseDeleteAt
                                                                                                                .from(merchant));
                                                                        });
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to trash merchant id={}", id, e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to trash merchant: " + e.getMessage(),
                                                                        (MerchantResponseDeleteAt) null);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<MerchantResponseDeleteAt>> restoreMerchant(Long id) {
                logger.info("Restoring merchant id={}", id);
                Attributes attrs = Attributes.builder()
                                .put("merchant.id", id.toString())
                                .build();

                return runTraced("restoreMerchant", "restore_merchant", attrs,
                                () -> merchantCommandRepository.restore(id)
                                                .chain(merchant -> {
                                                        if (merchant == null) {
                                                                logger.error("Merchant not found with id {}", id);
                                                                throw new ResourceNotFoundException(
                                                                                "Merchant not found");
                                                        }

                                                        String cacheIdKey = "merchant:id:" + id;
                                                        String cacheApiKey = "merchant:apikey:" + merchant.getApiKey();
                                                        String cacheUserKey = "merchant:user:" + merchant.getUserId();

                                                        return Uni.combine().all().unis(
                                                                        redisService.deleteReactive(cacheIdKey),
                                                                        redisService.deleteReactive(cacheApiKey),
                                                                        redisService.deleteReactive(cacheUserKey))
                                                                        .asTuple().map(v2 -> {
                                                                                logger.info("Merchant restored successfully | Id: {}",
                                                                                                id);
                                                                                return ApiResponse.success(
                                                                                                "Merchant restored successfully",
                                                                                                MerchantResponseDeleteAt
                                                                                                                .from(merchant));
                                                                        });
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to restore merchant id={}", id, e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to restore merchant: " + e.getMessage(),
                                                                        (MerchantResponseDeleteAt) null);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Boolean>> deleteMerchant(Long id) {
                logger.info("Permanently deleting merchant id={}", id);
                Attributes attrs = Attributes.builder()
                                .put("merchant.id", id.toString())
                                .build();

                return runTraced("deleteMerchant", "delete_merchant", attrs,
                                () -> merchantQueryRepository.findMerchantById(id)
                                                .chain(merchant -> {
                                                        if (merchant == null) {
                                                                logger.error("Merchant not found with id {}", id);
                                                                throw new ResourceNotFoundException(
                                                                                "Merchant not found");
                                                        }

                                                        String cacheIdKey = "merchant:id:" + id;
                                                        String cacheApiKey = "merchant:apikey:" + merchant.getApiKey();
                                                        String cacheUserKey = "merchant:user:" + merchant.getUserId();

                                                        return merchantCommandRepository.deletePermanent(id)
                                                                        .chain(deleted -> Uni.combine().all().unis(
                                                                                        redisService.deleteReactive(
                                                                                                        cacheIdKey),
                                                                                        redisService.deleteReactive(
                                                                                                        cacheApiKey),
                                                                                        redisService.deleteReactive(
                                                                                                        cacheUserKey))
                                                                                        .asTuple().map(v2 -> {
                                                                                                logger.info("Merchant permanently deleted | Id: {}",
                                                                                                                id);
                                                                                                return ApiResponse
                                                                                                                .success("Merchant permanently deleted",
                                                                                                                                true);
                                                                                        }));
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to permanently delete merchant id={}", id,
                                                                        e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to delete merchant: " + e.getMessage(),
                                                                        false);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Boolean>> restoreAll() {
                logger.info("Restoring ALL trashed merchants");

                return runTraced("restoreAllMerchants", "restore_all_merchants", Attributes.empty(),
                                () -> merchantCommandRepository.restoreAllDeleted()
                                                .map(restored -> {
                                                        if (!restored) {
                                                                throw new ResourceNotFoundException(
                                                                                "No trashed merchants");
                                                        }

                                                        logger.info("Restored all trashed merchants");
                                                        return ApiResponse.success("Restored all trashed merchants",
                                                                        restored);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Boolean>> deleteAll() {
                logger.info("Permanently deleting ALL trashed merchants");

                return runTraced("deleteAllMerchants", "delete_all_merchants", Attributes.empty(),
                                () -> merchantCommandRepository.deleteAllDeleted()
                                                .map(deleted -> {
                                                        if (!deleted) {
                                                                throw new ResourceNotFoundException(
                                                                                "No trashed merchants");
                                                        }

                                                        logger.info("Deleted all trashed merchants");
                                                        return ApiResponse.success("Deleted all trashed merchants",
                                                                        deleted);
                                                }));
        }

        private <T> Uni<T> runTraced(String operationName, String method, Attributes attributes,
                        java.util.function.Supplier<Uni<T>> supplier) {
                return tracingMetrics.traceAndMeasure(operationName, method, attributes, supplier);
        }
}