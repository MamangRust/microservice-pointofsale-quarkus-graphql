package com.sanedge.order_item.service;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.order_item.domain.requests.CreateOrderItemRequest;
import com.sanedge.order_item.domain.requests.UpdateOrderItemRequest;
import com.sanedge.order_item.domain.response.OrderItemResponse;
import com.sanedge.order_item.domain.response.OrderItemResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface OrderItemCommandService {
    Uni<ApiResponse<OrderItemResponse>> create(CreateOrderItemRequest request);
    Uni<ApiResponse<OrderItemResponse>> update(UpdateOrderItemRequest request);
    Uni<ApiResponse<OrderItemResponseDeleteAt>> trash(Integer id);
    Uni<ApiResponse<OrderItemResponseDeleteAt>> restore(Integer id);
    Uni<ApiResponse<Boolean>> delete(Integer id);
    Uni<ApiResponse<Boolean>> restoreAll();
    Uni<ApiResponse<Boolean>> deleteAll();
}
