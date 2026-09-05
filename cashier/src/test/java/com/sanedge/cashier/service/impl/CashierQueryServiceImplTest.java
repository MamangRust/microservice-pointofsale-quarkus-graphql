package com.sanedge.cashier.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.cashier.domain.requests.FindAllCashierMerchant;
import com.sanedge.cashier.domain.requests.FindAllCashiers;
import com.sanedge.cashier.domain.response.CashierResponse;
import com.sanedge.cashier.domain.response.CashierResponseDeleteAt;
import com.sanedge.cashier.entity.Cashier;
import com.sanedge.cashier.repository.CashierQueryRepository;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class CashierQueryServiceImplTest {

    @Mock
    private CashierQueryRepository cashierQueryRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    private CashierQueryServiceImpl cashierQueryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        cashierQueryService = new CashierQueryServiceImpl(
                cashierQueryRepository,
                redisService,
                objectMapper,
                tracingMetrics);

        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(3);
            return supplier.get();
        }).when(tracingMetrics)
                .traceAndMeasure(
                        anyString(),
                        anyString(),
                        any(Attributes.class),
                        any());
    }

    private Cashier createMockCashier(Long id, String name, Long merchantId, Long userId) {
        Cashier cashier = new Cashier();
        cashier.setCashierId(id);
        cashier.setName(name);
        cashier.setMerchantId(merchantId);
        cashier.setUserId(userId);
        cashier.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        cashier.setUpdatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        return cashier;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize in test helper", e);
        }
    }

    @Test
    void findById_cacheHit_returnsCached() {
        Cashier cashier = createMockCashier(1L, "Cashier1", 1L, 1L);
        CashierResponse cachedResponse = CashierResponse.from(cashier);
        String cachedJson = toJson(cachedResponse);

        when(redisService.getReactive("cashier:1")).thenReturn(Uni.createFrom().item(cachedJson));

        ApiResponse<CashierResponse> response = cashierQueryService.findById(1L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.data()).isNotNull();
        assertThat(response.data().getName()).isEqualTo("Cashier1");
        verify(cashierQueryRepository, never()).findByCashierId(anyLong());
    }

    @Test
    void findById_cacheMiss_fetchesFromDbAndCaches() {
        Cashier cashier = createMockCashier(2L, "Cashier2", 1L, 1L);

        when(redisService.getReactive("cashier:2")).thenReturn(Uni.createFrom().nullItem());
        when(cashierQueryRepository.findByCashierId(2L)).thenReturn(Uni.createFrom().item(cashier));
        when(redisService.setReactive(eq("cashier:2"), anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<CashierResponse> response = cashierQueryService.findById(2L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.data().getName()).isEqualTo("Cashier2");
        verify(cashierQueryRepository).findByCashierId(2L);
        verify(redisService).setReactive(eq("cashier:2"), anyString());
    }

    @Test
    void findById_notFound_throwsException() {
        when(redisService.getReactive("cashier:999")).thenReturn(Uni.createFrom().nullItem());
        when(cashierQueryRepository.findByCashierId(999L)).thenReturn(Uni.createFrom().nullItem());

        try {
            cashierQueryService.findById(999L).await().indefinitely();
            org.junit.jupiter.api.Assertions.fail("Expected exception");
        } catch (jakarta.ws.rs.NotFoundException e) {
            assertThat(e.getMessage()).contains("not found");
        }
    }

    @Test
    void findAll_cacheHit_returnsCachedList() {
        FindAllCashiers req = new FindAllCashiers();
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch(null);
        String cacheKey = "cashiers:all:1:10:null";

        CashierResponse res1 = CashierResponse.from(createMockCashier(1L, "C1", 1L, 1L));
        ApiResponsePagination<List<CashierResponse>> mockResponse = new ApiResponsePagination<>(
                "success", "Found", List.of(res1), null);
        String cachedJson = toJson(mockResponse);

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().item(cachedJson));

        ApiResponsePagination<List<CashierResponse>> response = cashierQueryService.findAll(req).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.data()).hasSize(1);
        verify(cashierQueryRepository, never()).findAllCashiers(any());
    }

    @Test
    void findAll_cacheMiss_fetchesFromDbAndCaches() {
        FindAllCashiers req = new FindAllCashiers();
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch(null);
        String cacheKey = "cashiers:all:1:10:null";

        Cashier c1 = createMockCashier(1L, "C1", 1L, 1L);
        PagedResult<Cashier> pagedResult = new PagedResult<>(List.of(c1), 1);

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().nullItem());
        when(cashierQueryRepository.findAllCashiers(req)).thenReturn(Uni.createFrom().item(pagedResult));
        when(redisService.setWithExpirationReactive(eq(cacheKey), anyString(), eq(300L)))
                .thenReturn(Uni.createFrom().voidItem());

        ApiResponsePagination<List<CashierResponse>> response = cashierQueryService.findAll(req).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.data()).hasSize(1);
        verify(cashierQueryRepository).findAllCashiers(req);
    }

    @Test
    void findActive_cacheHit_returnsCachedList() {
        FindAllCashiers req = new FindAllCashiers();
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch(null);
        String cacheKey = "cashiers:active:1:10:null";

        CashierResponseDeleteAt res1 = CashierResponseDeleteAt.from(createMockCashier(1L, "Active1", 1L, 1L));
        ApiResponsePagination<List<CashierResponseDeleteAt>> mockResponse = new ApiResponsePagination<>(
                "success", "Found", List.of(res1), null);
        String cachedJson = toJson(mockResponse);

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().item(cachedJson));

        ApiResponsePagination<List<CashierResponseDeleteAt>> response = cashierQueryService.findByActive(req).await().indefinitely();

        assertThat(response.data()).hasSize(1);
        verify(cashierQueryRepository, never()).findActiveCashiers(any());
    }

    @Test
    void findTrashed_cacheHit_returnsCachedList() {
        FindAllCashiers req = new FindAllCashiers();
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch(null);
        String cacheKey = "cashiers:trashed:1:10:null";

        CashierResponseDeleteAt res1 = CashierResponseDeleteAt.from(createMockCashier(2L, "Trashed1", 1L, 1L));
        ApiResponsePagination<List<CashierResponseDeleteAt>> mockResponse = new ApiResponsePagination<>(
                "success", "Found", List.of(res1), null);
        String cachedJson = toJson(mockResponse);

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().item(cachedJson));

        ApiResponsePagination<List<CashierResponseDeleteAt>> response = cashierQueryService.findByTrashed(req).await().indefinitely();

        assertThat(response.data()).hasSize(1);
        verify(cashierQueryRepository, never()).findTrashedCashiers(any());
    }

    @Test
    void findByMerchant_cacheHit_returnsCachedList() {
        FindAllCashierMerchant req = new FindAllCashierMerchant();
        req.setMerchantId(1);
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch(null);
        String cacheKey = "cashiers:merchant:1:1:10:null";

        CashierResponse res1 = CashierResponse.from(createMockCashier(1L, "MC1", 1L, 1L));
        ApiResponsePagination<List<CashierResponse>> mockResponse = new ApiResponsePagination<>(
                "success", "Found", List.of(res1), null);
        String cachedJson = toJson(mockResponse);

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().item(cachedJson));

        ApiResponsePagination<List<CashierResponse>> response = cashierQueryService.findByMerchant(req).await().indefinitely();

        assertThat(response.data()).hasSize(1);
        verify(cashierQueryRepository, never()).findByMerchants(any());
    }
}
