package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.OrderItemDto;
import io.smallrye.mutiny.Uni;

public interface OrderItemService {
    Uni<OrderItemDto.ApiResponsePaginationOrderItem> listOrderItems(int page, int size, String search);
    Uni<OrderItemDto.ApiResponsePaginationOrderItemDeleteAt> getActiveOrderItems(int page, int size, String search);
    Uni<OrderItemDto.ApiResponsePaginationOrderItemDeleteAt> getTrashedOrderItems(int page, int size, String search);
    Uni<OrderItemDto.ApiResponsesOrderItem> getOrderItemsByOrder(int orderId);
    Uni<OrderItemDto.ApiResponseOrderItem> createOrderItem(OrderItemDto.CreateOrderItemRequest body);
    Uni<OrderItemDto.ApiResponseOrderItem> updateOrderItem(int id, OrderItemDto.UpdateOrderItemRequest body);
    Uni<OrderItemDto.ApiResponseOrderItemDeleteAt> deleteOrderItem(int id);
    Uni<OrderItemDto.ApiResponseOrderItemDeleteAt> restoreOrderItem(int id);
    Uni<OrderItemDto.ApiResponseOrderItemDelete> deleteOrderItemPermanent(int id);
    Uni<OrderItemDto.ApiResponseOrderItemAll> restoreAllOrderItems();
    Uni<OrderItemDto.ApiResponseOrderItemAll> deleteAllOrderItems();
}
