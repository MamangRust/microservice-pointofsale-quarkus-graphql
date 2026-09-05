package com.sanedge.statsreader.handler;

import com.sanedge.statsreader.cache.StatsQueryService;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.order.stats.MutinyOrderTotalRevenueServiceGrpc;
import pb.order.Order;

@GrpcService
@Singleton
public class OrderTotalRevenueHandler
        extends MutinyOrderTotalRevenueServiceGrpc.OrderTotalRevenueServiceImplBase {

    @Inject
    StatsQueryService statsQuery;

    @Override
    public Uni<Order.ApiResponseOrderMonthlyTotalRevenue> findMonthlyTotalRevenue(
            Order.FindYearMonthTotalRevenue request) {
        String sql = "SELECT CAST(toYear(occurred_at) AS String) AS year, " +
                "formatDateTime(occurred_at, '%Y-%m') AS month, " +
                "CAST(COUNT(*) AS Int32) AS order_count, " +
                "CAST(SUM(total_amount) AS Int32) AS total_revenue " +
                "FROM order_daily WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "AND toMonth(occurred_at) = " + request.getMonth() + " GROUP BY year, month";
        return statsQuery.query("ordertotalrevenue", sql).map(rows -> {
            var b = Order.ApiResponseOrderMonthlyTotalRevenue.newBuilder().setStatus("success").setMessage("Found");
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.getJsonObject(i);
                b.addData(Order.OrderMonthlyTotalRevenueResponse.newBuilder()
                        .setYear(row.getString("year"))
                        .setMonth(row.getString("month"))
                        .setOrderCount(row.getInteger("order_count", 0))
                        .setTotalRevenue(row.getInteger("total_revenue", 0))
                        .build());
            }
            return b.build();
        }).onFailure().recoverWithItem(e -> Order.ApiResponseOrderMonthlyTotalRevenue.newBuilder()
                .setStatus("error").setMessage(e.getMessage()).build());
    }

    @Override
    public Uni<Order.ApiResponseOrderYearlyTotalRevenue> findYearlyTotalRevenue(
            Order.FindYearTotalRevenue request) {
        String sql = "SELECT CAST(toYear(occurred_at) AS String) AS year, " +
                "CAST(COUNT(*) AS Int32) AS order_count, " +
                "CAST(SUM(total_amount) AS Int32) AS total_revenue " +
                "FROM order_daily WHERE toYear(occurred_at) = " + request.getYear() + " GROUP BY year";
        return statsQuery.query("ordertotalrevenue", sql).map(rows -> {
            var b = Order.ApiResponseOrderYearlyTotalRevenue.newBuilder().setStatus("success").setMessage("Found");
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.getJsonObject(i);
                b.addData(Order.OrderYearlyTotalRevenueResponse.newBuilder()
                        .setYear(row.getString("year"))
                        .setOrderCount(row.getInteger("order_count", 0))
                        .setTotalRevenue(row.getInteger("total_revenue", 0))
                        .build());
            }
            return b.build();
        }).onFailure().recoverWithItem(e -> Order.ApiResponseOrderYearlyTotalRevenue.newBuilder()
                .setStatus("error").setMessage(e.getMessage()).build());
    }

    @Override
    public Uni<Order.ApiResponseOrderMonthlyTotalRevenue> findMonthlyTotalRevenueById(
            Order.FindYearMonthTotalRevenueById request) {
        return Uni.createFrom().item(Order.ApiResponseOrderMonthlyTotalRevenue.newBuilder()
                .setStatus("success").setMessage("Order by-id filter not available").build());
    }

    @Override
    public Uni<Order.ApiResponseOrderYearlyTotalRevenue> findYearlyTotalRevenueById(
            Order.FindYearTotalRevenueById request) {
        return Uni.createFrom().item(Order.ApiResponseOrderYearlyTotalRevenue.newBuilder()
                .setStatus("success").setMessage("Order by-id filter not available").build());
    }

    @Override
    public Uni<Order.ApiResponseOrderMonthlyTotalRevenue> findMonthlyTotalRevenueByMerchant(
            Order.FindYearMonthTotalRevenueByMerchant request) {
        String sql = "SELECT CAST(toYear(occurred_at) AS String) AS year, " +
                "formatDateTime(occurred_at, '%Y-%m') AS month, " +
                "CAST(COUNT(*) AS Int32) AS order_count, " +
                "CAST(SUM(total_amount) AS Int32) AS total_revenue " +
                "FROM order_daily WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "AND toMonth(occurred_at) = " + request.getMonth() + " " +
                "AND merchant_id = '" + request.getMerchantId() + "' GROUP BY year, month";
        return statsQuery.query("ordertotalrevenue", sql).map(rows -> {
            var b = Order.ApiResponseOrderMonthlyTotalRevenue.newBuilder().setStatus("success").setMessage("Found");
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.getJsonObject(i);
                b.addData(Order.OrderMonthlyTotalRevenueResponse.newBuilder()
                        .setYear(row.getString("year"))
                        .setMonth(row.getString("month"))
                        .setOrderCount(row.getInteger("order_count", 0))
                        .setTotalRevenue(row.getInteger("total_revenue", 0))
                        .build());
            }
            return b.build();
        }).onFailure().recoverWithItem(e -> Order.ApiResponseOrderMonthlyTotalRevenue.newBuilder()
                .setStatus("error").setMessage(e.getMessage()).build());
    }

    @Override
    public Uni<Order.ApiResponseOrderYearlyTotalRevenue> findYearlyTotalRevenueByMerchant(
            Order.FindYearTotalRevenueByMerchant request) {
        String sql = "SELECT CAST(toYear(occurred_at) AS String) AS year, " +
                "CAST(COUNT(*) AS Int32) AS order_count, " +
                "CAST(SUM(total_amount) AS Int32) AS total_revenue " +
                "FROM order_daily WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "AND merchant_id = '" + request.getMerchantId() + "' GROUP BY year";
        return statsQuery.query("ordertotalrevenue", sql).map(rows -> {
            var b = Order.ApiResponseOrderYearlyTotalRevenue.newBuilder().setStatus("success").setMessage("Found");
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.getJsonObject(i);
                b.addData(Order.OrderYearlyTotalRevenueResponse.newBuilder()
                        .setYear(row.getString("year"))
                        .setOrderCount(row.getInteger("order_count", 0))
                        .setTotalRevenue(row.getInteger("total_revenue", 0))
                        .build());
            }
            return b.build();
        }).onFailure().recoverWithItem(e -> Order.ApiResponseOrderYearlyTotalRevenue.newBuilder()
                .setStatus("error").setMessage(e.getMessage()).build());
    }
}
