package com.sanedge.statsreader.handler;

import com.sanedge.statsreader.cache.StatsQueryService;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.cashier.Cashier;
import pb.cashier.stats.CashierTotalSales;
import pb.cashier.stats.MutinyCashierTotalSalesServiceGrpc;

@GrpcService
@Singleton
public class CashierTotalSalesHandler
        extends MutinyCashierTotalSalesServiceGrpc.CashierTotalSalesServiceImplBase {

    @Inject
    StatsQueryService statsQuery;

    @Override
    public Uni<CashierTotalSales.ApiResponseCashierMonthlyTotalSales> findMonthlyTotalSales(
            Cashier.FindYearMonthTotalSales request) {
        String sql = "SELECT CAST(toYear(occurred_at) AS String) AS year, " +
                "formatDateTime(occurred_at, '%Y-%m') AS month, " +
                "CAST(SUM(total_amount) AS Int32) AS total_sales " +
                "FROM order_daily WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "AND toMonth(occurred_at) = " + request.getMonth() + " GROUP BY year, month";
        return statsQuery.query("cashier_total_sales", sql)
                .map(rows -> {
                    var b = CashierTotalSales.ApiResponseCashierMonthlyTotalSales.newBuilder()
                            .setStatus("success").setMessage("Found");
                    for (int i = 0; i < rows.size(); i++) {
                        var row = rows.getJsonObject(i);
                        b.addData(Cashier.CashierResponseMonthTotalSales.newBuilder()
                                .setYear(row.getString("year"))
                                .setMonth(row.getString("month"))
                                .setTotalSales(row.getInteger("total_sales", 0))
                                .build());
                    }
                    return b.build();
                })
                .onFailure().recoverWithItem(e ->
                        CashierTotalSales.ApiResponseCashierMonthlyTotalSales.newBuilder()
                                .setStatus("error").setMessage(e.getMessage()).build());
    }

    @Override
    public Uni<CashierTotalSales.ApiResponseCashierYearlyTotalSales> findYearlyTotalSales(
            Cashier.FindYearTotalSales request) {
        String sql = "SELECT CAST(toYear(occurred_at) AS String) AS year, " +
                "CAST(SUM(total_amount) AS Int32) AS total_sales " +
                "FROM order_daily WHERE toYear(occurred_at) = " + request.getYear() + " GROUP BY year";
        return statsQuery.query("cashier_total_sales", sql)
                .map(rows -> {
                    var b = CashierTotalSales.ApiResponseCashierYearlyTotalSales.newBuilder()
                            .setStatus("success").setMessage("Found");
                    for (int i = 0; i < rows.size(); i++) {
                        var row = rows.getJsonObject(i);
                        b.addData(Cashier.CashierResponseYearTotalSales.newBuilder()
                                .setYear(row.getString("year"))
                                .setTotalSales(row.getInteger("total_sales", 0))
                                .build());
                    }
                    return b.build();
                })
                .onFailure().recoverWithItem(e ->
                        CashierTotalSales.ApiResponseCashierYearlyTotalSales.newBuilder()
                                .setStatus("error").setMessage(e.getMessage()).build());
    }

    @Override
    public Uni<CashierTotalSales.ApiResponseCashierMonthlyTotalSales> findMonthlyTotalSalesById(
            Cashier.FindYearMonthTotalSalesById request) {
        String sql = "SELECT CAST(toYear(occurred_at) AS String) AS year, " +
                "formatDateTime(occurred_at, '%Y-%m') AS month, " +
                "CAST(SUM(total_amount) AS Int32) AS total_sales " +
                "FROM order_daily WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "AND toMonth(occurred_at) = " + request.getMonth() + " " +
                "AND cashier_id = '" + request.getCashierId() + "' GROUP BY year, month";
        return statsQuery.query("cashier_total_sales", sql)
                .map(rows -> {
                    var b = CashierTotalSales.ApiResponseCashierMonthlyTotalSales.newBuilder()
                            .setStatus("success").setMessage("Found");
                    for (int i = 0; i < rows.size(); i++) {
                        var row = rows.getJsonObject(i);
                        b.addData(Cashier.CashierResponseMonthTotalSales.newBuilder()
                                .setYear(row.getString("year"))
                                .setMonth(row.getString("month"))
                                .setTotalSales(row.getInteger("total_sales", 0))
                                .build());
                    }
                    return b.build();
                })
                .onFailure().recoverWithItem(e ->
                        CashierTotalSales.ApiResponseCashierMonthlyTotalSales.newBuilder()
                                .setStatus("error").setMessage(e.getMessage()).build());
    }

    @Override
    public Uni<CashierTotalSales.ApiResponseCashierYearlyTotalSales> findYearlyTotalSalesById(
            Cashier.FindYearTotalSalesById request) {
        String sql = "SELECT CAST(toYear(occurred_at) AS String) AS year, " +
                "CAST(SUM(total_amount) AS Int32) AS total_sales " +
                "FROM order_daily WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "AND cashier_id = '" + request.getCashierId() + "' GROUP BY year";
        return statsQuery.query("cashier_total_sales", sql)
                .map(rows -> {
                    var b = CashierTotalSales.ApiResponseCashierYearlyTotalSales.newBuilder()
                            .setStatus("success").setMessage("Found");
                    for (int i = 0; i < rows.size(); i++) {
                        var row = rows.getJsonObject(i);
                        b.addData(Cashier.CashierResponseYearTotalSales.newBuilder()
                                .setYear(row.getString("year"))
                                .setTotalSales(row.getInteger("total_sales", 0))
                                .build());
                    }
                    return b.build();
                })
                .onFailure().recoverWithItem(e ->
                        CashierTotalSales.ApiResponseCashierYearlyTotalSales.newBuilder()
                                .setStatus("error").setMessage(e.getMessage()).build());
    }

    @Override
    public Uni<CashierTotalSales.ApiResponseCashierMonthlyTotalSales> findMonthlyTotalSalesByMerchant(
            Cashier.FindYearMonthTotalSalesByMerchant request) {
        String sql = "SELECT CAST(toYear(occurred_at) AS String) AS year, " +
                "formatDateTime(occurred_at, '%Y-%m') AS month, " +
                "CAST(SUM(total_amount) AS Int32) AS total_sales " +
                "FROM order_daily WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "AND toMonth(occurred_at) = " + request.getMonth() + " " +
                "AND merchant_id = '" + request.getMerchantId() + "' GROUP BY year, month";
        return statsQuery.query("cashier_total_sales", sql)
                .map(rows -> {
                    var b = CashierTotalSales.ApiResponseCashierMonthlyTotalSales.newBuilder()
                            .setStatus("success").setMessage("Found");
                    for (int i = 0; i < rows.size(); i++) {
                        var row = rows.getJsonObject(i);
                        b.addData(Cashier.CashierResponseMonthTotalSales.newBuilder()
                                .setYear(row.getString("year"))
                                .setMonth(row.getString("month"))
                                .setTotalSales(row.getInteger("total_sales", 0))
                                .build());
                    }
                    return b.build();
                })
                .onFailure().recoverWithItem(e ->
                        CashierTotalSales.ApiResponseCashierMonthlyTotalSales.newBuilder()
                                .setStatus("error").setMessage(e.getMessage()).build());
    }

    @Override
    public Uni<CashierTotalSales.ApiResponseCashierYearlyTotalSales> findYearlyTotalSalesByMerchant(
            Cashier.FindYearTotalSalesByMerchant request) {
        String sql = "SELECT CAST(toYear(occurred_at) AS String) AS year, " +
                "CAST(SUM(total_amount) AS Int32) AS total_sales " +
                "FROM order_daily WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "AND merchant_id = '" + request.getMerchantId() + "' GROUP BY year";
        return statsQuery.query("cashier_total_sales", sql)
                .map(rows -> {
                    var b = CashierTotalSales.ApiResponseCashierYearlyTotalSales.newBuilder()
                            .setStatus("success").setMessage("Found");
                    for (int i = 0; i < rows.size(); i++) {
                        var row = rows.getJsonObject(i);
                        b.addData(Cashier.CashierResponseYearTotalSales.newBuilder()
                                .setYear(row.getString("year"))
                                .setTotalSales(row.getInteger("total_sales", 0))
                                .build());
                    }
                    return b.build();
                })
                .onFailure().recoverWithItem(e ->
                        CashierTotalSales.ApiResponseCashierYearlyTotalSales.newBuilder()
                                .setStatus("error").setMessage(e.getMessage()).build());
    }
}
