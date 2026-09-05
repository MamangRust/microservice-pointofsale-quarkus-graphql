package com.sanedge.statsreader.handler;

import com.sanedge.statsreader.cache.StatsQueryService;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.transaction.stats.MutinyTransactionStatsStatusServiceGrpc;
import pb.transaction.stats.TransactionStatsStatus;

@GrpcService
@Singleton
public class TransactionStatsStatusHandler
        extends MutinyTransactionStatsStatusServiceGrpc.TransactionStatsStatusServiceImplBase {

    @Inject
    StatsQueryService statsQuery;

    @Override
    public Uni<TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess> findMonthlyTransactionStatusSuccess(
            pb.transaction.Transaction.FindMonthlyTransactionStatus request) {
        String sql = "SELECT CAST(toYear(occurred_at) AS String) AS year, " +
                "formatDateTime(occurred_at, '%Y-%m') AS month, " +
                "CAST(COUNT(*) AS Int32) AS total_success, " +
                "CAST(SUM(amount) AS Int32) AS total_amount " +
                "FROM transaction_daily WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "AND formatDateTime(occurred_at, '%m') = '" + String.format("%02d", request.getMonth()) + "' " +
                "AND status = 'success' GROUP BY year, month";
        return statsQuery.query("transactionstatsstatus", sql).map(rows -> {
            var builder = TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess.newBuilder()
                    .setStatus("success").setMessage("Found");
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.getJsonObject(i);
                builder.addData(TransactionStatsStatus.TransactionMonthStatusSuccessResponse.newBuilder()
                        .setYear(row.getString("year"))
                        .setMonth(row.getString("month"))
                        .setTotalSuccess(row.getInteger("total_success", 0))
                        .setTotalAmount(row.getInteger("total_amount", 0))
                        .build());
            }
            return builder.build();
        }).onFailure().recoverWithItem(e -> TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess.newBuilder()
                .setStatus("error").setMessage(e.getMessage()).build());
    }

    @Override
    public Uni<TransactionStatsStatus.ApiResponseTransactionYearStatusSuccess> findYearlyTransactionStatusSuccess(
            pb.transaction.Transaction.FindYearTransactionStatus request) {
        String sql = "SELECT CAST(toYear(occurred_at) AS String) AS year, " +
                "CAST(COUNT(*) AS Int32) AS total_success, " +
                "CAST(SUM(amount) AS Int32) AS total_amount " +
                "FROM transaction_daily WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "AND status = 'success' GROUP BY year";
        return statsQuery.query("transactionstatsstatus", sql).map(rows -> {
            var builder = TransactionStatsStatus.ApiResponseTransactionYearStatusSuccess.newBuilder()
                    .setStatus("success").setMessage("Found");
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.getJsonObject(i);
                builder.addData(TransactionStatsStatus.TransactionYearStatusSuccessResponse.newBuilder()
                        .setYear(row.getString("year"))
                        .setTotalSuccess(row.getInteger("total_success", 0))
                        .setTotalAmount(row.getInteger("total_amount", 0))
                        .build());
            }
            return builder.build();
        }).onFailure().recoverWithItem(e -> TransactionStatsStatus.ApiResponseTransactionYearStatusSuccess.newBuilder()
                .setStatus("error").setMessage(e.getMessage()).build());
    }

    @Override
    public Uni<TransactionStatsStatus.ApiResponseTransactionMonthStatusFailed> findMonthlyTransactionStatusFailed(
            pb.transaction.Transaction.FindMonthlyTransactionStatus request) {
        String sql = "SELECT CAST(toYear(occurred_at) AS String) AS year, " +
                "formatDateTime(occurred_at, '%Y-%m') AS month, " +
                "CAST(COUNT(*) AS Int32) AS total_failed, " +
                "CAST(SUM(amount) AS Int32) AS total_amount " +
                "FROM transaction_daily WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "AND formatDateTime(occurred_at, '%m') = '" + String.format("%02d", request.getMonth()) + "' " +
                "AND status != 'success' GROUP BY year, month";
        return statsQuery.query("transactionstatsstatus", sql).map(rows -> {
            var builder = TransactionStatsStatus.ApiResponseTransactionMonthStatusFailed.newBuilder()
                    .setStatus("success").setMessage("Found");
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.getJsonObject(i);
                builder.addData(TransactionStatsStatus.TransactionMonthStatusFailedResponse.newBuilder()
                        .setYear(row.getString("year"))
                        .setMonth(row.getString("month"))
                        .setTotalFailed(row.getInteger("total_failed", 0))
                        .setTotalAmount(row.getInteger("total_amount", 0))
                        .build());
            }
            return builder.build();
        }).onFailure().recoverWithItem(e -> TransactionStatsStatus.ApiResponseTransactionMonthStatusFailed.newBuilder()
                .setStatus("error").setMessage(e.getMessage()).build());
    }

    @Override
    public Uni<TransactionStatsStatus.ApiResponseTransactionYearStatusFailed> findYearlyTransactionStatusFailed(
            pb.transaction.Transaction.FindYearTransactionStatus request) {
        String sql = "SELECT CAST(toYear(occurred_at) AS String) AS year, " +
                "CAST(COUNT(*) AS Int32) AS total_failed, " +
                "CAST(SUM(amount) AS Int32) AS total_amount " +
                "FROM transaction_daily WHERE toYear(occurred_at) = " + request.getYear() + " " +
                "AND status != 'success' GROUP BY year";
        return statsQuery.query("transactionstatsstatus", sql).map(rows -> {
            var builder = TransactionStatsStatus.ApiResponseTransactionYearStatusFailed.newBuilder()
                    .setStatus("success").setMessage("Found");
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.getJsonObject(i);
                builder.addData(TransactionStatsStatus.TransactionYearStatusFailedResponse.newBuilder()
                        .setYear(row.getString("year"))
                        .setTotalFailed(row.getInteger("total_failed", 0))
                        .setTotalAmount(row.getInteger("total_amount", 0))
                        .build());
            }
            return builder.build();
        }).onFailure().recoverWithItem(e -> TransactionStatsStatus.ApiResponseTransactionYearStatusFailed.newBuilder()
                .setStatus("error").setMessage(e.getMessage()).build());
    }

    // Card number variants — not available in stats reader (no card_number column)
    @Override
    public Uni<TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess> findMonthlyTransactionStatusSuccessByCardNumber(
            pb.transaction.Transaction.FindMonthlyTransactionStatusCardNumber request) {
        return Uni.createFrom().item(TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess.newBuilder()
                .setStatus("success").setMessage("Card number filter not available").build());
    }

    @Override
    public Uni<TransactionStatsStatus.ApiResponseTransactionYearStatusSuccess> findYearlyTransactionStatusSuccessByCardNumber(
            pb.transaction.Transaction.FindYearTransactionStatusCardNumber request) {
        return Uni.createFrom().item(TransactionStatsStatus.ApiResponseTransactionYearStatusSuccess.newBuilder()
                .setStatus("success").setMessage("Card number filter not available").build());
    }

    @Override
    public Uni<TransactionStatsStatus.ApiResponseTransactionMonthStatusFailed> findMonthlyTransactionStatusFailedByCardNumber(
            pb.transaction.Transaction.FindMonthlyTransactionStatusCardNumber request) {
        return Uni.createFrom().item(TransactionStatsStatus.ApiResponseTransactionMonthStatusFailed.newBuilder()
                .setStatus("success").setMessage("Card number filter not available").build());
    }

    @Override
    public Uni<TransactionStatsStatus.ApiResponseTransactionYearStatusFailed> findYearlyTransactionStatusFailedByCardNumber(
            pb.transaction.Transaction.FindYearTransactionStatusCardNumber request) {
        return Uni.createFrom().item(TransactionStatsStatus.ApiResponseTransactionYearStatusFailed.newBuilder()
                .setStatus("success").setMessage("Card number filter not available").build());
    }
}
