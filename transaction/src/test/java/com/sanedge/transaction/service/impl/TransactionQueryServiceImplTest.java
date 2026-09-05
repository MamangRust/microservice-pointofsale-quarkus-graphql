package com.sanedge.transaction.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.enums.PaymentStatus;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.transaction.domain.requests.FindAllTransactionByMerchantRequest;
import com.sanedge.transaction.domain.requests.FindAllTransactionRequest;
import com.sanedge.transaction.domain.response.TransactionResponse;
import com.sanedge.transaction.domain.response.TransactionResponseDeleteAt;
import com.sanedge.transaction.entity.Transaction;
import com.sanedge.transaction.repository.TransactionQueryRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class TransactionQueryServiceImplTest {

    @Mock
    private TransactionQueryRepository transactionQueryRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    private TransactionQueryServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new TransactionQueryServiceImpl(transactionQueryRepository, redisService, objectMapper, tracingMetrics);
        // Lenient stub to execute the supplier directly
        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(3);
            return supplier.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());
    }

    private Transaction createMockTransaction(Long id) {
        Transaction tx = new Transaction();
        tx.setTransactionId(id);
        tx.setOrderId(500L);
        tx.setMerchantId(10L);
        tx.setPaymentMethod("CREDIT");
        tx.setAmount(100000);
        tx.setStatus(PaymentStatus.SUCCESS);
        tx.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        tx.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return tx;
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (JsonProcessingException e) { throw new RuntimeException(e); }
    }

    private FindAllTransactionRequest findAllReq(int page, int size, String search) {
        FindAllTransactionRequest req = new FindAllTransactionRequest();
        req.setPage(page);
        req.setPageSize(size);
        req.setSearch(search == null ? "" : search);
        return req;
    }

    private FindAllTransactionByMerchantRequest findAllByMerchantReq(Long merchantId, int page, int size, String search) {
        FindAllTransactionByMerchantRequest req = new FindAllTransactionByMerchantRequest();
        req.setMerchantId(merchantId.intValue());
        req.setPage(page);
        req.setPageSize(size);
        req.setSearch(search == null ? "" : search);
        return req;
    }

    @Nested
    @DisplayName("findAllTransactions tests")
    class FindAllTransactionsTests {
        @Test void cacheMiss_fetchesFromDb() {
            FindAllTransactionRequest req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(transactionQueryRepository.findTransactions(any(FindAllTransactionRequest.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockTransaction(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong())).thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<TransactionResponse>> result = service.findAllTransactions(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }
        @Test void cacheHit_returnsCached() {
            FindAllTransactionRequest req = findAllReq(1, 10, "");
            ApiResponsePagination<List<TransactionResponse>> cached = new ApiResponsePagination<>(
                    "success", "Transactions retrieved successfully",
                    List.of(TransactionResponse.from(createMockTransaction(1L))), null);
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().item(toJson(cached)));

            ApiResponsePagination<List<TransactionResponse>> result = service.findAllTransactions(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("findByActive tests")
    class FindByActiveTests {
        @Test void cacheMiss_fetchesFromDb() {
            FindAllTransactionRequest req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(transactionQueryRepository.findActiveTransactions(any(FindAllTransactionRequest.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockTransaction(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong())).thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<TransactionResponseDeleteAt>> result = service.findByActive(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("findByTrashed tests")
    class FindByTrashedTests {
        @Test void cacheMiss_fetchesFromDb() {
            FindAllTransactionRequest req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(transactionQueryRepository.findTrashedTransactions(any(FindAllTransactionRequest.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockTransaction(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong())).thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<TransactionResponseDeleteAt>> result = service.findByTrashed(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("findByMerchant tests")
    class FindByMerchantTests {
        @Test void cacheMiss_fetchesFromDb() {
            FindAllTransactionByMerchantRequest req = findAllByMerchantReq(10L, 1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(transactionQueryRepository.findTransactionsByMerchant(any(FindAllTransactionByMerchantRequest.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockTransaction(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong())).thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<TransactionResponse>> result = service.findByMerchant(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("findById tests")
    class FindByIdTests {
        @Test void cacheMiss_fetchesFromDb() {
            Integer id = 1;
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(transactionQueryRepository.findByTransactionId(anyLong())).thenReturn(Uni.createFrom().item(createMockTransaction(id.longValue())));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong())).thenReturn(Uni.createFrom().voidItem());

            ApiResponse<TransactionResponse> result = service.findById(id).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
        }
        @Test void notFound_returnsError() {
            Integer id = 999;
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(transactionQueryRepository.findByTransactionId(anyLong())).thenReturn(Uni.createFrom().nullItem());

            ApiResponse<TransactionResponse> result = service.findById(id).await().indefinitely();
            assertThat(result.status()).isEqualTo("error");
            assertThat(result.message()).contains("Transaction not found");
        }
    }

    @Nested
    @DisplayName("findByOrderId tests")
    class FindByOrderIdTests {
        @Test void cacheMiss_fetchesFromDb() {
            Integer id = 500;
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(transactionQueryRepository.findByOrderId(anyLong())).thenReturn(Uni.createFrom().item(createMockTransaction(1L)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong())).thenReturn(Uni.createFrom().voidItem());

            ApiResponse<TransactionResponse> result = service.findByOrderId(id).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
        }
        @Test void notFound_returnsError() {
            Integer id = 999;
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(transactionQueryRepository.findByOrderId(anyLong())).thenReturn(Uni.createFrom().nullItem());

            ApiResponse<TransactionResponse> result = service.findByOrderId(id).await().indefinitely();
            assertThat(result.status()).isEqualTo("error");
            assertThat(result.message()).contains("Transaction not found for order");
        }
    }
}