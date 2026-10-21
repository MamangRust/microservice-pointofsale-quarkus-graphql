package com.sanedge.order_item.service;

import java.util.List;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.order_item.domain.requests.FindAllOrderItems;
import com.sanedge.order_item.domain.response.OrderItemResponse;
import com.sanedge.order_item.domain.response.OrderItemResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface OrderItemQueryService {
    Uni<ApiResponsePagination<List<OrderItemResponse>>> findAll(FindAllOrderItems request);
    Uni<ApiResponsePagination<List<OrderItemResponseDeleteAt>>> findByActive(FindAllOrderItems request);
    Uni<ApiResponsePagination<List<OrderItemResponseDeleteAt>>> findByTrashed(FindAllOrderItems request);
    Uni<ApiResponse<List<OrderItemResponse>>> findOrderItemByOrder(Integer orderId);
}
