package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.TransactionDto;
import io.smallrye.mutiny.Uni;

public interface TransactionService {
    Uni<TransactionDto.ApiResponsePaginationTransaction> listTransactions(int page, int size, String search);
    Uni<TransactionDto.ApiResponsePaginationTransaction> listTransactionsByCardNumber(String cardNumber, int page, int size, String search);
    Uni<TransactionDto.ApiResponseTransaction> getTransaction(int id);
    Uni<TransactionDto.ApiResponseTransactions> getTransactionsByMerchant(int merchantId);
    Uni<TransactionDto.ApiResponsePaginationTransactionDeleteAt> getActiveTransactions(int page, int size, String search);
    Uni<TransactionDto.ApiResponsePaginationTransactionDeleteAt> getTrashedTransactions(int page, int size, String search);
    Uni<TransactionDto.ApiResponseTransaction> createTransaction(TransactionDto.CreateTransactionRequest body);
    Uni<TransactionDto.ApiResponseTransaction> updateTransaction(int id, TransactionDto.UpdateTransactionRequest body);
    Uni<TransactionDto.ApiResponseTransactionDeleteAt> deleteTransaction(int id);
    Uni<TransactionDto.ApiResponseTransactionDeleteAt> restoreTransaction(int id);
    Uni<TransactionDto.ApiResponseTransactionDelete> deleteTransactionPermanent(int id);
    Uni<TransactionDto.ApiResponseTransactionAll> restoreAllTransaction();
    Uni<TransactionDto.ApiResponseTransactionAll> deleteAllTransactionPermanent();

    // Statistics - Amount
    Uni<TransactionDto.ApiResponseTransactionMonthAmount> getMonthlyAmounts(int year);
    Uni<TransactionDto.ApiResponseTransactionYearAmount> getYearlyAmounts(int year);
    Uni<TransactionDto.ApiResponseTransactionMonthAmount> getMonthlyAmountsByCardNumber(String cardNumber, int year);
    Uni<TransactionDto.ApiResponseTransactionYearAmount> getYearlyAmountsByCardNumber(String cardNumber, int year);

    // Statistics - Method
    Uni<TransactionDto.ApiResponseTransactionMonthMethod> getMonthlyPaymentMethods(int year);
    Uni<TransactionDto.ApiResponseTransactionYearMethod> getYearlyPaymentMethods(int year);
    Uni<TransactionDto.ApiResponseTransactionMonthMethod> getMonthlyPaymentMethodsByCardNumber(String cardNumber, int year);
    Uni<TransactionDto.ApiResponseTransactionYearMethod> getYearlyPaymentMethodsByCardNumber(String cardNumber, int year);

    // Statistics - Status
    Uni<TransactionDto.ApiResponseTransactionMonthStatusSuccess> getMonthlyTransactionStatusSuccess(int year, int month);
    Uni<TransactionDto.ApiResponseTransactionYearStatusSuccess> getYearlyTransactionStatusSuccess(int year);
    Uni<TransactionDto.ApiResponseTransactionMonthStatusFailed> getMonthlyTransactionStatusFailed(int year, int month);
    Uni<TransactionDto.ApiResponseTransactionYearStatusFailed> getYearlyTransactionStatusFailed(int year);
    Uni<TransactionDto.ApiResponseTransactionMonthStatusSuccess> getMonthlyTransactionStatusSuccessByCardNumber(String cardNumber, int year, int month);
    Uni<TransactionDto.ApiResponseTransactionYearStatusSuccess> getYearlyTransactionStatusSuccessByCardNumber(String cardNumber, int year);
    Uni<TransactionDto.ApiResponseTransactionMonthStatusFailed> getMonthlyTransactionStatusFailedByCardNumber(String cardNumber, int year, int month);
    Uni<TransactionDto.ApiResponseTransactionYearStatusFailed> getYearlyTransactionStatusFailedByCardNumber(String cardNumber, int year);
}
