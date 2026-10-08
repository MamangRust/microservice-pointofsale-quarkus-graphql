package com.sanedge.cashier.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.cashier.domain.requests.CreateCashierRequest;
import com.sanedge.cashier.domain.requests.UpdateCashierRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.cashier.domain.response.CashierResponse;
import com.sanedge.cashier.domain.response.CashierResponseDeleteAt;
import com.sanedge.cashier.entity.Cashier;
import com.sanedge.common.exception.ResourceAlreadyExistsException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.cashier.repository.CashierCommandRepository;
import com.sanedge.cashier.repository.CashierQueryRepository;
import com.sanedge.cashier.service.CashierCommandService;

import com.sanedge.common.adapter.merchant.MerchantQueryPort;
import com.sanedge.common.adapter.user.UserQueryPort;
import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;

@ApplicationScoped
public class CashierCommandServiceImpl implements CashierCommandService {
        private static final Logger logger = LoggerFactory.getLogger(CashierCommandServiceImpl.class);

        CashierCommandRepository cashierCommandRepository;
        CashierQueryRepository cashierQueryRepository;
        RedisService redisService;
        TracingMetrics tracingMetrics;
        private final MerchantQueryPort merchantQueryPort;
        private final UserQueryPort userQueryPort;

        @Inject
        public CashierCommandServiceImpl(CashierCommandRepository cashierCommandRepository,
                        CashierQueryRepository cashierQueryRepository,
                        RedisService redisService,
                        TracingMetrics tracingMetrics,
                        MerchantQueryPort merchantQueryPort,
                        UserQueryPort userQueryPort) {
                this.cashierCommandRepository = cashierCommandRepository;
                this.cashierQueryRepository = cashierQueryRepository;
                this.redisService = redisService;
                this.tracingMetrics = tracingMetrics;
                this.merchantQueryPort = merchantQueryPort;
                this.userQueryPort = userQueryPort;
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<CashierResponse>> createCashier(CreateCashierRequest req) {
                Attributes attrs = Attributes.builder()
                                .put("cashier.name", req.getName())
                                .build();

                return runTraced("createCashier", "create_cashier", attrs, () -> {
                        logger.info("Creating cashier for merchantId={}, userId={}, name={}", req.getMerchantId(),
                                        req.getUserId(), req.getName());

                        return merchantQueryPort.findByMerchantId(req.getMerchantId())
                                        .chain(merchant -> userQueryPort.findById(req.getUserId().intValue()))
                                        .chain(user -> cashierQueryRepository.findByNameAndMerchantId(req.getName(),
                                                        req.getMerchantId().longValue()))
                                        .chain(existingCashier -> {
                                                if (existingCashier != null) {
                                                        throw new ResourceAlreadyExistsException(
                                                                        "Cashier with name '" + req.getName()
                                                                                        + "' already exists for this merchant");
                                                }

                                                Cashier cashier = new Cashier();
                                                cashier.setMerchantId(req.getMerchantId().longValue());
                                                cashier.setUserId(req.getUserId().longValue());
                                                cashier.setName(req.getName());
                                                cashier.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                                                cashier.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

                                                return cashierCommandRepository.persist(cashier)
                                                                .map(v -> {
                                                                        CashierResponse cashierResponse = CashierResponse
                                                                                        .from(cashier);
                                                                        logger.info("Cashier created successfully with id={}",
                                                                                        cashier.getCashierId());
                                                                        return ApiResponse.success(
                                                                                        "Cashier created successfully",
                                                                                        cashierResponse);
                                                                });
                                        });
                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<CashierResponse>> updateCashier(UpdateCashierRequest req) {
                Attributes attrs = Attributes.builder()
                                .put("cashier.id", req.getCashierId().toString())
                                .build();

                return runTraced("updateCashier", "update_cashier", attrs, () -> {
                        logger.info("Updating cashier id={}", req.getCashierId());

                        return cashierCommandRepository.findById(req.getCashierId().longValue())
                                        .onItem().ifNull()
                                        .failWith(() -> new ResourceNotFoundException("Cashier not found"))
                                        .chain(cashier -> {
                                                cashier.setName(req.getName());
                                                cashier.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

                                                return cashierCommandRepository.persist(cashier)
                                                                .chain(v -> {
                                                                        String cacheKey = "cashier:"
                                                                                        + req.getCashierId();
                                                                        return redisService.deleteReactive(cacheKey)
                                                                                        .map(deleted -> {
                                                                                                CashierResponse cashierResponse = CashierResponse
                                                                                                                .from(cashier);
                                                                                                logger.info("Cashier updated successfully id={}",
                                                                                                                cashier.getCashierId());
                                                                                                return ApiResponse
                                                                                                                .success("Cashier updated successfully",
                                                                                                                                cashierResponse);
                                                                                        });
                                                                });
                                        });
                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<CashierResponseDeleteAt>> trashedCashier(Long cashierId) {
                Attributes attrs = Attributes.builder()
                                .put("cashier.id", cashierId.toString())
                                .build();

                return runTraced("trashCashier", "trash_cashier", attrs, () -> {
                        logger.info("Trashing cashier id={}", cashierId);

                        return cashierCommandRepository.trashed(cashierId)
                                        .onItem().ifNull()
                                        .failWith(() -> new ResourceNotFoundException("Cashier not found"))
                                        .chain(cashier -> {
                                                String cacheKey = "cashier:" + cashierId;
                                                return redisService.deleteReactive(cacheKey)
                                                                .map(deleted -> {
                                                                        CashierResponseDeleteAt cashierResponseDeleteAt = CashierResponseDeleteAt
                                                                                        .from(cashier);
                                                                        logger.info("Cashier trashed successfully id={}",
                                                                                        cashierId);
                                                                        return ApiResponse.success(
                                                                                        "Cashier trashed successfully",
                                                                                        cashierResponseDeleteAt);
                                                                });
                                        });
                });
        }

        @WithTransaction
        @Override
        public Uni<ApiResponse<CashierResponseDeleteAt>> restoreCashier(Long cashierId) {
                Attributes attrs = Attributes.builder()
                                .put("cashier.id", cashierId.toString())
                                .build();

                return runTraced("restoreCashier", "restore_cashier", attrs, () -> {
                        logger.info("Restoring cashier id={}", cashierId);

                        return cashierCommandRepository.restore(cashierId)
                                        .onItem().ifNull()
                                        .failWith(() -> new ResourceNotFoundException(
                                                        "Cashier not found or not trashed"))
                                        .chain(cashier -> {
                                                String cacheKey = "cashier:" + cashierId;
                                                return redisService.deleteReactive(cacheKey)
                                                                .map(deleted -> {
                                                                        CashierResponseDeleteAt cashierResponse = CashierResponseDeleteAt
                                                                                        .from(cashier);
                                                                        logger.info("Cashier restored successfully id={}",
                                                                                        cashierId);
                                                                        return ApiResponse.success(
                                                                                        "Cashier restored successfully",
                                                                                        cashierResponse);
                                                                });
                                        });
                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Boolean>> deleteCashierPermanent(Long cashierId) {
                Attributes attrs = Attributes.builder()
                                .put("cashier.id", cashierId.toString())
                                .build();

                return runTraced("deleteCashierPermanent", "delete_cashier_permanent", attrs, () -> {
                        logger.info("Permanently deleting cashier id={}", cashierId);

                        return cashierCommandRepository.deletePermanent(cashierId)
                                        .onItem().ifNull()
                                        .failWith(() -> new ResourceNotFoundException(
                                                        "Cashier not found or not trashed"))
                                        .chain(cashier -> {
                                                String cacheKey = "cashier:" + cashierId;
                                                return redisService.deleteReactive(cacheKey)
                                                                .map(deleted -> {
                                                                        logger.info("Cashier permanently deleted id={}",
                                                                                        cashierId);
                                                                        return ApiResponse.success(
                                                                                        "Cashier permanently deleted",
                                                                                        true);
                                                                });
                                        });
                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Boolean>> restoreAllCashier() {
                return runTraced("restoreAllCashier", "restore_all_cashier", Attributes.empty(), () -> {
                        logger.info("Restoring ALL trashed cashiers");

                        return cashierCommandRepository.restoreAllDeleted()
                                        .map(success -> {
                                                if (!success) {
                                                        throw new ResourceNotFoundException("No trashed cashiers");
                                                }

                                                logger.info("All cashiers restored successfully");
                                                return ApiResponse.success("All cashiers restored successfully", true);
                                        });
                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Boolean>> deleteAllCashierPermanent() {
                return runTraced("deleteAllCashierPermanent", "delete_all_cashier_permanent", Attributes.empty(),
                                () -> {
                                        logger.info("Permanently deleting ALL trashed cashiers");

                                        return cashierCommandRepository.deleteAllDeleted()
                                                        .map(success -> {
                                                                if (!success) {
                                                                        throw new ResourceNotFoundException(
                                                                                        "No trashed cashiers");
                                                                }

                                                                logger.info("All cashiers permanently deleted");
                                                                return ApiResponse.success(
                                                                                "All cashiers permanently deleted",
                                                                                true);
                                                        });
                                });
        }

        private <T> Uni<T> runTraced(String operationName, String method, Attributes attributes,
                        Supplier<Uni<T>> supplier) {
                return tracingMetrics.traceAndMeasure(operationName, method, attributes, supplier);
        }
}