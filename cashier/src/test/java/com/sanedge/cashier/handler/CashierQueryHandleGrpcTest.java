package com.sanedge.cashier.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.cashier.domain.response.CashierResponse;
import com.sanedge.cashier.domain.response.CashierResponseDeleteAt;
import com.sanedge.cashier.service.CashierQueryService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PaginationMeta;

import io.smallrye.mutiny.Uni;
import pb.cashier.Cashier.ApiResponseCashier;
import pb.cashier.Cashier.FindAllCashierRequest;
import pb.cashier.Cashier.FindByIdCashierRequest;
import pb.cashier.Cashier.FindByMerchantCashierRequest;
import pb.cashier.CashierQuery.ApiResponsePaginationCashier;
import pb.cashier.CashierQuery.ApiResponsePaginationCashierDeleteAt;

@ExtendWith(MockitoExtension.class)
class CashierQueryHandleGrpcTest {

    @Mock
    private CashierQueryService cashierQueryService;

    private CashierQueryGrpcHandler queryHandler;

    @BeforeEach
    void setUp() {
        queryHandler = new CashierQueryGrpcHandler();
        injectField(queryHandler, "cashierQueryService", cashierQueryService);
    }

    @Test
    void findById_success_mapsToProtoCorrectly() {
        FindByIdCashierRequest request = FindByIdCashierRequest.newBuilder().setId(1).build();
        CashierResponse domainRes = createDomainCashierResponse();

        when(cashierQueryService.findById(1L))
                .thenReturn(Uni.createFrom().item(ApiResponse.success("Found", domainRes)));

        ApiResponseCashier response = queryHandler.findById(request).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getId()).isEqualTo(1);
    }

    @Test
    void findById_whenDataIsNull_doesNotSetData() {
        FindByIdCashierRequest request = FindByIdCashierRequest.newBuilder().setId(99).build();

        when(cashierQueryService.findById(99L))
                .thenReturn(Uni.createFrom().item(ApiResponse.success("Not found", null)));

        ApiResponseCashier response = queryHandler.findById(request).await().indefinitely();

        assertThat(response.hasData()).isFalse();
    }

    @Test
    void findAll_success_mapsListAndPaginationCorrectly() {
        FindAllCashierRequest request = FindAllCashierRequest.newBuilder().setPage(1).setPageSize(10).setSearch("").build();

        List<CashierResponse> mockList = List.of(createDomainCashierResponse());
        PaginationMeta meta = new PaginationMeta(1, 10, 1, 1);
        ApiResponsePagination<List<CashierResponse>> serviceRes = new ApiResponsePagination<>("success", "Found",
                mockList, meta);

        when(cashierQueryService.findAll(any(com.sanedge.cashier.domain.requests.FindAllCashiers.class)))
                .thenReturn(Uni.createFrom().item(serviceRes));

        ApiResponsePaginationCashier response = queryHandler.findAll(request).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
    }

    @Test
    void findByActive_success_mapsDeletedAt() {
        FindAllCashierRequest request = FindAllCashierRequest.newBuilder().setPage(1).setPageSize(10).build();
        List<CashierResponseDeleteAt> mockList = List.of(createDomainCashierResponseDeleteAt());
        PaginationMeta meta = new PaginationMeta(1, 10, 1, 1);
        ApiResponsePagination<List<CashierResponseDeleteAt>> serviceRes = new ApiResponsePagination<>("success", "Found",
                mockList, meta);

        when(cashierQueryService.findByActive(any(com.sanedge.cashier.domain.requests.FindAllCashiers.class)))
                .thenReturn(Uni.createFrom().item(serviceRes));

        ApiResponsePaginationCashierDeleteAt response = queryHandler.findByActive(request).await().indefinitely();

        assertThat(response.getDataCount()).isEqualTo(1);
    }

    @Test
    void findByTrashed_success_mapsCorrectly() {
        FindAllCashierRequest request = FindAllCashierRequest.newBuilder().setPage(1).setPageSize(10).build();
        List<CashierResponseDeleteAt> mockList = List.of(createDomainCashierResponseDeleteAt());
        PaginationMeta meta = new PaginationMeta(1, 10, 1, 1);
        ApiResponsePagination<List<CashierResponseDeleteAt>> serviceRes = new ApiResponsePagination<>("success", "Found",
                mockList, meta);

        when(cashierQueryService.findByTrashed(any(com.sanedge.cashier.domain.requests.FindAllCashiers.class)))
                .thenReturn(Uni.createFrom().item(serviceRes));

        ApiResponsePaginationCashierDeleteAt response = queryHandler.findByTrashed(request).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo("success");
    }

    @Test
    void findByMerchant_success_mapsCorrectly() {
        FindByMerchantCashierRequest request = FindByMerchantCashierRequest.newBuilder()
                .setMerchantId(1).setPage(1).setPageSize(10).build();
        List<CashierResponse> mockList = List.of(createDomainCashierResponse());
        PaginationMeta meta = new PaginationMeta(1, 10, 1, 1);
        ApiResponsePagination<List<CashierResponse>> serviceRes = new ApiResponsePagination<>("success", "Found",
                mockList, meta);

        when(cashierQueryService.findByMerchant(any(com.sanedge.cashier.domain.requests.FindAllCashierMerchant.class)))
                .thenReturn(Uni.createFrom().item(serviceRes));

        ApiResponsePaginationCashier response = queryHandler.findByMerchant(request).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject " + fieldName, e);
        }
    }

    private CashierResponse createDomainCashierResponse() {
        return CashierResponse.builder()
                .id(1)
                .merchantId(1)
                .name("Cashier1")
                .createdAt(LocalDateTime.now().toString())
                .updatedAt(LocalDateTime.now().toString())
                .build();
    }

    private CashierResponseDeleteAt createDomainCashierResponseDeleteAt() {
        return CashierResponseDeleteAt.builder()
                .id(1)
                .merchantId(1)
                .name("Cashier1")
                .createdAt(LocalDateTime.now().toString())
                .updatedAt(LocalDateTime.now().toString())
                .deletedAt(LocalDateTime.of(2023, 10, 10, 10, 10).toString())
                .build();
    }
}
