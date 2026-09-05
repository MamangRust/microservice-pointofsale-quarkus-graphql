package com.sanedge.statsreader.handler;

import com.sanedge.statsreader.cache.StatsQueryService;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.order.stats.MutinyOrderSoldoutServiceGrpc;
import pb.order.Order;

@GrpcService
@Singleton
public class OrderSoldoutHandler
        extends MutinyOrderSoldoutServiceGrpc.OrderSoldoutServiceImplBase {

    @Inject
    StatsQueryService statsQuery;

    @Override
    public Uni<Order.ApiResponseOrderMonthly> findMonthlyRevenue(Order.FindYearOrder request) {
        String sql = "SELECT formatDateTime(occurred_at, '%Y-%m') AS month, " +
                "CAST(COUNT(*) AS Int32) AS order_count, " +
                "CAST(SUM(total_amount) AS Int32) AS total_revenue " +
                "FROM order_daily WHERE toYear(occurred_at) = " + request.getYear() + " GROUP BY month ORDER BY month";
        return statsQuery.query("ordersoldout", sql).map(rows -> {
            var b = Order.ApiResponseOrderMonthly.newBuilder().setStatus("success").setMessage("Found");
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.getJsonObject(i);
                b.addData(Order.OrderMonthlyResponse.newBuilder()
                        .setMonth(row.getString("month"))
                        .setOrderCount(row.getInteger("order_count", 0))
                        .setTotalRevenue(row.getInteger("total_revenue", 0))
                        .build());
            }
            return b.build();
        }).onFailure().recoverWithItem(e -> Order.ApiResponseOrderMonthly.newBuilder()
                .setStatus("error").setMessage(e.getMessage()).build());
    }

    @Override
    public Uni<Order.ApiResponseOrderYearly> findYearlyRevenue(Order.FindYearOrder request) {
        String sql = "SELECT CAST(toYear(occurred_at) AS String) AS year, " +
                "CAST(COUNT(*) AS Int32) AS order_count, " +
                "CAST(SUM(total_amount) AS Int32) AS total_revenue, " +
                "CAST(COUNT(DISTINCT cashier_id) AS Int32) AS active_cashiers " +
                "FROM order_daily WHERE toYear(occurred_at) = " + request.getYear() + " GROUP BY year";
        return statsQuery.query("ordersoldout", sql).map(rows -> {
            var b = Order.ApiResponseOrderYearly.newBuilder().setStatus("success").setMessage("Found");
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.getJsonObject(i);
                b.addData(Order.OrderYearlyResponse.newBuilder()
                        .setYear(row.getString("year"))
                        .setOrderCount(row.getInteger("order_count", 0))
                        .setTotalRevenue(row.getInteger("total_revenue", 0))
                        .setActiveCashiers(row.getInteger("active_cashiers", 0))
                        .build());
            }
            return b.build();
        }).onFailure().recoverWithItem(e -> Order.ApiResponseOrderYearly.newBuilder()
                .setStatus("error").setMessage(e.getMessage()).build());
    }

    @Override
    public Uni<Order.ApiResponseOrderMonthly> findMonthlyRevenueByMerchant(Order.FindYearOrderByMerchant request) {
        String sql = "SELECT formatDateTime(occurred_at, '%Y-%m') AS month, " +
                "CAST(COUNT(*) AS Int32) AS order_count, " +
                "CAST(SUM(total_amount) AS Int32) AS total_revenue " +
                "FROM order_daily WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "AND merchant_id = '" + request.getMerchantId() + "' GROUP BY month ORDER BY month";
        return statsQuery.query("ordersoldout", sql).map(rows -> {
            var b = Order.ApiResponseOrderMonthly.newBuilder().setStatus("success").setMessage("Found");
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.getJsonObject(i);
                b.addData(Order.OrderMonthlyResponse.newBuilder()
                        .setMonth(row.getString("month"))
                        .setOrderCount(row.getInteger("order_count", 0))
                        .setTotalRevenue(row.getInteger("total_revenue", 0))
                        .build());
            }
            return b.build();
        }).onFailure().recoverWithItem(e -> Order.ApiResponseOrderMonthly.newBuilder()
                .setStatus("error").setMessage(e.getMessage()).build());
    }

    @Override
    public Uni<Order.ApiResponseOrderYearly> findYearlyRevenueByMerchant(Order.FindYearOrderByMerchant request) {
        String sql = "SELECT CAST(toYear(occurred_at) AS String) AS year, " +
                "CAST(COUNT(*) AS Int32) AS order_count, " +
                "CAST(SUM(total_amount) AS Int32) AS total_revenue, " +
                "CAST(COUNT(DISTINCT cashier_id) AS Int32) AS active_cashiers " +
                "FROM order_daily WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "AND merchant_id = '" + request.getMerchantId() + "' GROUP BY year";
        return statsQuery.query("ordersoldout", sql).map(rows -> {
            var b = Order.ApiResponseOrderYearly.newBuilder().setStatus("success").setMessage("Found");
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.getJsonObject(i);
                b.addData(Order.OrderYearlyResponse.newBuilder()
                        .setYear(row.getString("year"))
                        .setOrderCount(row.getInteger("order_count", 0))
                        .setTotalRevenue(row.getInteger("total_revenue", 0))
                        .setActiveCashiers(row.getInteger("active_cashiers", 0))
                        .build());
            }
            return b.build();
        }).onFailure().recoverWithItem(e -> Order.ApiResponseOrderYearly.newBuilder()
                .setStatus("error").setMessage(e.getMessage()).build());
    }
}
