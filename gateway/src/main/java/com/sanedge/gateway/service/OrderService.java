package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.OrderDto;
import io.smallrye.mutiny.Uni;

public interface OrderService {
    Uni<OrderDto.ApiResponsePaginationOrder> listOrders(int page, int size, String search);
    Uni<OrderDto.ApiResponsePaginationOrderDeleteAt> getActiveOrders(int page, int size, String search);
    Uni<OrderDto.ApiResponsePaginationOrderDeleteAt> getTrashedOrders(int page, int size, String search);
    Uni<OrderDto.ApiResponseOrder> getOrder(int id);
    Uni<OrderDto.ApiResponsePaginationOrder> getOrdersByMerchant(int merchantId, int page, int size, String search);
    Uni<OrderDto.ApiResponseOrder> createOrder(OrderDto.CreateOrderRequest body);
    Uni<OrderDto.ApiResponseOrder> updateOrder(int id, OrderDto.UpdateOrderRequest body);
    Uni<OrderDto.ApiResponseOrderDeleteAt> deleteOrder(int id);
    Uni<OrderDto.ApiResponseOrderDeleteAt> restoreOrder(int id);
    Uni<OrderDto.ApiResponseOrderDelete> deleteOrderPermanent(int id);
    Uni<OrderDto.ApiResponseOrderAll> restoreAllOrders();
    Uni<OrderDto.ApiResponseOrderAll> deleteAllOrders();

    // Statistics
    Uni<OrderDto.ApiResponseOrderMonthlyTotalRevenue> getMonthlyTotalRevenues(int year, int month);
    Uni<OrderDto.ApiResponseOrderYearlyTotalRevenue> getYearlyTotalRevenues(int year);
    Uni<OrderDto.ApiResponseOrderMonthlyTotalRevenue> getMonthlyTotalRevenuesByMerchant(int merchantId, int year, int month);
    Uni<OrderDto.ApiResponseOrderYearlyTotalRevenue> getYearlyTotalRevenuesByMerchant(int merchantId, int year);
    Uni<OrderDto.ApiResponseOrderMonthly> getMonthlyRevenues(int year);
    Uni<OrderDto.ApiResponseOrderYearly> getYearlyRevenues(int year);
    Uni<OrderDto.ApiResponseOrderMonthly> getMonthlyRevenuesByMerchant(int merchantId, int year);
    Uni<OrderDto.ApiResponseOrderYearly> getYearlyRevenuesByMerchant(int merchantId, int year);
}
