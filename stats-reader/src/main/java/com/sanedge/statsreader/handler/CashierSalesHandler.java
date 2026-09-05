package com.sanedge.statsreader.handler;

import com.sanedge.statsreader.cache.StatsQueryService;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.cashier.stats.MutinyCashierSalesServiceGrpc;
import pb.cashier.Cashier;

@GrpcService
@Singleton
public class CashierSalesHandler
        extends MutinyCashierSalesServiceGrpc.CashierSalesServiceImplBase {

    @Inject
    StatsQueryService statsQuery;

    @Override
    public Uni<Cashier.ApiResponseCashierMonthSales> findMonthSales(Cashier.FindYearCashier request) {
        String sql = "SELECT formatDateTime(occurred_at, '%Y-%m') AS month, " +
                "cashier_id, '' AS cashier_name, CAST(COUNT(*) AS Int32) AS order_count, " +
                "CAST(SUM(total_amount) AS Int32) AS total_sales " +
                "FROM order_daily WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "AND cashier_id IS NOT NULL GROUP BY month, cashier_id ORDER BY month";
        return statsQuery.query("cashiersales", sql).map(rows -> {
            var b = Cashier.ApiResponseCashierMonthSales.newBuilder().setStatus("success").setMessage("Found");
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.getJsonObject(i);
                b.addData(Cashier.CashierResponseMonthSales.newBuilder()
                        .setMonth(row.getString("month"))
                        .setCashierId(row.getInteger("cashier_id", 0))
                        .setCashierName(row.getString("cashier_name", ""))
                        .setOrderCount(row.getInteger("order_count", 0))
                        .setTotalSales(row.getInteger("total_sales", 0))
                        .build());
            }
            return b.build();
        }).onFailure().recoverWithItem(e -> Cashier.ApiResponseCashierMonthSales.newBuilder()
                .setStatus("error").setMessage(e.getMessage()).build());
    }

    @Override
    public Uni<Cashier.ApiResponseCashierYearSales> findYearSales(Cashier.FindYearCashier request) {
        String sql = "SELECT CAST(toYear(occurred_at) AS String) AS year, " +
                "cashier_id, '' AS cashier_name, CAST(COUNT(*) AS Int32) AS order_count, " +
                "CAST(SUM(total_amount) AS Int32) AS total_sales " +
                "FROM order_daily WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "AND cashier_id IS NOT NULL GROUP BY year, cashier_id";
        return statsQuery.query("cashiersales", sql).map(rows -> {
            var b = Cashier.ApiResponseCashierYearSales.newBuilder().setStatus("success").setMessage("Found");
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.getJsonObject(i);
                b.addData(Cashier.CashierResponseYearSales.newBuilder()
                        .setYear(row.getString("year"))
                        .setCashierId(row.getInteger("cashier_id", 0))
                        .setCashierName(row.getString("cashier_name", ""))
                        .setOrderCount(row.getInteger("order_count", 0))
                        .setTotalSales(row.getInteger("total_sales", 0))
                        .build());
            }
            return b.build();
        }).onFailure().recoverWithItem(e -> Cashier.ApiResponseCashierYearSales.newBuilder()
                .setStatus("error").setMessage(e.getMessage()).build());
    }

    @Override
    public Uni<Cashier.ApiResponseCashierMonthSales> findMonthSalesByMerchant(Cashier.FindYearCashierByMerchant request) {
        String sql = "SELECT formatDateTime(occurred_at, '%Y-%m') AS month, " +
                "cashier_id, '' AS cashier_name, CAST(COUNT(*) AS Int32) AS order_count, " +
                "CAST(SUM(total_amount) AS Int32) AS total_sales " +
                "FROM order_daily WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "AND merchant_id = '" + request.getMerchantId() + "' AND cashier_id IS NOT NULL " +
                "GROUP BY month, cashier_id ORDER BY month";
        return statsQuery.query("cashiersales", sql).map(rows -> {
            var b = Cashier.ApiResponseCashierMonthSales.newBuilder().setStatus("success").setMessage("Found");
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.getJsonObject(i);
                b.addData(Cashier.CashierResponseMonthSales.newBuilder()
                        .setMonth(row.getString("month"))
                        .setCashierId(row.getInteger("cashier_id", 0))
                        .setCashierName(row.getString("cashier_name", ""))
                        .setOrderCount(row.getInteger("order_count", 0))
                        .setTotalSales(row.getInteger("total_sales", 0))
                        .build());
            }
            return b.build();
        }).onFailure().recoverWithItem(e -> Cashier.ApiResponseCashierMonthSales.newBuilder()
                .setStatus("error").setMessage(e.getMessage()).build());
    }

    @Override
    public Uni<Cashier.ApiResponseCashierYearSales> findYearSalesByMerchant(Cashier.FindYearCashierByMerchant request) {
        String sql = "SELECT CAST(toYear(occurred_at) AS String) AS year, " +
                "cashier_id, '' AS cashier_name, CAST(COUNT(*) AS Int32) AS order_count, " +
                "CAST(SUM(total_amount) AS Int32) AS total_sales " +
                "FROM order_daily WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "AND merchant_id = '" + request.getMerchantId() + "' AND cashier_id IS NOT NULL " +
                "GROUP BY year, cashier_id";
        return statsQuery.query("cashiersales", sql).map(rows -> {
            var b = Cashier.ApiResponseCashierYearSales.newBuilder().setStatus("success").setMessage("Found");
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.getJsonObject(i);
                b.addData(Cashier.CashierResponseYearSales.newBuilder()
                        .setYear(row.getString("year"))
                        .setCashierId(row.getInteger("cashier_id", 0))
                        .setCashierName(row.getString("cashier_name", ""))
                        .setOrderCount(row.getInteger("order_count", 0))
                        .setTotalSales(row.getInteger("total_sales", 0))
                        .build());
            }
            return b.build();
        }).onFailure().recoverWithItem(e -> Cashier.ApiResponseCashierYearSales.newBuilder()
                .setStatus("error").setMessage(e.getMessage()).build());
    }

    @Override
    public Uni<Cashier.ApiResponseCashierMonthSales> findMonthSalesById(Cashier.FindYearCashierById request) {
        String sql = "SELECT formatDateTime(occurred_at, '%Y-%m') AS month, " +
                "cashier_id, '' AS cashier_name, CAST(COUNT(*) AS Int32) AS order_count, " +
                "CAST(SUM(total_amount) AS Int32) AS total_sales " +
                "FROM order_daily WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "AND cashier_id = '" + request.getCashierId() + "' GROUP BY month, cashier_id ORDER BY month";
        return statsQuery.query("cashiersales", sql).map(rows -> {
            var b = Cashier.ApiResponseCashierMonthSales.newBuilder().setStatus("success").setMessage("Found");
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.getJsonObject(i);
                b.addData(Cashier.CashierResponseMonthSales.newBuilder()
                        .setMonth(row.getString("month"))
                        .setCashierId(row.getInteger("cashier_id", 0))
                        .setCashierName(row.getString("cashier_name", ""))
                        .setOrderCount(row.getInteger("order_count", 0))
                        .setTotalSales(row.getInteger("total_sales", 0))
                        .build());
            }
            return b.build();
        }).onFailure().recoverWithItem(e -> Cashier.ApiResponseCashierMonthSales.newBuilder()
                .setStatus("error").setMessage(e.getMessage()).build());
    }

    @Override
    public Uni<Cashier.ApiResponseCashierYearSales> findYearSalesById(Cashier.FindYearCashierById request) {
        String sql = "SELECT CAST(toYear(occurred_at) AS String) AS year, " +
                "cashier_id, '' AS cashier_name, CAST(COUNT(*) AS Int32) AS order_count, " +
                "CAST(SUM(total_amount) AS Int32) AS total_sales " +
                "FROM order_daily WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "AND cashier_id = '" + request.getCashierId() + "' GROUP BY year, cashier_id";
        return statsQuery.query("cashiersales", sql).map(rows -> {
            var b = Cashier.ApiResponseCashierYearSales.newBuilder().setStatus("success").setMessage("Found");
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.getJsonObject(i);
                b.addData(Cashier.CashierResponseYearSales.newBuilder()
                        .setYear(row.getString("year"))
                        .setCashierId(row.getInteger("cashier_id", 0))
                        .setCashierName(row.getString("cashier_name", ""))
                        .setOrderCount(row.getInteger("order_count", 0))
                        .setTotalSales(row.getInteger("total_sales", 0))
                        .build());
            }
            return b.build();
        }).onFailure().recoverWithItem(e -> Cashier.ApiResponseCashierYearSales.newBuilder()
                .setStatus("error").setMessage(e.getMessage()).build());
    }
}
