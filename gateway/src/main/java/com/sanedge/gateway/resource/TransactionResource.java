package com.sanedge.gateway.resource;

import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

import com.sanedge.gateway.dto.TransactionDto;
import com.sanedge.gateway.service.TransactionService;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

@GraphQLApi
public class TransactionResource {

        @Inject
        TransactionService transactionService;

        @Query("transactions")
        @Description("List all transactions")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<TransactionDto.ApiResponsePaginationTransaction> listTransactions(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return transactionService.listTransactions(page, size, search);
        }

        @Query("transactionsByCardNumber")
        @Description("List transactions by card number")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<TransactionDto.ApiResponsePaginationTransaction> listTransactionsByCardNumber(
                        @Name("cardNumber") String cardNumber,
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return transactionService.listTransactionsByCardNumber(cardNumber, page, size, search);
        }

        @Query("transaction")
        @Description("Get transaction by ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<TransactionDto.ApiResponseTransaction> getTransaction(@Name("id") int id) {
                return transactionService.getTransaction(id);
        }

        @Query("transactionsByMerchant")
        @Description("Get transactions by merchant ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<TransactionDto.ApiResponseTransactions> getTransactionsByMerchant(
                        @Name("merchantId") int merchantId) {
                return transactionService.getTransactionsByMerchant(merchantId);
        }

        @Query("activeTransactions")
        @Description("Get active transactions")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<TransactionDto.ApiResponsePaginationTransactionDeleteAt> getActiveTransactions(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return transactionService.getActiveTransactions(page, size, search);
        }

        @Query("trashedTransactions")
        @Description("Get trashed transactions")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<TransactionDto.ApiResponsePaginationTransactionDeleteAt> getTrashedTransactions(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return transactionService.getTrashedTransactions(page, size, search);
        }

        @Mutation("createTransaction")
        @Description("Create a new transaction")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<TransactionDto.ApiResponseTransaction> createTransaction(
                        @Name("body") TransactionDto.CreateTransactionRequest body) {
                return transactionService.createTransaction(body);
        }

        @Mutation("updateTransaction")
        @Description("Update transaction")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<TransactionDto.ApiResponseTransaction> updateTransaction(@Name("id") int id,
                        @Name("body") TransactionDto.UpdateTransactionRequest body) {
                return transactionService.updateTransaction(id, body);
        }

        @Mutation("deleteTransaction")
        @Description("Soft-delete a transaction")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<TransactionDto.ApiResponseTransactionDeleteAt> deleteTransaction(@Name("id") int id) {
                return transactionService.deleteTransaction(id);
        }

        @Mutation("restoreTransaction")
        @Description("Restore a soft-deleted transaction")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<TransactionDto.ApiResponseTransactionDeleteAt> restoreTransaction(@Name("id") int id) {
                return transactionService.restoreTransaction(id);
        }

        @Mutation("deleteTransactionPermanent")
        @Description("Permanently delete a transaction")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<TransactionDto.ApiResponseTransactionDelete> deleteTransactionPermanent(@Name("id") int id) {
                return transactionService.deleteTransactionPermanent(id);
        }

        @Mutation("restoreAllTransactions")
        @Description("Restore all soft-deleted transactions")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<TransactionDto.ApiResponseTransactionAll> restoreAllTransaction() {
                return transactionService.restoreAllTransaction();
        }

        @Mutation("deleteAllTransactionsPermanent")
        @Description("Permanently delete all soft-deleted transactions")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<TransactionDto.ApiResponseTransactionAll> deleteAllTransactionPermanent() {
                return transactionService.deleteAllTransactionPermanent();
        }

        // --- Statistics - Amount ---

        @Query("transactionMonthlyAmounts")
        @Description("Get monthly transaction amount statistics")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<TransactionDto.ApiResponseTransactionMonthAmount> getMonthlyAmounts(@Name("year") int year) {
                return transactionService.getMonthlyAmounts(year);
        }

        @Query("transactionYearlyAmounts")
        @Description("Get yearly transaction amount statistics")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<TransactionDto.ApiResponseTransactionYearAmount> getYearlyAmounts(@Name("year") int year) {
                return transactionService.getYearlyAmounts(year);
        }

        @Query("transactionMonthlyAmountsByCardNumber")
        @Description("Get monthly transaction amount statistics by card number")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<TransactionDto.ApiResponseTransactionMonthAmount> getMonthlyAmountsByCardNumber(
                        @Name("cardNumber") String cardNumber,
                        @Name("year") int year) {
                return transactionService.getMonthlyAmountsByCardNumber(cardNumber, year);
        }

        @Query("transactionYearlyAmountsByCardNumber")
        @Description("Get yearly transaction amount statistics by card number")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<TransactionDto.ApiResponseTransactionYearAmount> getYearlyAmountsByCardNumber(
                        @Name("cardNumber") String cardNumber,
                        @Name("year") int year) {
                return transactionService.getYearlyAmountsByCardNumber(cardNumber, year);
        }

