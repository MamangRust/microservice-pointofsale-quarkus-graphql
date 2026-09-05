package com.sanedge.merchant.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.merchant.domain.requests.CreateMerchantDocumentRequest;
import com.sanedge.merchant.domain.requests.UpdateMerchantDocumentRequest;
import com.sanedge.merchant.domain.requests.UpdateMerchantDocumentStatus;
import com.sanedge.merchant.domain.response.MerchantDocumentResponse;
import com.sanedge.merchant.domain.response.MerchantDocumentResponseDeleteAt;
import com.sanedge.merchant.entity.MerchantDocument;
import com.sanedge.merchant.repository.MerchantDocumentCommandRepository;
import com.sanedge.merchant.repository.MerchantDocumentQueryRepository;
import com.sanedge.merchant.repository.MerchantQueryRepository;
import com.sanedge.merchant.service.MerchantDocumentCommandService;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MerchantDocumentCommandServiceImpl implements MerchantDocumentCommandService {
        private static final Logger logger = LoggerFactory.getLogger(MerchantDocumentCommandServiceImpl.class);

        private final MerchantQueryRepository merchantQueryRepository;
        private final MerchantDocumentQueryRepository merchantDocumentQueryRepository;
        private final MerchantDocumentCommandRepository merchantDocumentCommandRepository;
        private final RedisService redisService;
        private final TracingMetrics tracingMetrics;

        @Inject
        public MerchantDocumentCommandServiceImpl(
                        MerchantQueryRepository merchantQueryRepository,
                        MerchantDocumentQueryRepository merchantDocumentQueryRepository,
                        MerchantDocumentCommandRepository merchantDocumentCommandRepository,
                        RedisService redisService,
                        TracingMetrics tracingMetrics) {
                this.merchantQueryRepository = merchantQueryRepository;
                this.merchantDocumentQueryRepository = merchantDocumentQueryRepository;
                this.merchantDocumentCommandRepository = merchantDocumentCommandRepository;
                this.redisService = redisService;
                this.tracingMetrics = tracingMetrics;
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<MerchantDocumentResponse>> create(CreateMerchantDocumentRequest req) {
                logger.info("Creating merchant document | MerchantId: {}, Type: {}", req.getMerchantId(),
                                req.getDocumentType());
                Attributes attrs = Attributes.builder()
                                .put("merchant.id", req.getMerchantId().toString())
                                .build();

                return runTraced("createMerchantDocument", "create_merchant_document", attrs,
                                () -> merchantQueryRepository.findMerchantById(req.getMerchantId())
                                                .chain(merchant -> {
                                                        if (merchant == null) {
                                                                logger.error("Merchant not found with id {}",
                                                                                req.getMerchantId());
                                                                throw new ResourceNotFoundException(
                                                                                "Merchant not found");
                                                        }

                                                        MerchantDocument doc = new MerchantDocument();
                                                        doc.setMerchantId(req.getMerchantId().intValue());
                                                        doc.setDocumentType(req.getDocumentType());
                                                        doc.setDocumentUrl(req.getDocumentUrl());
                                                        doc.setStatus("PENDING");

                                                        return merchantDocumentCommandRepository.persist(doc)
                                                                        .chain(savedDoc -> {
                                                                                logger.info("Merchant document created successfully | Id: {}",
                                                                                                savedDoc.getDocumentId());
                                                                                return Uni.createFrom()
                                                                                                .item(ApiResponse
                                                                                                                .success("Merchant document created successfully",
                                                                                                                                MerchantDocumentResponse
                                                                                                                                                .from(savedDoc)));
                                                                        });
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to create merchant document", e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to create merchant document: "
                                                                                        + e.getMessage(),
                                                                        (MerchantDocumentResponse) null);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<MerchantDocumentResponse>> update(UpdateMerchantDocumentRequest req) {
                logger.info("Updating merchant document | Id: {}", req.getDocumentId());
                Attributes attrs = Attributes.builder()
                                .put("doc.id", req.getDocumentId().toString())
                                .build();

                return runTraced("updateMerchantDocument", "update_merchant_document", attrs,
                                () -> merchantDocumentQueryRepository.findDocumentById(req.getDocumentId())
                                                .chain(doc -> {
                                                        if (doc == null) {
                                                                logger.error("Merchant document not found with id {}",
                                                                                req.getDocumentId());
                                                                throw new ResourceNotFoundException(
                                                                                "Merchant document not found");
                                                        }

                                                        return merchantQueryRepository
                                                                        .findMerchantById(req.getMerchantId())
                                                                        .chain(merchant -> {
                                                                                if (merchant == null) {
                                                                                        logger.error("Merchant not found with id {}",
                                                                                                        req.getMerchantId());
                                                                                        throw new ResourceNotFoundException(
                                                                                                        "Merchant not found");
                                                                                }

                                                                                doc.setMerchantId(req.getMerchantId()
                                                                                                .intValue());
                                                                                doc.setDocumentType(
                                                                                                req.getDocumentType());
                                                                                doc.setDocumentUrl(
                                                                                                req.getDocumentUrl());
                                                                                doc.setNote(req.getNote());
                                                                                doc.setStatus(req.getStatus());

                                                                                return merchantDocumentCommandRepository
                                                                                                .persist(doc)
                                                                                                .chain(savedDoc -> {
                                                                                                        String cacheKey = "merchant_doc:id:"
                                                                                                                        + req.getDocumentId();

                                                                                                        return redisService
                                                                                                                        .deleteReactive(cacheKey)
                                                                                                                        .map(v -> {
                                                                                                                                logger.info(
                                                                                                                                                "Merchant document updated successfully | Id: {}",
                                                                                                                                                req.getDocumentId());
                                                                                                                                return ApiResponse
                                                                                                                                                .success(
                                                                                                                                                                "Merchant document updated successfully",
                                                                                                                                                                MerchantDocumentResponse
                                                                                                                                                                                .from(savedDoc));
                                                                                                                        });
                                                                                                });
                                                                        });
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to update merchant document", e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to update merchant document: "
                                                                                        + e.getMessage(),
                                                                        (MerchantDocumentResponse) null);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<MerchantDocumentResponse>> updateStatus(UpdateMerchantDocumentStatus req) {
                logger.info("Updating merchant document status | Id: {}", req.getDocumentId());
                Attributes attrs = Attributes.builder()
                                .put("doc.id", req.getDocumentId().toString())
                                .build();

                return runTraced("updateMerchantDocumentStatus", "update_merchant_document_status", attrs,
                                () -> merchantDocumentQueryRepository.findDocumentById(req.getDocumentId())
                                                .chain(doc -> {
                                                        if (doc == null) {
                                                                logger.error("Merchant document not found with id {}",
                                                                                req.getDocumentId());
                                                                throw new ResourceNotFoundException(
                                                                                "Merchant document not found");
                                                        }

                                                        return merchantQueryRepository
                                                                        .findMerchantById(req.getMerchantId())
                                                                        .chain(merchant -> {
                                                                                if (merchant == null) {
                                                                                        logger.error("Merchant not found with id {}",
                                                                                                        req.getMerchantId());
                                                                                        throw new ResourceNotFoundException(
                                                                                                        "Merchant not found");
                                                                                }

                                                                                doc.setStatus(req.getStatus());
                                                                                doc.setNote(req.getNote());

                                                                                return merchantDocumentCommandRepository
                                                                                                .persist(doc)
                                                                                                .chain(savedDoc -> {
                                                                                                        String cacheKey = "merchant_doc:id:"
                                                                                                                        + req.getDocumentId();

                                                                                                        return redisService
                                                                                                                        .deleteReactive(cacheKey)
                                                                                                                        .map(v -> {
                                                                                                                                logger.info(
                                                                                                                                                "Merchant document status updated successfully | Id: {}",
                                                                                                                                                req.getDocumentId());
                                                                                                                                return ApiResponse
                                                                                                                                                .success(
                                                                                                                                                                "Merchant document status updated successfully",
                                                                                                                                                                MerchantDocumentResponse
                                                                                                                                                                                .from(savedDoc));
                                                                                                                        });
                                                                                                });
                                                                        });
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to update merchant document status", e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to update merchant document status: "
                                                                                        + e.getMessage(),
                                                                        (MerchantDocumentResponse) null);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<MerchantDocumentResponseDeleteAt>> trash(Long id) {
                logger.info("Trashing merchant document | Id: {}", id);
                Attributes attrs = Attributes.builder()
                                .put("doc.id", id.toString())
                                .build();

                return runTraced("trashMerchantDocument", "trash_merchant_document", attrs,
                                () -> merchantDocumentCommandRepository.trashed(id)
                                                .chain(doc -> {
                                                        if (doc == null) {
                                                                logger.error("Merchant document not found with id {}",
                                                                                id);
                                                                throw new ResourceNotFoundException(
                                                                                "Merchant document not found");
                                                        }

                                                        String cacheKey = "merchant_doc:id:" + id;

                                                        return redisService.deleteReactive(cacheKey)
                                                                        .map(v -> {
                                                                                logger.info("Merchant document trashed successfully | Id: {}",
                                                                                                id);
                                                                                return ApiResponse.success(
                                                                                                "Merchant document trashed successfully",
                                                                                                MerchantDocumentResponseDeleteAt
                                                                                                                .from(doc));
                                                                        });
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to trash merchant document", e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to trash merchant document: "
                                                                                        + e.getMessage(),
                                                                        (MerchantDocumentResponseDeleteAt) null);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<MerchantDocumentResponseDeleteAt>> restore(Long id) {
                logger.info("Restoring merchant document | Id: {}", id);
                Attributes attrs = Attributes.builder()
                                .put("doc.id", id.toString())
                                .build();

                return runTraced("restoreMerchantDocument", "restore_merchant_document", attrs,
                                () -> merchantDocumentCommandRepository.restore(id)
                                                .chain(doc -> {
                                                        if (doc == null) {
                                                                logger.error("Merchant document not found with id {}",
                                                                                id);
                                                                throw new ResourceNotFoundException(
                                                                                "Merchant document not found");
                                                        }

                                                        String cacheKey = "merchant_doc:id:" + id;

                                                        return redisService.deleteReactive(cacheKey)
                                                                        .map(v -> {
                                                                                logger.info("Merchant document restored successfully | Id: {}",
                                                                                                id);
                                                                                return ApiResponse.success(
                                                                                                "Merchant document restored successfully",
                                                                                                MerchantDocumentResponseDeleteAt
                                                                                                                .from(doc));
                                                                        });
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to restore merchant document", e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to restore merchant document: "
                                                                                        + e.getMessage(),
                                                                        (MerchantDocumentResponseDeleteAt) null);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Boolean>> deletePermanent(Long id) {
                logger.info("Permanently deleting merchant document | Id: {}", id);
                Attributes attrs = Attributes.builder()
                                .put("doc.id", id.toString())
                                .build();

                return runTraced("deletePermanentMerchantDocument", "delete_permanent_merchant_document", attrs,
                                () -> merchantDocumentCommandRepository.deletePermanent(id)
                                                .chain(success -> {
                                                        if (!success) {
                                                                logger.error("Merchant document not found with id {}",
                                                                                id);
                                                                throw new ResourceNotFoundException(
                                                                                "Merchant document not found");
                                                        }

                                                        String cacheKey = "merchant_doc:id:" + id;

                                                        return redisService.deleteReactive(cacheKey)
                                                                        .map(v -> {
                                                                                logger.info("Merchant document permanently deleted | Id: {}",
                                                                                                id);
                                                                                return ApiResponse.success(
                                                                                                "Merchant document permanently deleted",
                                                                                                true);
                                                                        });
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to permanently delete merchant document",
                                                                        e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to delete merchant document: "
                                                                                        + e.getMessage(),
                                                                        false);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Boolean>> restoreAll() {
                logger.info("Restoring all merchant documents");

                return runTraced("restoreAllMerchantDocuments", "restore_all_merchant_documents", Attributes.empty(),
                                () -> merchantDocumentCommandRepository.restoreAllDeleted()
                                                .map(success -> {
                                                        logger.info("All merchant documents restored successfully");
                                                        return ApiResponse.success(
                                                                        "All merchant documents restored successfully",
                                                                        success);
                                                })
                                                .onFailure().recoverWithItem(e -> {
                                                        logger.error("Failed to restore all merchant documents", e);
                                                        return new ApiResponse<>("error",
                                                                        "Failed to restore all merchant documents: "
                                                                                        + e.getMessage(),
                                                                        false);
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Boolean>> deleteAllPermanent() {
                logger.info("Permanently deleting all trashed merchant documents");

                return runTraced("deleteAllPermanentMerchantDocuments", "delete_all_permanent_merchant_documents",
                                Attributes.empty(),
                                () -> merchantDocumentCommandRepository.deleteAllDeleted()
                                                .map(success -> {
                                                        if (!success) {
                                                                throw new ResourceNotFoundException(
                                                                                "No trashed merchant documents");
                                                        }

                                                        logger.info("All trashed merchant documents permanently deleted");
                                                        return ApiResponse.success(
                                                                        "All trashed merchant documents permanently deleted",
                                                                        success);
                                                }));
        }

        private <T> Uni<T> runTraced(String operationName, String method, Attributes attributes,
                        java.util.function.Supplier<Uni<T>> supplier) {
                return tracingMetrics.traceAndMeasure(operationName, method, attributes, supplier);
        }
}