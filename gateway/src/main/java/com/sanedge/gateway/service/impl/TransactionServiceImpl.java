package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.TransactionDto;
import com.sanedge.gateway.service.TransactionService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TransactionServiceImpl implements TransactionService {

    private static final Logger LOG = Logger.getLogger(TransactionServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("transaction")
    pb.transaction.MutinyTransactionQueryServiceGrpc.MutinyTransactionQueryServiceStub transactionQueryService;

    @GrpcClient("transaction")
    pb.transaction.MutinyTransactionCommandServiceGrpc.MutinyTransactionCommandServiceStub transactionCommandService;

    @GrpcClient("stats-reader")
    pb.transaction.stats.MutinyTransactionStatsAmountServiceGrpc.MutinyTransactionStatsAmountServiceStub transactionStatsAmountService;

    @GrpcClient("stats-reader")
    pb.transaction.stats.MutinyTransactionStatsMethodServiceGrpc.MutinyTransactionStatsMethodServiceStub transactionStatsMethodService;

    @GrpcClient("stats-reader")
    pb.transaction.stats.MutinyTransactionStatsStatusServiceGrpc.MutinyTransactionStatsStatusServiceStub transactionStatsStatusService;

    @Override
    public Uni<TransactionDto.ApiResponsePaginationTransaction> listTransactions(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("transaction.listTransactions", () -> transactionQueryService.findAllTransaction(pb.transaction.TransactionQuery.FindAllTransactionRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(TransactionDto.ApiResponsePaginationTransaction::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list transactions: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponsePaginationTransaction> listTransactionsByCardNumber(String cardNumber, int page, int size, String search) {
        return telemetryHelper.traceAndMetric("transaction.listTransactionsByCardNumber", () -> transactionQueryService.findAllTransactionByCardNumber(pb.transaction.TransactionQuery.FindAllTransactionCardNumberRequest.newBuilder()
                .setCardNumber(cardNumber)
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(TransactionDto.ApiResponsePaginationTransaction::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list transactions by card number: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponseTransaction> getTransaction(int id) {
        return telemetryHelper.traceAndMetric("transaction.getTransaction", () -> transactionQueryService.findByIdTransaction(pb.transaction.Transaction.FindByIdTransactionRequest.newBuilder()
                .setTransactionId(id)
                .build())
                .map(TransactionDto.ApiResponseTransaction::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get transaction " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponseTransactions> getTransactionsByMerchant(int merchantId) {
        return telemetryHelper.traceAndMetric("transaction.getTransactionsByMerchant", () -> transactionQueryService.findTransactionByMerchantId(pb.transaction.TransactionQuery.FindTransactionByMerchantIdRequest.newBuilder()
                .setMerchantId(merchantId)
                .build())
                .map(TransactionDto.ApiResponseTransactions::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get transactions by merchant " + merchantId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponsePaginationTransactionDeleteAt> getActiveTransactions(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("transaction.getActiveTransactions", () -> transactionQueryService.findByActiveTransaction(pb.transaction.TransactionQuery.FindAllTransactionRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(TransactionDto.ApiResponsePaginationTransactionDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list active transactions: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponsePaginationTransactionDeleteAt> getTrashedTransactions(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("transaction.getTrashedTransactions", () -> transactionQueryService.findByTrashedTransaction(pb.transaction.TransactionQuery.FindAllTransactionRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(TransactionDto.ApiResponsePaginationTransactionDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list trashed transactions: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponseTransaction> createTransaction(TransactionDto.CreateTransactionRequest body) {
        return telemetryHelper.traceAndMetric("transaction.createTransaction", () -> transactionCommandService.createTransaction(pb.transaction.TransactionCommand.CreateTransactionRequest.newBuilder()
                .setCardNumber(body.cardNumber())
                .setAmount(body.amount())
                .setPaymentMethod(body.paymentMethod())
                .setMerchantId(body.merchantId())
                .build())
                .map(TransactionDto.ApiResponseTransaction::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create transaction: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponseTransaction> updateTransaction(int id, TransactionDto.UpdateTransactionRequest body) {
        return telemetryHelper.traceAndMetric("transaction.updateTransaction", () -> transactionCommandService.updateTransaction(pb.transaction.TransactionCommand.UpdateTransactionRequest.newBuilder()
                .setTransactionId(id)
                .setCardNumber(body.cardNumber())
                .setAmount(body.amount())
                .setPaymentMethod(body.paymentMethod())
                .setMerchantId(body.merchantId())
                .build())
                .map(TransactionDto.ApiResponseTransaction::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update transaction " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponseTransactionDeleteAt> deleteTransaction(int id) {
        return telemetryHelper.traceAndMetric("transaction.deleteTransaction", () -> transactionCommandService.trashedTransaction(pb.transaction.Transaction.FindByIdTransactionRequest.newBuilder()
                .setTransactionId(id)
                .build())
                .map(TransactionDto.ApiResponseTransactionDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete transaction " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponseTransactionDeleteAt> restoreTransaction(int id) {
        return telemetryHelper.traceAndMetric("transaction.restoreTransaction", () -> transactionCommandService.restoreTransaction(pb.transaction.Transaction.FindByIdTransactionRequest.newBuilder()
                .setTransactionId(id)
                .build())
                .map(TransactionDto.ApiResponseTransactionDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore transaction " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponseTransactionDelete> deleteTransactionPermanent(int id) {
        return telemetryHelper.traceAndMetric("transaction.deleteTransactionPermanent", () -> transactionCommandService.deleteTransactionPermanent(pb.transaction.Transaction.FindByIdTransactionRequest.newBuilder()
                .setTransactionId(id)
                .build())
                .map(TransactionDto.ApiResponseTransactionDelete::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete transaction " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponseTransactionAll> restoreAllTransaction() {
        return telemetryHelper.traceAndMetric("transaction.restoreAllTransactions", () -> transactionCommandService.restoreAllTransaction(com.google.protobuf.Empty.getDefaultInstance())
                .map(TransactionDto.ApiResponseTransactionAll::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all transactions: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponseTransactionAll> deleteAllTransactionPermanent() {
        return telemetryHelper.traceAndMetric("transaction.deleteAllTransactionsPermanent", () -> transactionCommandService.deleteAllTransactionPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .map(TransactionDto.ApiResponseTransactionAll::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all transactions: " + throwable.getMessage(), throwable)));
    }

    // Statistics - Amount

    @Override
    public Uni<TransactionDto.ApiResponseTransactionMonthAmount> getMonthlyAmounts(int year) {
        return telemetryHelper.traceAndMetric("transaction.getMonthlyAmounts", () -> transactionStatsAmountService.findMonthlyAmounts(pb.transaction.Transaction.FindYearTransactionStatus.newBuilder()
                .setYear(year)
                .build())
                .map(TransactionDto.ApiResponseTransactionMonthAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly amounts: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponseTransactionYearAmount> getYearlyAmounts(int year) {
        return telemetryHelper.traceAndMetric("transaction.getYearlyAmounts", () -> transactionStatsAmountService.findYearlyAmounts(pb.transaction.Transaction.FindYearTransactionStatus.newBuilder()
                .setYear(year)
                .build())
                .map(TransactionDto.ApiResponseTransactionYearAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly amounts: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponseTransactionMonthAmount> getMonthlyAmountsByCardNumber(String cardNumber, int year) {
        return telemetryHelper.traceAndMetric("transaction.getMonthlyAmountsByCardNumber", () -> transactionStatsAmountService.findMonthlyAmountsByCardNumber(pb.transaction.Transaction.FindByYearCardNumberTransactionRequest.newBuilder()
                .setCardNumber(cardNumber)
                .setYear(year)
                .build())
                .map(TransactionDto.ApiResponseTransactionMonthAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly amounts by card number: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponseTransactionYearAmount> getYearlyAmountsByCardNumber(String cardNumber, int year) {
        return telemetryHelper.traceAndMetric("transaction.getYearlyAmountsByCardNumber", () -> transactionStatsAmountService.findYearlyAmountsByCardNumber(pb.transaction.Transaction.FindByYearCardNumberTransactionRequest.newBuilder()
                .setCardNumber(cardNumber)
                .setYear(year)
                .build())
                .map(TransactionDto.ApiResponseTransactionYearAmount::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly amounts by card number: " + throwable.getMessage(), throwable)));
    }

    // Statistics - Method

    @Override
    public Uni<TransactionDto.ApiResponseTransactionMonthMethod> getMonthlyPaymentMethods(int year) {
        return telemetryHelper.traceAndMetric("transaction.getMonthlyPaymentMethods", () -> transactionStatsMethodService.findMonthlyPaymentMethods(pb.transaction.Transaction.FindYearTransactionStatus.newBuilder()
                .setYear(year)
                .build())
                .map(TransactionDto.ApiResponseTransactionMonthMethod::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly payment methods: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponseTransactionYearMethod> getYearlyPaymentMethods(int year) {
        return telemetryHelper.traceAndMetric("transaction.getYearlyPaymentMethods", () -> transactionStatsMethodService.findYearlyPaymentMethods(pb.transaction.Transaction.FindYearTransactionStatus.newBuilder()
                .setYear(year)
                .build())
                .map(TransactionDto.ApiResponseTransactionYearMethod::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly payment methods: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponseTransactionMonthMethod> getMonthlyPaymentMethodsByCardNumber(String cardNumber, int year) {
        return telemetryHelper.traceAndMetric("transaction.getMonthlyPaymentMethodsByCardNumber", () -> transactionStatsMethodService.findMonthlyPaymentMethodsByCardNumber(pb.transaction.Transaction.FindByYearCardNumberTransactionRequest.newBuilder()
                .setCardNumber(cardNumber)
                .setYear(year)
                .build())
                .map(TransactionDto.ApiResponseTransactionMonthMethod::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly payment methods by card number: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponseTransactionYearMethod> getYearlyPaymentMethodsByCardNumber(String cardNumber, int year) {
        return telemetryHelper.traceAndMetric("transaction.getYearlyPaymentMethodsByCardNumber", () -> transactionStatsMethodService.findYearlyPaymentMethodsByCardNumber(pb.transaction.Transaction.FindByYearCardNumberTransactionRequest.newBuilder()
                .setCardNumber(cardNumber)
                .setYear(year)
                .build())
                .map(TransactionDto.ApiResponseTransactionYearMethod::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly payment methods by card number: " + throwable.getMessage(), throwable)));
    }

    // Statistics - Status

    @Override
    public Uni<TransactionDto.ApiResponseTransactionMonthStatusSuccess> getMonthlyTransactionStatusSuccess(int year, int month) {
        return telemetryHelper.traceAndMetric("transaction.getMonthlyTransactionStatusSuccess", () -> transactionStatsStatusService.findMonthlyTransactionStatusSuccess(pb.transaction.Transaction.FindMonthlyTransactionStatus.newBuilder()
                .setYear(year)
                .setMonth(month)
                .build())
                .map(TransactionDto.ApiResponseTransactionMonthStatusSuccess::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly status success: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponseTransactionYearStatusSuccess> getYearlyTransactionStatusSuccess(int year) {
        return telemetryHelper.traceAndMetric("transaction.getYearlyTransactionStatusSuccess", () -> transactionStatsStatusService.findYearlyTransactionStatusSuccess(pb.transaction.Transaction.FindYearTransactionStatus.newBuilder()
                .setYear(year)
                .build())
                .map(TransactionDto.ApiResponseTransactionYearStatusSuccess::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly status success: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponseTransactionMonthStatusFailed> getMonthlyTransactionStatusFailed(int year, int month) {
        return telemetryHelper.traceAndMetric("transaction.getMonthlyTransactionStatusFailed", () -> transactionStatsStatusService.findMonthlyTransactionStatusFailed(pb.transaction.Transaction.FindMonthlyTransactionStatus.newBuilder()
                .setYear(year)
                .setMonth(month)
                .build())
                .map(TransactionDto.ApiResponseTransactionMonthStatusFailed::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly status failed: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponseTransactionYearStatusFailed> getYearlyTransactionStatusFailed(int year) {
        return telemetryHelper.traceAndMetric("transaction.getYearlyTransactionStatusFailed", () -> transactionStatsStatusService.findYearlyTransactionStatusFailed(pb.transaction.Transaction.FindYearTransactionStatus.newBuilder()
                .setYear(year)
                .build())
                .map(TransactionDto.ApiResponseTransactionYearStatusFailed::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly status failed: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponseTransactionMonthStatusSuccess> getMonthlyTransactionStatusSuccessByCardNumber(String cardNumber, int year, int month) {
        return telemetryHelper.traceAndMetric("transaction.getMonthlyTransactionStatusSuccessByCardNumber", () -> transactionStatsStatusService.findMonthlyTransactionStatusSuccessByCardNumber(pb.transaction.Transaction.FindMonthlyTransactionStatusCardNumber.newBuilder()
                .setCardNumber(cardNumber)
                .setYear(year)
                .setMonth(month)
                .build())
                .map(TransactionDto.ApiResponseTransactionMonthStatusSuccess::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly status success by card number: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponseTransactionYearStatusSuccess> getYearlyTransactionStatusSuccessByCardNumber(String cardNumber, int year) {
        return telemetryHelper.traceAndMetric("transaction.getYearlyTransactionStatusSuccessByCardNumber", () -> transactionStatsStatusService.findYearlyTransactionStatusSuccessByCardNumber(pb.transaction.Transaction.FindYearTransactionStatusCardNumber.newBuilder()
                .setCardNumber(cardNumber)
                .setYear(year)
                .build())
                .map(TransactionDto.ApiResponseTransactionYearStatusSuccess::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly status success by card number: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponseTransactionMonthStatusFailed> getMonthlyTransactionStatusFailedByCardNumber(String cardNumber, int year, int month) {
        return telemetryHelper.traceAndMetric("transaction.getMonthlyTransactionStatusFailedByCardNumber", () -> transactionStatsStatusService.findMonthlyTransactionStatusFailedByCardNumber(pb.transaction.Transaction.FindMonthlyTransactionStatusCardNumber.newBuilder()
                .setCardNumber(cardNumber)
                .setYear(year)
                .setMonth(month)
                .build())
                .map(TransactionDto.ApiResponseTransactionMonthStatusFailed::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly status failed by card number: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TransactionDto.ApiResponseTransactionYearStatusFailed> getYearlyTransactionStatusFailedByCardNumber(String cardNumber, int year) {
        return telemetryHelper.traceAndMetric("transaction.getYearlyTransactionStatusFailedByCardNumber", () -> transactionStatsStatusService.findYearlyTransactionStatusFailedByCardNumber(pb.transaction.Transaction.FindYearTransactionStatusCardNumber.newBuilder()
                .setCardNumber(cardNumber)
                .setYear(year)
                .build())
                .map(TransactionDto.ApiResponseTransactionYearStatusFailed::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly status failed by card number: " + throwable.getMessage(), throwable)));
    }
}