        // --- Statistics - Method ---

        @Query("transactionMonthlyPaymentMethods")
        @Description("Get monthly payment method statistics")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<TransactionDto.ApiResponseTransactionMonthMethod> getMonthlyPaymentMethods(@Name("year") int year) {
                return transactionService.getMonthlyPaymentMethods(year);
        }

        @Query("transactionYearlyPaymentMethods")
        @Description("Get yearly payment method statistics")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<TransactionDto.ApiResponseTransactionYearMethod> getYearlyPaymentMethods(@Name("year") int year) {
                return transactionService.getYearlyPaymentMethods(year);
        }

        @Query("transactionMonthlyPaymentMethodsByCardNumber")
        @Description("Get monthly payment method statistics by card number")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<TransactionDto.ApiResponseTransactionMonthMethod> getMonthlyPaymentMethodsByCardNumber(
                        @Name("cardNumber") String cardNumber,
                        @Name("year") int year) {
                return transactionService.getMonthlyPaymentMethodsByCardNumber(cardNumber, year);
        }

        @Query("transactionYearlyPaymentMethodsByCardNumber")
        @Description("Get yearly payment method statistics by card number")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<TransactionDto.ApiResponseTransactionYearMethod> getYearlyPaymentMethodsByCardNumber(
                        @Name("cardNumber") String cardNumber,
                        @Name("year") int year) {
                return transactionService.getYearlyPaymentMethodsByCardNumber(cardNumber, year);
        }

        // --- Statistics - Status ---

        @Query("transactionMonthlyStatusSuccess")
        @Description("Get monthly successful transaction status statistics")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<TransactionDto.ApiResponseTransactionMonthStatusSuccess> getMonthlyTransactionStatusSuccess(
                        @Name("year") int year,
                        @Name("month") int month) {
                return transactionService.getMonthlyTransactionStatusSuccess(year, month);
        }

        @Query("transactionYearlyStatusSuccess")
        @Description("Get yearly successful transaction status statistics")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<TransactionDto.ApiResponseTransactionYearStatusSuccess> getYearlyTransactionStatusSuccess(
                        @Name("year") int year) {
                return transactionService.getYearlyTransactionStatusSuccess(year);
        }

        @Query("transactionMonthlyStatusFailed")
        @Description("Get monthly failed transaction status statistics")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<TransactionDto.ApiResponseTransactionMonthStatusFailed> getMonthlyTransactionStatusFailed(
                        @Name("year") int year,
                        @Name("month") int month) {
                return transactionService.getMonthlyTransactionStatusFailed(year, month);
        }

        @Query("transactionYearlyStatusFailed")
        @Description("Get yearly failed transaction status statistics")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<TransactionDto.ApiResponseTransactionYearStatusFailed> getYearlyTransactionStatusFailed(
                        @Name("year") int year) {
                return transactionService.getYearlyTransactionStatusFailed(year);
        }

        @Query("transactionMonthlyStatusSuccessByCardNumber")
        @Description("Get monthly successful transaction status statistics by card number")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<TransactionDto.ApiResponseTransactionMonthStatusSuccess> getMonthlyTransactionStatusSuccessByCardNumber(
                        @Name("cardNumber") String cardNumber,
                        @Name("year") int year,
                        @Name("month") int month) {
                return transactionService.getMonthlyTransactionStatusSuccessByCardNumber(cardNumber, year, month);
        }

        @Query("transactionYearlyStatusSuccessByCardNumber")
        @Description("Get yearly successful transaction status statistics by card number")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<TransactionDto.ApiResponseTransactionYearStatusSuccess> getYearlyTransactionStatusSuccessByCardNumber(
                        @Name("cardNumber") String cardNumber,
                        @Name("year") int year) {
                return transactionService.getYearlyTransactionStatusSuccessByCardNumber(cardNumber, year);
        }

        @Query("transactionMonthlyStatusFailedByCardNumber")
        @Description("Get monthly failed transaction status statistics by card number")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<TransactionDto.ApiResponseTransactionMonthStatusFailed> getMonthlyTransactionStatusFailedByCardNumber(
                        @Name("cardNumber") String cardNumber,
                        @Name("year") int year,
                        @Name("month") int month) {
                return transactionService.getMonthlyTransactionStatusFailedByCardNumber(cardNumber, year, month);
        }

        @Query("transactionYearlyStatusFailedByCardNumber")
        @Description("Get yearly failed transaction status statistics by card number")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<TransactionDto.ApiResponseTransactionYearStatusFailed> getYearlyTransactionStatusFailedByCardNumber(
                        @Name("cardNumber") String cardNumber,
                        @Name("year") int year) {
                return transactionService.getYearlyTransactionStatusFailedByCardNumber(cardNumber, year);
        }
}
