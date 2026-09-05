package com.sanedge.gateway.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.CashierDto;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class CashierServiceImplTest {

    @Mock
    TelemetryHelper telemetryHelper;

    @Mock
    pb.cashier.MutinyCashierServiceGrpc.MutinyCashierServiceStub cashierQueryService;

    @Mock
    pb.cashier.MutinyCashierCommandServiceGrpc.MutinyCashierCommandServiceStub cashierCommandService;

    @Mock
    pb.cashier.stats.MutinyCashierTotalSalesServiceGrpc.MutinyCashierTotalSalesServiceStub cashierTotalSalesServiceStub;

    @Mock
    pb.cashier.stats.MutinyCashierSalesServiceGrpc.MutinyCashierSalesServiceStub cashierSalesServiceStub;

    CashierServiceImpl cashierService;

    @BeforeEach
    void setUp() throws Exception {
        cashierService = new CashierServiceImpl();

        setField(cashierService, "telemetryHelper", telemetryHelper);
        setField(cashierService, "cashierQueryService", cashierQueryService);
        setField(cashierService, "cashierCommandService", cashierCommandService);
        setField(cashierService, "cashierTotalSalesServiceStub", cashierTotalSalesServiceStub);
        setField(cashierService, "cashierSalesServiceStub", cashierSalesServiceStub);

        lenient().when(telemetryHelper.traceAndMetric(anyString(), any(java.util.function.Supplier.class)))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<Uni<?>> supplier = inv.getArgument(1);
                    return supplier.get();
                });
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void listCashiers_returnsSuccess() {
        pb.cashier.Cashier.CashierResponse cashierProto = pb.cashier.Cashier.CashierResponse.newBuilder()
                .setId(1)
                .setName("Cashier One")
                .setMerchantId(100)
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.cashier.CashierQuery.ApiResponsePaginationCashier responseProto =
                pb.cashier.CashierQuery.ApiResponsePaginationCashier.newBuilder()
                        .addData(cashierProto)
                        .setStatus("success")
                        .setMessage("Cashiers found")
                        .build();

        when(cashierQueryService.findAll(any(pb.cashier.Cashier.FindAllCashierRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CashierDto.ApiResponsePaginationCashier result =
                cashierService.listCashiers(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).name()).isEqualTo("Cashier One");
    }

    @Test
    void getCashier_returnsSuccess() {
        pb.cashier.Cashier.CashierResponse cashierProto = pb.cashier.Cashier.CashierResponse.newBuilder()
                .setId(1)
                .setName("Cashier One")
                .build();

        pb.cashier.Cashier.ApiResponseCashier responseProto =
                pb.cashier.Cashier.ApiResponseCashier.newBuilder()
                        .setData(cashierProto)
                        .setStatus("success")
                        .setMessage("Cashier found")
                        .build();

        when(cashierQueryService.findById(any(pb.cashier.Cashier.FindByIdCashierRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CashierDto.ApiResponseCashier result = cashierService.getCashier(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().id()).isEqualTo(1);
    }

    @Test
    void createCashier_returnsSuccess() {
        pb.cashier.Cashier.CashierResponse cashierProto = pb.cashier.Cashier.CashierResponse.newBuilder()
                .setId(1)
                .setName("New Cashier")
                .setMerchantId(100)
                .build();

        pb.cashier.Cashier.ApiResponseCashier responseProto =
                pb.cashier.Cashier.ApiResponseCashier.newBuilder()
                        .setData(cashierProto)
                        .setStatus("success")
                        .setMessage("Cashier created")
                        .build();

        when(cashierCommandService.createCashier(any(pb.cashier.Cashier.CreateCashierRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CashierDto.CreateCashierRequest request = new CashierDto.CreateCashierRequest(100, 10, "New Cashier");
        CashierDto.ApiResponseCashier result = cashierService.createCashier(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().name()).isEqualTo("New Cashier");
    }

    @Test
    void updateCashier_returnsSuccess() {
        pb.cashier.Cashier.CashierResponse cashierProto = pb.cashier.Cashier.CashierResponse.newBuilder()
                .setId(1)
                .setName("Updated Cashier")
                .build();

        pb.cashier.Cashier.ApiResponseCashier responseProto =
                pb.cashier.Cashier.ApiResponseCashier.newBuilder()
                        .setData(cashierProto)
                        .setStatus("success")
                        .setMessage("Cashier updated")
                        .build();

        when(cashierCommandService.updateCashier(any(pb.cashier.Cashier.UpdateCashierRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CashierDto.UpdateCashierRequest request = new CashierDto.UpdateCashierRequest("Updated Cashier");
        CashierDto.ApiResponseCashier result = cashierService.updateCashier(1, request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().name()).isEqualTo("Updated Cashier");
    }

    @Test
    void deleteCashier_returnsSuccess() {
        pb.cashier.Cashier.CashierResponseDeleteAt cashierProto =
                pb.cashier.Cashier.CashierResponseDeleteAt.newBuilder()
                        .setId(1)
                        .setName("Cashier One")
                        .setDeletedAt(com.google.protobuf.StringValue.of("2024-06-01T00:00:00Z"))
                        .build();

        pb.cashier.Cashier.ApiResponseCashierDeleteAt responseProto =
                pb.cashier.Cashier.ApiResponseCashierDeleteAt.newBuilder()
                        .setData(cashierProto)
                        .setStatus("success")
                        .setMessage("Cashier trashed")
                        .build();

        when(cashierCommandService.trashedCashier(any(pb.cashier.Cashier.FindByIdCashierRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CashierDto.ApiResponseCashierDeleteAt result = cashierService.deleteCashier(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().id()).isEqualTo(1);
    }

    @Test
    void restoreCashier_returnsSuccess() {
        pb.cashier.Cashier.CashierResponseDeleteAt cashierProto =
                pb.cashier.Cashier.CashierResponseDeleteAt.newBuilder()
                        .setId(1)
                        .setName("Cashier One")
                        .build();

        pb.cashier.Cashier.ApiResponseCashierDeleteAt responseProto =
                pb.cashier.Cashier.ApiResponseCashierDeleteAt.newBuilder()
                        .setData(cashierProto)
                        .setStatus("success")
                        .setMessage("Cashier restored")
                        .build();

        when(cashierCommandService.restoreCashier(any(pb.cashier.Cashier.FindByIdCashierRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CashierDto.ApiResponseCashierDeleteAt result = cashierService.restoreCashier(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Cashier restored");
    }

    @Test
    void deleteCashierPermanent_returnsSuccess() {
        pb.cashier.CashierCommand.ApiResponseCashierDelete responseProto =
                pb.cashier.CashierCommand.ApiResponseCashierDelete.newBuilder()
                        .setStatus("success")
                        .setMessage("Cashier permanently deleted")
                        .build();

        when(cashierCommandService.deleteCashierPermanent(any(pb.cashier.Cashier.FindByIdCashierRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CashierDto.ApiResponseCashierDelete result = cashierService.deleteCashierPermanent(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Cashier permanently deleted");
    }

    @Test
    void restoreAllCashier_returnsSuccess() {
        pb.cashier.CashierCommand.ApiResponseCashierAll responseProto =
                pb.cashier.CashierCommand.ApiResponseCashierAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All cashiers restored")
                        .build();

        when(cashierCommandService.restoreAllCashier(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CashierDto.ApiResponseCashierAll result = cashierService.restoreAllCashier().await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("All cashiers restored");
    }

    @Test
    void getMonthlyTotalSales_returnsSuccess() {
        pb.cashier.Cashier.CashierResponseMonthTotalSales salesProto =
                pb.cashier.Cashier.CashierResponseMonthTotalSales.newBuilder()
                        .setMonth("6")
                        .setYear("2024")
                        .setTotalSales(15000)
                        .build();

        pb.cashier.stats.CashierTotalSales.ApiResponseCashierMonthlyTotalSales responseProto =
                pb.cashier.stats.CashierTotalSales.ApiResponseCashierMonthlyTotalSales.newBuilder()
                        .addData(salesProto)
                        .setStatus("success")
                        .setMessage("Monthly total sales retrieved")
                        .build();

        when(cashierTotalSalesServiceStub.findMonthlyTotalSales(any(pb.cashier.Cashier.FindYearMonthTotalSales.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CashierDto.ApiResponseCashierMonthlyTotalSales result =
                cashierService.getMonthlyTotalSales(2024, 6).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().get(0).totalSales()).isEqualTo(15000);
    }

    @Test
    void getMonthSales_returnsSuccess() {
        pb.cashier.Cashier.CashierResponseMonthSales salesProto =
                pb.cashier.Cashier.CashierResponseMonthSales.newBuilder()
                        .setMonth("6")
                        .setTotalSales(8000)
                        .build();

        pb.cashier.Cashier.ApiResponseCashierMonthSales responseProto =
                pb.cashier.Cashier.ApiResponseCashierMonthSales.newBuilder()
                        .addData(salesProto)
                        .setStatus("success")
                        .setMessage("Month sales retrieved")
                        .build();

        when(cashierSalesServiceStub.findMonthSales(any(pb.cashier.Cashier.FindYearCashier.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CashierDto.ApiResponseCashierMonthSales result =
                cashierService.getMonthSales(2024).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).totalSales()).isEqualTo(8000);
    }
}
