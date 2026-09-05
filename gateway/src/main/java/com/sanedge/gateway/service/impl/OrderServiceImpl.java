package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.OrderDto;
import com.sanedge.gateway.service.OrderService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OrderServiceImpl implements OrderService {

    private static final Logger LOG = Logger.getLogger(OrderServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("order")
    pb.order.MutinyOrderQueryServiceGrpc.MutinyOrderQueryServiceStub orderQueryService;

    @GrpcClient("order")
    pb.order.MutinyOrderCommandServiceGrpc.MutinyOrderCommandServiceStub orderCommandService;

    @GrpcClient("stats-reader")
    pb.order.stats.MutinyOrderTotalRevenueServiceGrpc.MutinyOrderTotalRevenueServiceStub orderTotalRevenueServiceStub;

    @GrpcClient("stats-reader")
    pb.order.stats.MutinyOrderSoldoutServiceGrpc.MutinyOrderSoldoutServiceStub orderRevenueServiceStub;

    @Override
    public Uni<OrderDto.ApiResponsePaginationOrder> listOrders(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("order.listOrders", () -> orderQueryService.findAll(pb.order.Order.FindAllOrderRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(OrderDto.ApiResponsePaginationOrder::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list orders: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponsePaginationOrderDeleteAt> getActiveOrders(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("order.getActiveOrders", () -> orderQueryService.findByActive(pb.order.Order.FindAllOrderRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(OrderDto.ApiResponsePaginationOrderDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list active orders: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponsePaginationOrderDeleteAt> getTrashedOrders(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("order.getTrashedOrders", () -> orderQueryService.findByTrashed(pb.order.Order.FindAllOrderRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(OrderDto.ApiResponsePaginationOrderDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list trashed orders: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrder> getOrder(int id) {
        return telemetryHelper.traceAndMetric("order.getOrder", () -> orderQueryService.findById(pb.order.Order.FindByIdOrderRequest.newBuilder()
                .setId(id)
                .build())
                .map(OrderDto.ApiResponseOrder::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get order " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponsePaginationOrder> getOrdersByMerchant(int merchantId, int page, int size, String search) {
        return telemetryHelper.traceAndMetric("order.getOrdersByMerchant", () -> orderQueryService.findByMerchant(pb.order.Order.FindAllOrderMerchantRequest.newBuilder()
                .setMerchantId(merchantId)
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(OrderDto.ApiResponsePaginationOrder::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get orders by merchant " + merchantId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrder> createOrder(OrderDto.CreateOrderRequest body) {
        return telemetryHelper.traceAndMetric("order.createOrder", () -> orderCommandService.create(pb.order.Order.CreateOrderRequest.newBuilder()
                .setMerchantId(body.merchantId())
                .setCashierId(body.cashierId())
                .addAllItems(body.items().stream()
                        .map(item -> pb.order.Order.CreateOrderItemRequest.newBuilder()
                                .setProductId(item.productId())
                                .setQuantity(item.quantity())
                                .build())
                        .collect(Collectors.toList()))
                .build())
                .map(OrderDto.ApiResponseOrder::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create order: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrder> updateOrder(int id, OrderDto.UpdateOrderRequest body) {
        return telemetryHelper.traceAndMetric("order.updateOrder", () -> orderCommandService.update(pb.order.Order.UpdateOrderRequest.newBuilder()
                .setOrderId(id)
                .setCashierId(body.cashierId())
                .addAllItems(body.items().stream()
                        .map(item -> pb.order.Order.UpdateOrderItemRequest.newBuilder()
                                .setOrderItemId(item.orderItemId())
                                .setProductId(item.productId())
                                .setQuantity(item.quantity())
                                .build())
                        .collect(Collectors.toList()))
                .build())
                .map(OrderDto.ApiResponseOrder::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update order " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrderDeleteAt> deleteOrder(int id) {
        return telemetryHelper.traceAndMetric("order.deleteOrder", () -> orderCommandService.trashedOrder(pb.order.Order.FindByIdOrderRequest.newBuilder()
                .setId(id)
                .build())
                .map(OrderDto.ApiResponseOrderDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete order " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrderDeleteAt> restoreOrder(int id) {
        return telemetryHelper.traceAndMetric("order.restoreOrder", () -> orderCommandService.restoreOrder(pb.order.Order.FindByIdOrderRequest.newBuilder()
                .setId(id)
                .build())
                .map(OrderDto.ApiResponseOrderDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore order " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrderDelete> deleteOrderPermanent(int id) {
        return telemetryHelper.traceAndMetric("order.deleteOrderPermanent", () -> orderCommandService.deleteOrderPermanent(pb.order.Order.FindByIdOrderRequest.newBuilder()
                .setId(id)
                .build())
                .map(OrderDto.ApiResponseOrderDelete::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete order " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrderAll> restoreAllOrders() {
        return telemetryHelper.traceAndMetric("order.restoreAllOrders", () -> orderCommandService.restoreAllOrder(com.google.protobuf.Empty.getDefaultInstance())
                .map(OrderDto.ApiResponseOrderAll::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all orders: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrderAll> deleteAllOrders() {
        return telemetryHelper.traceAndMetric("order.deleteAllOrdersPermanent", () -> orderCommandService.deleteAllOrderPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .map(OrderDto.ApiResponseOrderAll::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all orders: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrderMonthlyTotalRevenue> getMonthlyTotalRevenues(int year, int month) {
        return telemetryHelper.traceAndMetric("order.getMonthlyTotalRevenues", () -> orderTotalRevenueServiceStub.findMonthlyTotalRevenue(pb.order.Order.FindYearMonthTotalRevenue.newBuilder()
                .setYear(year)
                .setMonth(month)
                .build())
                .map(OrderDto.ApiResponseOrderMonthlyTotalRevenue::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly total revenues: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrderYearlyTotalRevenue> getYearlyTotalRevenues(int year) {
        return telemetryHelper.traceAndMetric("order.getYearlyTotalRevenues", () -> orderTotalRevenueServiceStub.findYearlyTotalRevenue(pb.order.Order.FindYearTotalRevenue.newBuilder()
                .setYear(year)
                .build())
                .map(OrderDto.ApiResponseOrderYearlyTotalRevenue::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly total revenues: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrderMonthlyTotalRevenue> getMonthlyTotalRevenuesByMerchant(int merchantId, int year, int month) {
        return telemetryHelper.traceAndMetric("order.getMonthlyTotalRevenuesByMerchant", () -> orderTotalRevenueServiceStub.findMonthlyTotalRevenueByMerchant(pb.order.Order.FindYearMonthTotalRevenueByMerchant.newBuilder()
                .setMerchantId(merchantId)
                .setYear(year)
                .setMonth(month)
                .build())
                .map(OrderDto.ApiResponseOrderMonthlyTotalRevenue::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly total revenues for merchant " + merchantId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrderYearlyTotalRevenue> getYearlyTotalRevenuesByMerchant(int merchantId, int year) {
        return telemetryHelper.traceAndMetric("order.getYearlyTotalRevenuesByMerchant", () -> orderTotalRevenueServiceStub.findYearlyTotalRevenueByMerchant(pb.order.Order.FindYearTotalRevenueByMerchant.newBuilder()
                .setMerchantId(merchantId)
                .setYear(year)
                .build())
                .map(OrderDto.ApiResponseOrderYearlyTotalRevenue::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly total revenues for merchant " + merchantId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrderMonthly> getMonthlyRevenues(int year) {
        return telemetryHelper.traceAndMetric("order.getMonthlyRevenues", () -> orderRevenueServiceStub.findMonthlyRevenue(pb.order.Order.FindYearOrder.newBuilder()
                .setYear(year)
                .build())
                .map(OrderDto.ApiResponseOrderMonthly::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly revenues: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrderYearly> getYearlyRevenues(int year) {
        return telemetryHelper.traceAndMetric("order.getYearlyRevenues", () -> orderRevenueServiceStub.findYearlyRevenue(pb.order.Order.FindYearOrder.newBuilder()
                .setYear(year)
                .build())
                .map(OrderDto.ApiResponseOrderYearly::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly revenues: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrderMonthly> getMonthlyRevenuesByMerchant(int merchantId, int year) {
        return telemetryHelper.traceAndMetric("order.getMonthlyRevenuesByMerchant", () -> orderRevenueServiceStub.findMonthlyRevenueByMerchant(pb.order.Order.FindYearOrderByMerchant.newBuilder()
                .setMerchantId(merchantId)
                .setYear(year)
                .build())
                .map(OrderDto.ApiResponseOrderMonthly::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly revenues for merchant " + merchantId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<OrderDto.ApiResponseOrderYearly> getYearlyRevenuesByMerchant(int merchantId, int year) {
        return telemetryHelper.traceAndMetric("order.getYearlyRevenuesByMerchant", () -> orderRevenueServiceStub.findYearlyRevenueByMerchant(pb.order.Order.FindYearOrderByMerchant.newBuilder()
                .setMerchantId(merchantId)
                .setYear(year)
                .build())
                .map(OrderDto.ApiResponseOrderYearly::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly revenues for merchant " + merchantId + ": " + throwable.getMessage(), throwable)));
    }
}
