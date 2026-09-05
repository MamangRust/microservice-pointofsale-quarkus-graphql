package com.sanedge.gateway.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.OrderItemDto;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceImplTest {

    @Mock
    TelemetryHelper telemetryHelper;

    @Mock
    pb.order_item.MutinyOrderItemServiceGrpc.MutinyOrderItemServiceStub orderItemQueryService;

    @Mock
    pb.order_item.MutinyOrderItemCommandServiceGrpc.MutinyOrderItemCommandServiceStub orderItemCommandService;

    OrderItemServiceImpl orderItemService;

    @BeforeEach
    void setUp() throws Exception {
        orderItemService = new OrderItemServiceImpl();

        setField(orderItemService, "telemetryHelper", telemetryHelper);
        setField(orderItemService, "orderItemQueryService", orderItemQueryService);
        setField(orderItemService, "orderItemCommandService", orderItemCommandService);

        lenient().when(telemetryHelper.traceAndMetric(anyString(), any(Uni.class)))
                .thenAnswer(inv -> {
                    Uni<?> uni = inv.getArgument(1);
                    return uni;
                });
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void listOrderItems_returnsSuccess() {
        pb.order_item.OrderItem.OrderItemResponse orderItemProto =
                pb.order_item.OrderItem.OrderItemResponse.newBuilder()
                        .setId(1)
                        .setOrderId(100)
                        .setProductId(200)
                        .setQuantity(2)
                        .setPrice(2550)
                        .build();

        pb.order_item.OrderItemQuery.ApiResponsePaginationOrderItem responseProto =
                pb.order_item.OrderItemQuery.ApiResponsePaginationOrderItem.newBuilder()
                        .addData(orderItemProto)
                        .setStatus("success")
                        .setMessage("Order items found")
                        .build();

        when(orderItemQueryService.findAll(any(pb.order_item.OrderItem.FindAllOrderItemRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderItemDto.ApiResponsePaginationOrderItem result =
                orderItemService.listOrderItems(1, 10, "product").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).id()).isEqualTo(1);
        assertThat(result.data().get(0).orderId()).isEqualTo(100);
        assertThat(result.data().get(0).productId()).isEqualTo(200);
        assertThat(result.data().get(0).quantity()).isEqualTo(2);
        assertThat(result.data().get(0).price()).isEqualTo(2550);
    }

    @Test
    void getActiveOrderItems_returnsSuccess() {
        pb.order_item.OrderItem.OrderItemResponseDeleteAt orderItemProto =
                pb.order_item.OrderItem.OrderItemResponseDeleteAt.newBuilder()
                        .setId(2)
                        .setOrderId(101)
                        .setProductId(201)
                        .setQuantity(3)
                        .setPrice(3000)
                        .build();

        pb.order_item.OrderItemQuery.ApiResponsePaginationOrderItemDeleteAt responseProto =
                pb.order_item.OrderItemQuery.ApiResponsePaginationOrderItemDeleteAt.newBuilder()
                        .addData(orderItemProto)
                        .setStatus("success")
                        .setMessage("Active order items")
                        .build();

        when(orderItemQueryService.findByActive(any(pb.order_item.OrderItem.FindAllOrderItemRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderItemDto.ApiResponsePaginationOrderItemDeleteAt result =
                orderItemService.getActiveOrderItems(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).id()).isEqualTo(2);
    }

    @Test
    void getTrashedOrderItems_returnsSuccess() {
        pb.order_item.OrderItem.OrderItemResponseDeleteAt orderItemProto =
                pb.order_item.OrderItem.OrderItemResponseDeleteAt.newBuilder()
                        .setId(3)
                        .setDeletedAt(com.google.protobuf.StringValue.of("2024-07-01T00:00:00Z"))
                        .build();

        pb.order_item.OrderItemQuery.ApiResponsePaginationOrderItemDeleteAt responseProto =
                pb.order_item.OrderItemQuery.ApiResponsePaginationOrderItemDeleteAt.newBuilder()
                        .addData(orderItemProto)
                        .setStatus("success")
                        .setMessage("Trashed order items")
                        .build();

        when(orderItemQueryService.findByTrashed(any(pb.order_item.OrderItem.FindAllOrderItemRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderItemDto.ApiResponsePaginationOrderItemDeleteAt result =
                orderItemService.getTrashedOrderItems(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).deletedAt()).isEqualTo("2024-07-01T00:00:00Z");
    }

    @Test
    void getOrderItemsByOrder_returnsSuccess() {
        pb.order_item.OrderItem.OrderItemResponse orderItemProto =
                pb.order_item.OrderItem.OrderItemResponse.newBuilder()
                        .setId(4)
                        .setOrderId(102)
                        .setProductId(202)
                        .setQuantity(1)
                        .setPrice(1575)
                        .build();

        pb.order_item.OrderItem.ApiResponsesOrderItem responseProto =
                pb.order_item.OrderItem.ApiResponsesOrderItem.newBuilder()
                        .addData(orderItemProto)
                        .setStatus("success")
                        .setMessage("Order items for order")
                        .build();

        when(orderItemQueryService.findOrderItemByOrder(any(pb.order_item.OrderItem.FindByIdOrderItemRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderItemDto.ApiResponsesOrderItem result =
                orderItemService.getOrderItemsByOrder(102).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).orderId()).isEqualTo(102);
    }

    @Test
    void createOrderItem_returnsSuccess() {
        pb.order_item.OrderItem.OrderItemResponse orderItemProto =
                pb.order_item.OrderItem.OrderItemResponse.newBuilder()
                        .setId(5)
                        .setOrderId(103)
                        .setProductId(203)
                        .setQuantity(4)
                        .setPrice(4500)
                        .build();

        pb.order_item.OrderItem.ApiResponseOrderItem responseProto =
                pb.order_item.OrderItem.ApiResponseOrderItem.newBuilder()
                        .setData(orderItemProto)
                        .setStatus("success")
                        .setMessage("Order item created")
                        .build();

        when(orderItemCommandService.createOrderItem(any(pb.order_item.OrderItemCommand.CreateOrderItemRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderItemDto.CreateOrderItemRequest request =
                new OrderItemDto.CreateOrderItemRequest(103, 203, 4, 4500);

        OrderItemDto.ApiResponseOrderItem result =
                orderItemService.createOrderItem(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().id()).isEqualTo(5);
        assertThat(result.data().orderId()).isEqualTo(103);
        assertThat(result.data().productId()).isEqualTo(203);
        assertThat(result.data().quantity()).isEqualTo(4);
        assertThat(result.data().price()).isEqualTo(4500);
    }

    @Test
    void updateOrderItem_returnsSuccess() {
        pb.order_item.OrderItem.OrderItemResponse orderItemProto =
                pb.order_item.OrderItem.OrderItemResponse.newBuilder()
                        .setId(6)
                        .setOrderId(104)
                        .setProductId(204)
                        .setQuantity(5)
                        .setPrice(5550)
                        .build();

        pb.order_item.OrderItem.ApiResponseOrderItem responseProto =
                pb.order_item.OrderItem.ApiResponseOrderItem.newBuilder()
                        .setData(orderItemProto)
                        .setStatus("success")
                        .setMessage("Order item updated")
                        .build();

        when(orderItemCommandService.updateOrderItem(any(pb.order_item.OrderItemCommand.UpdateOrderItemRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderItemDto.UpdateOrderItemRequest request =
                new OrderItemDto.UpdateOrderItemRequest(6, 104, 204, 5, 5550);

        OrderItemDto.ApiResponseOrderItem result =
                orderItemService.updateOrderItem(6, request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().id()).isEqualTo(6);
        assertThat(result.data().quantity()).isEqualTo(5);
    }

    @Test
    void deleteOrderItem_returnsSuccess() {
        pb.order_item.OrderItem.OrderItemResponseDeleteAt orderItemProto =
                pb.order_item.OrderItem.OrderItemResponseDeleteAt.newBuilder()
                        .setId(7)
                        .setDeletedAt(com.google.protobuf.StringValue.of("2024-07-01T00:00:00Z"))
                        .build();

        pb.order_item.OrderItem.ApiResponseOrderItemDeleteAt responseProto =
                pb.order_item.OrderItem.ApiResponseOrderItemDeleteAt.newBuilder()
                        .setData(orderItemProto)
                        .setStatus("success")
                        .setMessage("Order item trashed")
                        .build();

        when(orderItemCommandService.trashedOrderItem(any(pb.order_item.OrderItem.FindByIdOrderItemRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderItemDto.ApiResponseOrderItemDeleteAt result =
                orderItemService.deleteOrderItem(7).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Order item trashed");
        assertThat(result.data().deletedAt()).isEqualTo("2024-07-01T00:00:00Z");
    }

    @Test
    void restoreOrderItem_returnsSuccess() {
        pb.order_item.OrderItem.OrderItemResponseDeleteAt orderItemProto =
                pb.order_item.OrderItem.OrderItemResponseDeleteAt.newBuilder()
                        .setId(8)
                        .build();

        pb.order_item.OrderItem.ApiResponseOrderItemDeleteAt responseProto =
                pb.order_item.OrderItem.ApiResponseOrderItemDeleteAt.newBuilder()
                        .setData(orderItemProto)
                        .setStatus("success")
                        .setMessage("Order item restored")
                        .build();

        when(orderItemCommandService.restoreOrderItem(any(pb.order_item.OrderItem.FindByIdOrderItemRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderItemDto.ApiResponseOrderItemDeleteAt result =
                orderItemService.restoreOrderItem(8).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Order item restored");
    }

    @Test
    void deleteOrderItemPermanent_returnsSuccess() {
        pb.order_item.OrderItem.ApiResponseOrderItemDelete responseProto =
                pb.order_item.OrderItem.ApiResponseOrderItemDelete.newBuilder()
                        .setStatus("success")
                        .setMessage("Order item permanently deleted")
                        .build();

        when(orderItemCommandService.deleteOrderItemPermanent(any(pb.order_item.OrderItem.FindByIdOrderItemRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderItemDto.ApiResponseOrderItemDelete result =
                orderItemService.deleteOrderItemPermanent(9).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Order item permanently deleted");
    }

    @Test
    void restoreAllOrderItems_returnsSuccess() {
        pb.order_item.OrderItem.ApiResponseOrderItemAll responseProto =
                pb.order_item.OrderItem.ApiResponseOrderItemAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All order items restored")
                        .build();

        when(orderItemCommandService.restoreAllOrderItem(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderItemDto.ApiResponseOrderItemAll result =
                orderItemService.restoreAllOrderItems().await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("All order items restored");
    }

    @Test
    void deleteAllOrderItems_returnsSuccess() {
        pb.order_item.OrderItem.ApiResponseOrderItemAll responseProto =
                pb.order_item.OrderItem.ApiResponseOrderItemAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All order items permanently deleted")
                        .build();

        when(orderItemCommandService.deleteAllOrderItemPermanent(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        OrderItemDto.ApiResponseOrderItemAll result =
                orderItemService.deleteAllOrderItems().await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("All order items permanently deleted");
    }
}
