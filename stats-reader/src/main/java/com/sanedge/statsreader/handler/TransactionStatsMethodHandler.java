package com.sanedge.statsreader.handler;

import com.sanedge.statsreader.cache.StatsQueryService;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.transaction.stats.MutinyTransactionStatsMethodServiceGrpc;
import pb.transaction.stats.TransactionStatsMethod;

@GrpcService
@Singleton
public class TransactionStatsMethodHandler
        extends MutinyTransactionStatsMethodServiceGrpc.TransactionStatsMethodServiceImplBase {

    @Inject
    StatsQueryService statsQuery;

    @Override
    public Uni<TransactionStatsMethod.ApiResponseTransactionMonthMethod> findMonthlyPaymentMethods(
            pb.transaction.Transaction.FindYearTransactionStatus request) {
        String sql = "SELECT formatDateTime(occurred_at, '%Y-%m') AS month, " +
                "payment_method, CAST(COUNT(*) AS Int32) AS total_transactions, " +
                "CAST(SUM(amount) AS Int32) AS total_amount " +
                "FROM transaction_daily WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "GROUP BY month, payment_method ORDER BY month, payment_method";
        return statsQuery.query("transactionstatsmethod", sql).map(rows -> {
            var builder = TransactionStatsMethod.ApiResponseTransactionMonthMethod.newBuilder()
                    .setStatus("success").setMessage("Found");
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.getJsonObject(i);
                builder.addData(TransactionStatsMethod.TransactionMonthMethodResponse.newBuilder()
                        .setMonth(row.getString("month"))
                        .setPaymentMethod(row.getString("payment_method"))
                        .setTotalTransactions(row.getInteger("total_transactions", 0))
                        .setTotalAmount(row.getInteger("total_amount", 0))
                        .build());
            }
            return builder.build();
        }).onFailure().recoverWithItem(e -> TransactionStatsMethod.ApiResponseTransactionMonthMethod.newBuilder()
                .setStatus("error").setMessage(e.getMessage()).build());
    }

    @Override
    public Uni<TransactionStatsMethod.ApiResponseTransactionYearMethod> findYearlyPaymentMethods(
            pb.transaction.Transaction.FindYearTransactionStatus request) {
        String sql = "SELECT CAST(toYear(occurred_at) AS String) AS year, " +
                "payment_method, CAST(COUNT(*) AS Int32) AS total_transactions, " +
                "CAST(SUM(amount) AS Int32) AS total_amount " +
                "FROM transaction_daily WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "GROUP BY year, payment_method ORDER BY payment_method";
        return statsQuery.query("transactionstatsmethod", sql).map(rows -> {
            var builder = TransactionStatsMethod.ApiResponseTransactionYearMethod.newBuilder()
                    .setStatus("success").setMessage("Found");
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.getJsonObject(i);
                builder.addData(TransactionStatsMethod.TransactionYearMethodResponse.newBuilder()
                        .setYear(row.getString("year"))
                        .setPaymentMethod(row.getString("payment_method"))
                        .setTotalTransactions(row.getInteger("total_transactions", 0))
                        .setTotalAmount(row.getInteger("total_amount", 0))
                        .build());
            }
            return builder.build();
        }).onFailure().recoverWithItem(e -> TransactionStatsMethod.ApiResponseTransactionYearMethod.newBuilder()
                .setStatus("error").setMessage(e.getMessage()).build());
    }

    @Override
    public Uni<TransactionStatsMethod.ApiResponseTransactionMonthMethod> findMonthlyPaymentMethodsByCardNumber(
            pb.transaction.Transaction.FindByYearCardNumberTransactionRequest request) {
        return Uni.createFrom().item(TransactionStatsMethod.ApiResponseTransactionMonthMethod.newBuilder()
                .setStatus("success").setMessage("Card number filter not available in stats reader").build());
    }

    @Override
    public Uni<TransactionStatsMethod.ApiResponseTransactionYearMethod> findYearlyPaymentMethodsByCardNumber(
            pb.transaction.Transaction.FindByYearCardNumberTransactionRequest request) {
        return Uni.createFrom().item(TransactionStatsMethod.ApiResponseTransactionYearMethod.newBuilder()
                .setStatus("success").setMessage("Card number filter not available in stats reader").build());
    }
}
