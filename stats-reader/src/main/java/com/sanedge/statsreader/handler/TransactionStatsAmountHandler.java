package com.sanedge.statsreader.handler;

import com.sanedge.statsreader.cache.StatsQueryService;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.transaction.stats.MutinyTransactionStatsAmountServiceGrpc;
import pb.transaction.stats.TransactionStatsAmount;

@GrpcService
@Singleton
public class TransactionStatsAmountHandler
        extends MutinyTransactionStatsAmountServiceGrpc.TransactionStatsAmountServiceImplBase {

    @Inject
    StatsQueryService statsQuery;

    @Override
    public Uni<TransactionStatsAmount.ApiResponseTransactionMonthAmount> findMonthlyAmounts(
            pb.transaction.Transaction.FindYearTransactionStatus request) {
        String sql = "SELECT formatDateTime(occurred_at, '%Y-%m') AS month, " +
                "CAST(SUM(amount) AS Int32) AS total_amount " +
                "FROM transaction_daily " +
                "WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "GROUP BY month ORDER BY month";
        return statsQuery.query("tx_amount", sql)
                .map(rows -> {
                    TransactionStatsAmount.ApiResponseTransactionMonthAmount.Builder builder =
                            TransactionStatsAmount.ApiResponseTransactionMonthAmount.newBuilder()
                                    .setStatus("success").setMessage("Found");
                    for (int i = 0; i < rows.size(); i++) {
                        var row = rows.getJsonObject(i);
                        builder.addData(TransactionStatsAmount.TransactionMonthAmountResponse.newBuilder()
                                .setMonth(row.getString("month"))
                                .setTotalAmount(row.getInteger("total_amount", 0))
                                .build());
                    }
                    return builder.build();
                })
                .onFailure().recoverWithItem(e ->
                        TransactionStatsAmount.ApiResponseTransactionMonthAmount.newBuilder()
                                .setStatus("error").setMessage(e.getMessage()).build());
    }

    @Override
    public Uni<TransactionStatsAmount.ApiResponseTransactionYearAmount> findYearlyAmounts(
            pb.transaction.Transaction.FindYearTransactionStatus request) {
        String sql = "SELECT CAST(toYear(occurred_at) AS String) AS year, " +
                "CAST(SUM(amount) AS Int32) AS total_amount " +
                "FROM transaction_daily " +
                "WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "GROUP BY year";
        return statsQuery.query("tx_amount", sql)
                .map(rows -> {
                    TransactionStatsAmount.ApiResponseTransactionYearAmount.Builder builder =
                            TransactionStatsAmount.ApiResponseTransactionYearAmount.newBuilder()
                                    .setStatus("success").setMessage("Found");
                    for (int i = 0; i < rows.size(); i++) {
                        var row = rows.getJsonObject(i);
                        builder.addData(TransactionStatsAmount.TransactionYearlyAmountResponse.newBuilder()
                                .setYear(row.getString("year"))
                                .setTotalAmount(row.getInteger("total_amount", 0))
                                .build());
                    }
                    return builder.build();
                })
                .onFailure().recoverWithItem(e ->
                        TransactionStatsAmount.ApiResponseTransactionYearAmount.newBuilder()
                                .setStatus("error").setMessage(e.getMessage()).build());
    }

    @Override
    public Uni<TransactionStatsAmount.ApiResponseTransactionMonthAmount> findMonthlyAmountsByCardNumber(
            pb.transaction.Transaction.FindByYearCardNumberTransactionRequest request) {
        return Uni.createFrom().item(TransactionStatsAmount.ApiResponseTransactionMonthAmount.newBuilder()
                .setStatus("success").setMessage("Card number filter not available in stats reader").build());
    }

    @Override
    public Uni<TransactionStatsAmount.ApiResponseTransactionYearAmount> findYearlyAmountsByCardNumber(
            pb.transaction.Transaction.FindByYearCardNumberTransactionRequest request) {
        return Uni.createFrom().item(TransactionStatsAmount.ApiResponseTransactionYearAmount.newBuilder()
                .setStatus("success").setMessage("Card number filter not available in stats reader").build());
    }
}
