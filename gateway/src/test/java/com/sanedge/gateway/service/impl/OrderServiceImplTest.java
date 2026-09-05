package com.sanedge.gateway.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.OrderDto;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    TelemetryHelper telemetryHelper;

    @Mock
    pb.order.MutinyOrderQueryServiceGrpc.MutinyOrderQueryServiceStub orderQueryService;

    @Mock
    pb.order.MutinyOrderCommandServiceGrpc.MutinyOrderCommandServiceStub orderCommandService;

    @Mock
    pb.order.stats.MutinyOrderTotalRevenueServiceGrpc.MutinyOrderTotalRevenueServiceStub orderTotalRevenueServiceStub;

    @Mock
    pb.order.stats.MutinyOrderSoldoutServiceGrpc.MutinyOrderSoldoutServiceStub orderRevenueServiceStub;

    OrderServiceImpl orderService;

    @BeforeEach
    void setUp() throws Exception {
        orderService = new OrderServiceImpl();

        setField(orderService, "telemetryHelper", telemetryHelper);
        setField(orderService, "orderQueryService", orderQueryService);
        setField(orderService, "orderCommandService", orderCommandService);
        setField(orderService, "orderTotalRevenueServiceStub", orderTotalRevenueServiceStub);
        setField(orderService, "orderRevenueServiceStub", orderRevenueServiceStub);

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
    void listOrders_returnsSuccess() {
        pb.order.Order.OrderResponse orderProto = pb.order.Order.OrderResponse.newBuilder()
                .setId(1)
                .setMerchantId(100)
                .setCashierId(10)
                .build();

        pb.order.OrderQuery.ApiResponsePaginationOrder responseProto =
                pb.order.OrderQuery.ApiResponsePaginationOrder.newBuilder()
                        .addData(orderProto)
                        .setStatus("success")
                        .setMessage("Orders found")
                        .build();

        when(orderQueryService.findAll(any(pb.order.Order.FindAllOrderRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderDto.ApiResponsePaginationOrder result =
                orderService.listOrders(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).id()).isEqualTo(1);
    }

    @Test
    void getActiveOrders_returnsSuccess() {
        pb.order.Order.OrderResponseDeleteAt orderProto = pb.order.Order.OrderResponseDeleteAt.newBuilder()
                .setId(1)
                .setMerchantId(100)
                .build();

        pb.order.OrderQuery.ApiResponsePaginationOrderDeleteAt responseProto =
                pb.order.OrderQuery.ApiResponsePaginationOrderDeleteAt.newBuilder()
                        .addData(orderProto)
                        .setStatus("success")
                        .setMessage("Active orders")
                        .build();

        when(orderQueryService.findByActive(any(pb.order.Order.FindAllOrderRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderDto.ApiResponsePaginationOrderDeleteAt result =
                orderService.getActiveOrders(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
    }

    @Test
    void getTrashedOrders_returnsSuccess() {
        pb.order.Order.OrderResponseDeleteAt orderProto = pb.order.Order.OrderResponseDeleteAt.newBuilder()
                .setId(2)
                .build();

        pb.order.OrderQuery.ApiResponsePaginationOrderDeleteAt responseProto =
                pb.order.OrderQuery.ApiResponsePaginationOrderDeleteAt.newBuilder()
                        .addData(orderProto)
                        .setStatus("success")
                        .setMessage("Trashed orders")
                        .build();

        when(orderQueryService.findByTrashed(any(pb.order.Order.FindAllOrderRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderDto.ApiResponsePaginationOrderDeleteAt result =
                orderService.getTrashedOrders(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getOrder_returnsSuccess() {
        pb.order.Order.OrderResponse orderProto = pb.order.Order.OrderResponse.newBuilder()
                .setId(1)
                .setMerchantId(100)
                .build();

        pb.order.Order.ApiResponseOrder responseProto =
                pb.order.Order.ApiResponseOrder.newBuilder()
                        .setData(orderProto)
                        .setStatus("success")
                        .setMessage("Order found")
                        .build();

        when(orderQueryService.findById(any(pb.order.Order.FindByIdOrderRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderDto.ApiResponseOrder result = orderService.getOrder(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().id()).isEqualTo(1);
    }

    @Test
    void getOrdersByMerchant_returnsSuccess() {
        pb.order.Order.OrderResponse orderProto = pb.order.Order.OrderResponse.newBuilder()
                .setId(1)
                .setMerchantId(200)
                .build();

        pb.order.OrderQuery.ApiResponsePaginationOrder responseProto =
                pb.order.OrderQuery.ApiResponsePaginationOrder.newBuilder()
                        .addData(orderProto)
                        .setStatus("success")
                        .setMessage("Orders by merchant")
                        .build();

        when(orderQueryService.findByMerchant(any(pb.order.Order.FindAllOrderMerchantRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderDto.ApiResponsePaginationOrder result =
                orderService.getOrdersByMerchant(200, 1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().get(0).merchantId()).isEqualTo(200);
    }

    @Test
    void createOrder_returnsSuccess() {
        pb.order.Order.OrderResponse orderProto = pb.order.Order.OrderResponse.newBuilder()
                .setId(1)
                .setMerchantId(100)
                .setCashierId(10)
                .build();

        pb.order.Order.ApiResponseOrder responseProto =
                pb.order.Order.ApiResponseOrder.newBuilder()
                        .setData(orderProto)
                        .setStatus("success")
                        .setMessage("Order created")
                        .build();

        when(orderCommandService.create(any(pb.order.Order.CreateOrderRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderDto.CreateOrderItemRequest item = new OrderDto.CreateOrderItemRequest(1, 2);
        OrderDto.CreateOrderRequest request = new OrderDto.CreateOrderRequest(100, 10, List.of(item));
        OrderDto.ApiResponseOrder result = orderService.createOrder(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Order created");
    }

    @Test
    void updateOrder_returnsSuccess() {
        pb.order.Order.OrderResponse orderProto = pb.order.Order.OrderResponse.newBuilder()
                .setId(1)
                .setMerchantId(100)
                .build();

        pb.order.Order.ApiResponseOrder responseProto =
                pb.order.Order.ApiResponseOrder.newBuilder()
                        .setData(orderProto)
                        .setStatus("success")
                        .setMessage("Order updated")
                        .build();

        when(orderCommandService.update(any(pb.order.Order.UpdateOrderRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderDto.UpdateOrderItemRequest item = new OrderDto.UpdateOrderItemRequest(101, 1, 3);
        OrderDto.UpdateOrderRequest request = new OrderDto.UpdateOrderRequest(10, List.of(item));
        OrderDto.ApiResponseOrder result = orderService.updateOrder(1, request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Order updated");
    }

    @Test
    void deleteOrder_returnsSuccess() {
        pb.order.Order.OrderResponseDeleteAt orderProto = pb.order.Order.OrderResponseDeleteAt.newBuilder()
                .setId(1)
                .setDeletedAt(com.google.protobuf.StringValue.of("2024-06-01T00:00:00Z"))
                .build();

        pb.order.Order.ApiResponseOrderDeleteAt responseProto =
                pb.order.Order.ApiResponseOrderDeleteAt.newBuilder()
                        .setData(orderProto)
                        .setStatus("success")
                        .setMessage("Order trashed")
                        .build();

        when(orderCommandService.trashedOrder(any(pb.order.Order.FindByIdOrderRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderDto.ApiResponseOrderDeleteAt result = orderService.deleteOrder(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Order trashed");
    }

    @Test
    void restoreOrder_returnsSuccess() {
        pb.order.Order.OrderResponseDeleteAt orderProto = pb.order.Order.OrderResponseDeleteAt.newBuilder()
                .setId(1)
                .build();

        pb.order.Order.ApiResponseOrderDeleteAt responseProto =
                pb.order.Order.ApiResponseOrderDeleteAt.newBuilder()
                        .setData(orderProto)
                        .setStatus("success")
                        .setMessage("Order restored")
                        .build();

        when(orderCommandService.restoreOrder(any(pb.order.Order.FindByIdOrderRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderDto.ApiResponseOrderDeleteAt result = orderService.restoreOrder(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Order restored");
    }

    @Test
    void deleteOrderPermanent_returnsSuccess() {
        pb.order.Order.ApiResponseOrderDelete responseProto =
                pb.order.Order.ApiResponseOrderDelete.newBuilder()
                        .setStatus("success")
                        .setMessage("Permanently deleted")
                        .build();

        when(orderCommandService.deleteOrderPermanent(any(pb.order.Order.FindByIdOrderRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderDto.ApiResponseOrderDelete result = orderService.deleteOrderPermanent(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Permanently deleted");
    }

    @Test
    void restoreAllOrders_returnsSuccess() {
        pb.order.Order.ApiResponseOrderAll responseProto =
                pb.order.Order.ApiResponseOrderAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All orders restored")
                        .build();

        when(orderCommandService.restoreAllOrder(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderDto.ApiResponseOrderAll result = orderService.restoreAllOrders().await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("All orders restored");
    }

    @Test
    void deleteAllOrders_returnsSuccess() {
        pb.order.Order.ApiResponseOrderAll responseProto =
                pb.order.Order.ApiResponseOrderAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All orders permanently deleted")
                        .build();

        when(orderCommandService.deleteAllOrderPermanent(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderDto.ApiResponseOrderAll result = orderService.deleteAllOrders().await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("All orders permanently deleted");
    }

    @Test
    void getMonthlyTotalRevenues_returnsSuccess() {
        pb.order.Order.OrderMonthlyTotalRevenueResponse dataProto =
                pb.order.Order.OrderMonthlyTotalRevenueResponse.newBuilder()
                        .setMonth("6")
                        .setYear("2024")
                        .setTotalRevenue(15000)
                        .build();

        pb.order.Order.ApiResponseOrderMonthlyTotalRevenue responseProto =
                pb.order.Order.ApiResponseOrderMonthlyTotalRevenue.newBuilder()
                        .addData(dataProto)
                        .setStatus("success")
                        .setMessage("Monthly total revenue")
                        .build();

        when(orderTotalRevenueServiceStub.findMonthlyTotalRevenue(any(pb.order.Order.FindYearMonthTotalRevenue.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderDto.ApiResponseOrderMonthlyTotalRevenue result =
                orderService.getMonthlyTotalRevenues(2024, 6).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().get(0).totalRevenue()).isEqualTo(15000);
    }

    @Test
    void getYearlyTotalRevenues_returnsSuccess() {
        pb.order.Order.OrderYearlyTotalRevenueResponse dataProto =
                pb.order.Order.OrderYearlyTotalRevenueResponse.newBuilder()
                        .setYear("2024")
                        .setTotalRevenue(100000)
                        .build();

        pb.order.Order.ApiResponseOrderYearlyTotalRevenue responseProto =
                pb.order.Order.ApiResponseOrderYearlyTotalRevenue.newBuilder()
                        .addData(dataProto)
                        .setStatus("success")
                        .setMessage("Yearly total revenue")
                        .build();

        when(orderTotalRevenueServiceStub.findYearlyTotalRevenue(any(pb.order.Order.FindYearTotalRevenue.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderDto.ApiResponseOrderYearlyTotalRevenue result =
                orderService.getYearlyTotalRevenues(2024).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().get(0).totalRevenue()).isEqualTo(100000);
    }

    @Test
    void getMonthlyRevenues_returnsSuccess() {
        pb.order.Order.OrderMonthlyResponse dataProto =
                pb.order.Order.OrderMonthlyResponse.newBuilder()
                        .setMonth("6")
                        .setTotalRevenue(5000)
                        .build();

        pb.order.Order.ApiResponseOrderMonthly responseProto =
                pb.order.Order.ApiResponseOrderMonthly.newBuilder()
                        .addData(dataProto)
                        .setStatus("success")
                        .setMessage("Monthly revenues")
                        .build();

        when(orderRevenueServiceStub.findMonthlyRevenue(any(pb.order.Order.FindYearOrder.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderDto.ApiResponseOrderMonthly result =
                orderService.getMonthlyRevenues(2024).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).totalRevenue()).isEqualTo(5000);
    }

    @Test
    void getYearlyRevenues_returnsSuccess() {
        pb.order.Order.OrderYearlyResponse dataProto =
                pb.order.Order.OrderYearlyResponse.newBuilder()
                        .setYear("2024")
                        .setTotalRevenue(60000)
                        .build();

        pb.order.Order.ApiResponseOrderYearly responseProto =
                pb.order.Order.ApiResponseOrderYearly.newBuilder()
                        .addData(dataProto)
                        .setStatus("success")
                        .setMessage("Yearly revenues")
                        .build();

        when(orderRevenueServiceStub.findYearlyRevenue(any(pb.order.Order.FindYearOrder.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderDto.ApiResponseOrderYearly result =
                orderService.getYearlyRevenues(2024).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().get(0).totalRevenue()).isEqualTo(60000);
    }

    @Test
    void getMonthlyTotalRevenuesByMerchant_returnsSuccess() {
        pb.order.Order.OrderMonthlyTotalRevenueResponse dataProto =
                pb.order.Order.OrderMonthlyTotalRevenueResponse.newBuilder()
                        .setYear("2024")
                        .setMonth("6")
                        .setTotalRevenue(3000)
                        .build();

        pb.order.Order.ApiResponseOrderMonthlyTotalRevenue responseProto =
                pb.order.Order.ApiResponseOrderMonthlyTotalRevenue.newBuilder()
                        .addData(dataProto)
                        .setStatus("success")
                        .setMessage("Merchant monthly revenue")
                        .build();

        when(orderTotalRevenueServiceStub.findMonthlyTotalRevenueByMerchant(any(pb.order.Order.FindYearMonthTotalRevenueByMerchant.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderDto.ApiResponseOrderMonthlyTotalRevenue result =
                orderService.getMonthlyTotalRevenuesByMerchant(200, 2024, 6).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getYearlyTotalRevenuesByMerchant_returnsSuccess() {
        pb.order.Order.OrderYearlyTotalRevenueResponse dataProto =
                pb.order.Order.OrderYearlyTotalRevenueResponse.newBuilder()
                        .setYear("2024")
                        .setTotalRevenue(25000)
                        .build();

        pb.order.Order.ApiResponseOrderYearlyTotalRevenue responseProto =
                pb.order.Order.ApiResponseOrderYearlyTotalRevenue.newBuilder()
                        .addData(dataProto)
                        .setStatus("success")
                        .setMessage("Merchant yearly revenue")
                        .build();

        when(orderTotalRevenueServiceStub.findYearlyTotalRevenueByMerchant(any(pb.order.Order.FindYearTotalRevenueByMerchant.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderDto.ApiResponseOrderYearlyTotalRevenue result =
                orderService.getYearlyTotalRevenuesByMerchant(200, 2024).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getMonthlyRevenuesByMerchant_returnsSuccess() {
        pb.order.Order.OrderMonthlyResponse dataProto =
                pb.order.Order.OrderMonthlyResponse.newBuilder()
                        .setMonth("6")
                        .setTotalRevenue(1200)
                        .build();

        pb.order.Order.ApiResponseOrderMonthly responseProto =
                pb.order.Order.ApiResponseOrderMonthly.newBuilder()
                        .addData(dataProto)
                        .setStatus("success")
                        .setMessage("Monthly revenue by merchant")
                        .build();

        when(orderRevenueServiceStub.findMonthlyRevenueByMerchant(any(pb.order.Order.FindYearOrderByMerchant.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderDto.ApiResponseOrderMonthly result =
                orderService.getMonthlyRevenuesByMerchant(200, 2024).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getYearlyRevenuesByMerchant_returnsSuccess() {
        pb.order.Order.OrderYearlyResponse dataProto =
                pb.order.Order.OrderYearlyResponse.newBuilder()
                        .setYear("2024")
                        .setTotalRevenue(18000)
                        .build();

        pb.order.Order.ApiResponseOrderYearly responseProto =
                pb.order.Order.ApiResponseOrderYearly.newBuilder()
                        .addData(dataProto)
                        .setStatus("success")
                        .setMessage("Yearly revenue by merchant")
                        .build();

        when(orderRevenueServiceStub.findYearlyRevenueByMerchant(any(pb.order.Order.FindYearOrderByMerchant.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderDto.ApiResponseOrderYearly result =
                orderService.getYearlyRevenuesByMerchant(200, 2024).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
    }
}
