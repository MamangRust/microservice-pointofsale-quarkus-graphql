package com.sanedge.gateway.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.TransactionDto;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    TelemetryHelper telemetryHelper;

    @Mock
    pb.transaction.MutinyTransactionQueryServiceGrpc.MutinyTransactionQueryServiceStub transactionQueryService;

    @Mock
    pb.transaction.MutinyTransactionCommandServiceGrpc.MutinyTransactionCommandServiceStub transactionCommandService;

    @Mock
    pb.transaction.stats.MutinyTransactionStatsAmountServiceGrpc.MutinyTransactionStatsAmountServiceStub transactionStatsAmountService;

    @Mock
    pb.transaction.stats.MutinyTransactionStatsMethodServiceGrpc.MutinyTransactionStatsMethodServiceStub transactionStatsMethodService;

    @Mock
    pb.transaction.stats.MutinyTransactionStatsStatusServiceGrpc.MutinyTransactionStatsStatusServiceStub transactionStatsStatusService;

    TransactionServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        service = new TransactionServiceImpl();

        setField(service, "telemetryHelper", telemetryHelper);
        setField(service, "transactionQueryService", transactionQueryService);
        setField(service, "transactionCommandService", transactionCommandService);
        setField(service, "transactionStatsAmountService", transactionStatsAmountService);
        setField(service, "transactionStatsMethodService", transactionStatsMethodService);
        setField(service, "transactionStatsStatusService", transactionStatsStatusService);

        lenient().when(telemetryHelper.traceAndMetric(anyString(), any(java.util.function.Supplier.class)))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<Uni<?>> supplier = inv.getArgument(1);
                    return supplier.get();
                });
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // Basic CRUD

    @Test
    void listTransactions_returnsSuccess() {
        pb.transaction.Transaction.TransactionResponse txProto = pb.transaction.Transaction.TransactionResponse.newBuilder()
                .setId(1).setCardNumber("1234").setAmount(1000).build();

        pb.transaction.TransactionQuery.ApiResponsePaginationTransaction responseProto =
                pb.transaction.TransactionQuery.ApiResponsePaginationTransaction.newBuilder()
                        .addData(txProto).setStatus("success").setMessage("Listed").build();

        when(transactionQueryService.findAllTransaction(any(pb.transaction.TransactionQuery.FindAllTransactionRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponsePaginationTransaction result = service.listTransactions(1, 10, "").await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
    }

    @Test
    void listTransactionsByCardNumber_returnsSuccess() {
        pb.transaction.Transaction.TransactionResponse txProto = pb.transaction.Transaction.TransactionResponse.newBuilder()
                .setId(2).setCardNumber("5678").build();

        pb.transaction.TransactionQuery.ApiResponsePaginationTransaction responseProto =
                pb.transaction.TransactionQuery.ApiResponsePaginationTransaction.newBuilder()
                        .addData(txProto).setStatus("success").setMessage("Listed by card").build();

        when(transactionQueryService.findAllTransactionByCardNumber(any(pb.transaction.TransactionQuery.FindAllTransactionCardNumberRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponsePaginationTransaction result =
                service.listTransactionsByCardNumber("5678", 1, 10, "").await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getTransaction_returnsSuccess() {
        pb.transaction.Transaction.TransactionResponse txProto = pb.transaction.Transaction.TransactionResponse.newBuilder()
                .setId(1).setAmount(500).build();

        pb.transaction.Transaction.ApiResponseTransaction responseProto =
                pb.transaction.Transaction.ApiResponseTransaction.newBuilder()
                        .setData(txProto).setStatus("success").setMessage("Found").build();

        when(transactionQueryService.findByIdTransaction(any(pb.transaction.Transaction.FindByIdTransactionRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponseTransaction result = service.getTransaction(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().id()).isEqualTo(1);
    }

    @Test
    void getTransactionsByMerchant_returnsSuccess() {
        pb.transaction.Transaction.TransactionResponse txProto = pb.transaction.Transaction.TransactionResponse.newBuilder()
                .setId(1).setMerchantId(200).build();

        pb.transaction.Transaction.ApiResponseTransactions responseProto =
                pb.transaction.Transaction.ApiResponseTransactions.newBuilder()
                        .addData(txProto).setStatus("success").setMessage("Merchant txs").build();

        when(transactionQueryService.findTransactionByMerchantId(any(pb.transaction.TransactionQuery.FindTransactionByMerchantIdRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponseTransactions result = service.getTransactionsByMerchant(200).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getActiveTransactions_returnsSuccess() {
        pb.transaction.Transaction.TransactionResponseDeleteAt txProto = pb.transaction.Transaction.TransactionResponseDeleteAt.newBuilder()
                .setId(1).build();

        pb.transaction.TransactionQuery.ApiResponsePaginationTransactionDeleteAt responseProto =
                pb.transaction.TransactionQuery.ApiResponsePaginationTransactionDeleteAt.newBuilder()
                        .addData(txProto).setStatus("success").setMessage("Active").build();

        when(transactionQueryService.findByActiveTransaction(any(pb.transaction.TransactionQuery.FindAllTransactionRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponsePaginationTransactionDeleteAt result = service.getActiveTransactions(1, 10, "").await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getTrashedTransactions_returnsSuccess() {
        pb.transaction.Transaction.TransactionResponseDeleteAt txProto = pb.transaction.Transaction.TransactionResponseDeleteAt.newBuilder()
                .setId(2).build();

        pb.transaction.TransactionQuery.ApiResponsePaginationTransactionDeleteAt responseProto =
                pb.transaction.TransactionQuery.ApiResponsePaginationTransactionDeleteAt.newBuilder()
                        .addData(txProto).setStatus("success").setMessage("Trashed").build();

        when(transactionQueryService.findByTrashedTransaction(any(pb.transaction.TransactionQuery.FindAllTransactionRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponsePaginationTransactionDeleteAt result = service.getTrashedTransactions(1, 10, "").await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void createTransaction_returnsSuccess() {
        pb.transaction.Transaction.TransactionResponse txProto = pb.transaction.Transaction.TransactionResponse.newBuilder()
                .setId(5).setAmount(100).build();

        pb.transaction.Transaction.ApiResponseTransaction responseProto =
                pb.transaction.Transaction.ApiResponseTransaction.newBuilder()
                        .setData(txProto).setStatus("success").setMessage("Created").build();

        when(transactionCommandService.createTransaction(any(pb.transaction.TransactionCommand.CreateTransactionRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.CreateTransactionRequest req = new TransactionDto.CreateTransactionRequest("1234", 100, "Credit", 1);
        TransactionDto.ApiResponseTransaction result = service.createTransaction(req).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void updateTransaction_returnsSuccess() {
        pb.transaction.Transaction.TransactionResponse txProto = pb.transaction.Transaction.TransactionResponse.newBuilder()
                .setId(5).setAmount(200).build();

        pb.transaction.Transaction.ApiResponseTransaction responseProto =
                pb.transaction.Transaction.ApiResponseTransaction.newBuilder()
                        .setData(txProto).setStatus("success").setMessage("Updated").build();

        when(transactionCommandService.updateTransaction(any(pb.transaction.TransactionCommand.UpdateTransactionRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.UpdateTransactionRequest req = new TransactionDto.UpdateTransactionRequest("1234", 200, "Debit", 1);
        TransactionDto.ApiResponseTransaction result = service.updateTransaction(5, req).await().indefinitely();
        assertThat(result.message()).isEqualTo("Updated");
    }

    @Test
    void deleteTransaction_returnsSuccess() {
        pb.transaction.Transaction.TransactionResponseDeleteAt proto = pb.transaction.Transaction.TransactionResponseDeleteAt.newBuilder()
                .setId(5).setDeletedAt(com.google.protobuf.StringValue.of("2024-07-01T00:00:00Z")).build();

        pb.transaction.Transaction.ApiResponseTransactionDeleteAt response =
                pb.transaction.Transaction.ApiResponseTransactionDeleteAt.newBuilder()
                        .setData(proto).setStatus("success").setMessage("Trashed").build();

        when(transactionCommandService.trashedTransaction(any(pb.transaction.Transaction.FindByIdTransactionRequest.class)))
                .thenReturn(Uni.createFrom().item(response));

        TransactionDto.ApiResponseTransactionDeleteAt result = service.deleteTransaction(5).await().indefinitely();
        assertThat(result.message()).isEqualTo("Trashed");
    }

    @Test
    void restoreTransaction_returnsSuccess() {
        pb.transaction.Transaction.TransactionResponseDeleteAt proto = pb.transaction.Transaction.TransactionResponseDeleteAt.newBuilder()
                .setId(5).build();

        pb.transaction.Transaction.ApiResponseTransactionDeleteAt response =
                pb.transaction.Transaction.ApiResponseTransactionDeleteAt.newBuilder()
                        .setData(proto).setStatus("success").setMessage("Restored").build();

        when(transactionCommandService.restoreTransaction(any(pb.transaction.Transaction.FindByIdTransactionRequest.class)))
                .thenReturn(Uni.createFrom().item(response));

        TransactionDto.ApiResponseTransactionDeleteAt result = service.restoreTransaction(5).await().indefinitely();
        assertThat(result.message()).isEqualTo("Restored");
    }

    @Test
    void deleteTransactionPermanent_returnsSuccess() {
        pb.transaction.TransactionCommand.ApiResponseTransactionDelete responseProto =
                pb.transaction.TransactionCommand.ApiResponseTransactionDelete.newBuilder()
                        .setStatus("success").setMessage("Permanent delete").build();

        when(transactionCommandService.deleteTransactionPermanent(any(pb.transaction.Transaction.FindByIdTransactionRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponseTransactionDelete result = service.deleteTransactionPermanent(5).await().indefinitely();
        assertThat(result.message()).isEqualTo("Permanent delete");
    }

    @Test
    void restoreAllTransaction_returnsSuccess() {
        pb.transaction.TransactionCommand.ApiResponseTransactionAll responseProto =
                pb.transaction.TransactionCommand.ApiResponseTransactionAll.newBuilder()
                        .setStatus("success").setMessage("All restored").build();

        when(transactionCommandService.restoreAllTransaction(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponseTransactionAll result = service.restoreAllTransaction().await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void deleteAllTransactionPermanent_returnsSuccess() {
        pb.transaction.TransactionCommand.ApiResponseTransactionAll responseProto =
                pb.transaction.TransactionCommand.ApiResponseTransactionAll.newBuilder()
                        .setStatus("success").setMessage("All deleted").build();

        when(transactionCommandService.deleteAllTransactionPermanent(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponseTransactionAll result = service.deleteAllTransactionPermanent().await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    // Stats: Amounts

    @Test
    void getMonthlyAmounts_returnsSuccess() {
        pb.transaction.stats.TransactionStatsAmount.TransactionMonthAmountResponse dataProto =
                pb.transaction.stats.TransactionStatsAmount.TransactionMonthAmountResponse.newBuilder()
                        .setMonth("6").setTotalAmount(5000).build();

        pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionMonthAmount responseProto =
                pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionMonthAmount.newBuilder()
                        .addData(dataProto).setStatus("success").setMessage("Monthly amounts").build();

        when(transactionStatsAmountService.findMonthlyAmounts(any(pb.transaction.Transaction.FindYearTransactionStatus.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponseTransactionMonthAmount result = service.getMonthlyAmounts(2024).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).totalAmount()).isEqualTo(5000);
    }

    @Test
    void getYearlyAmounts_returnsSuccess() {
        pb.transaction.stats.TransactionStatsAmount.TransactionYearlyAmountResponse dataProto =
                pb.transaction.stats.TransactionStatsAmount.TransactionYearlyAmountResponse.newBuilder()
                        .setYear("2024").setTotalAmount(60000).build();

        pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionYearAmount responseProto =
                pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionYearAmount.newBuilder()
                        .addData(dataProto).setStatus("success").setMessage("Yearly amounts").build();

        when(transactionStatsAmountService.findYearlyAmounts(any(pb.transaction.Transaction.FindYearTransactionStatus.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponseTransactionYearAmount result = service.getYearlyAmounts(2024).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).totalAmount()).isEqualTo(60000);
    }

    @Test
    void getMonthlyAmountsByCardNumber_returnsSuccess() {
        pb.transaction.stats.TransactionStatsAmount.TransactionMonthAmountResponse dataProto =
                pb.transaction.stats.TransactionStatsAmount.TransactionMonthAmountResponse.newBuilder()
                        .setMonth("6").setTotalAmount(3000).build();

        pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionMonthAmount responseProto =
                pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionMonthAmount.newBuilder()
                        .addData(dataProto).setStatus("success").setMessage("Monthly by card").build();

        when(transactionStatsAmountService.findMonthlyAmountsByCardNumber(any(pb.transaction.Transaction.FindByYearCardNumberTransactionRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponseTransactionMonthAmount result = service.getMonthlyAmountsByCardNumber("1234", 2024).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getYearlyAmountsByCardNumber_returnsSuccess() {
        pb.transaction.stats.TransactionStatsAmount.TransactionYearlyAmountResponse dataProto =
                pb.transaction.stats.TransactionStatsAmount.TransactionYearlyAmountResponse.newBuilder()
                        .setYear("2024").setTotalAmount(40000).build();

        pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionYearAmount responseProto =
                pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionYearAmount.newBuilder()
                        .addData(dataProto).setStatus("success").setMessage("Yearly by card").build();

        when(transactionStatsAmountService.findYearlyAmountsByCardNumber(any(pb.transaction.Transaction.FindByYearCardNumberTransactionRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponseTransactionYearAmount result = service.getYearlyAmountsByCardNumber("1234", 2024).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    // Stats: Methods

    @Test
    void getMonthlyPaymentMethods_returnsSuccess() {
        pb.transaction.stats.TransactionStatsMethod.TransactionMonthMethodResponse dataProto =
                pb.transaction.stats.TransactionStatsMethod.TransactionMonthMethodResponse.newBuilder()
                        .setMonth("6").setPaymentMethod("Credit").setTotalTransactions(10).setTotalAmount(0).build();

        pb.transaction.stats.TransactionStatsMethod.ApiResponseTransactionMonthMethod responseProto =
                pb.transaction.stats.TransactionStatsMethod.ApiResponseTransactionMonthMethod.newBuilder()
                        .addData(dataProto).setStatus("success").setMessage("Monthly methods").build();

        when(transactionStatsMethodService.findMonthlyPaymentMethods(any(pb.transaction.Transaction.FindYearTransactionStatus.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponseTransactionMonthMethod result = service.getMonthlyPaymentMethods(2024).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getYearlyPaymentMethods_returnsSuccess() {
        pb.transaction.stats.TransactionStatsMethod.TransactionYearMethodResponse dataProto =
                pb.transaction.stats.TransactionStatsMethod.TransactionYearMethodResponse.newBuilder()
                        .setYear("2024").setPaymentMethod("Debit").setTotalTransactions(50).setTotalAmount(0).build();

        pb.transaction.stats.TransactionStatsMethod.ApiResponseTransactionYearMethod responseProto =
                pb.transaction.stats.TransactionStatsMethod.ApiResponseTransactionYearMethod.newBuilder()
                        .addData(dataProto).setStatus("success").setMessage("Yearly methods").build();

        when(transactionStatsMethodService.findYearlyPaymentMethods(any(pb.transaction.Transaction.FindYearTransactionStatus.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponseTransactionYearMethod result = service.getYearlyPaymentMethods(2024).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getMonthlyPaymentMethodsByCardNumber_returnsSuccess() {
        pb.transaction.stats.TransactionStatsMethod.TransactionMonthMethodResponse dataProto =
                pb.transaction.stats.TransactionStatsMethod.TransactionMonthMethodResponse.newBuilder()
                        .setMonth("6").setPaymentMethod("Credit").setTotalTransactions(5).setTotalAmount(0).build();

        pb.transaction.stats.TransactionStatsMethod.ApiResponseTransactionMonthMethod responseProto =
                pb.transaction.stats.TransactionStatsMethod.ApiResponseTransactionMonthMethod.newBuilder()
                        .addData(dataProto).setStatus("success").setMessage("Monthly methods by card").build();

        when(transactionStatsMethodService.findMonthlyPaymentMethodsByCardNumber(any(pb.transaction.Transaction.FindByYearCardNumberTransactionRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponseTransactionMonthMethod result = service.getMonthlyPaymentMethodsByCardNumber("1234", 2024).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getYearlyPaymentMethodsByCardNumber_returnsSuccess() {
        pb.transaction.stats.TransactionStatsMethod.TransactionYearMethodResponse dataProto =
                pb.transaction.stats.TransactionStatsMethod.TransactionYearMethodResponse.newBuilder()
                        .setYear("2024").setPaymentMethod("Debit").setTotalTransactions(30).setTotalAmount(0).build();

        pb.transaction.stats.TransactionStatsMethod.ApiResponseTransactionYearMethod responseProto =
                pb.transaction.stats.TransactionStatsMethod.ApiResponseTransactionYearMethod.newBuilder()
                        .addData(dataProto).setStatus("success").setMessage("Yearly methods by card").build();

        when(transactionStatsMethodService.findYearlyPaymentMethodsByCardNumber(any(pb.transaction.Transaction.FindByYearCardNumberTransactionRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponseTransactionYearMethod result = service.getYearlyPaymentMethodsByCardNumber("1234", 2024).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    // Stats: Status

    @Test
    void getMonthlyTransactionStatusSuccess_returnsSuccess() {
        pb.transaction.stats.TransactionStatsStatus.TransactionMonthStatusSuccessResponse dataProto =
                pb.transaction.stats.TransactionStatsStatus.TransactionMonthStatusSuccessResponse.newBuilder()
                        .setYear("2024").setMonth("6").setTotalSuccess(10).setTotalAmount(0).build();

        pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess responseProto =
                pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess.newBuilder()
                        .addData(dataProto).setStatus("success").setMessage("Monthly success").build();

        when(transactionStatsStatusService.findMonthlyTransactionStatusSuccess(any(pb.transaction.Transaction.FindMonthlyTransactionStatus.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponseTransactionMonthStatusSuccess result = service.getMonthlyTransactionStatusSuccess(2024, 6).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getYearlyTransactionStatusSuccess_returnsSuccess() {
        pb.transaction.stats.TransactionStatsStatus.TransactionYearStatusSuccessResponse dataProto =
                pb.transaction.stats.TransactionStatsStatus.TransactionYearStatusSuccessResponse.newBuilder()
                        .setYear("2024").setTotalSuccess(120).setTotalAmount(0).build();

        pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionYearStatusSuccess responseProto =
                pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionYearStatusSuccess.newBuilder()
                        .addData(dataProto).setStatus("success").setMessage("Yearly success").build();

        when(transactionStatsStatusService.findYearlyTransactionStatusSuccess(any(pb.transaction.Transaction.FindYearTransactionStatus.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponseTransactionYearStatusSuccess result = service.getYearlyTransactionStatusSuccess(2024).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getMonthlyTransactionStatusFailed_returnsSuccess() {
        pb.transaction.stats.TransactionStatsStatus.TransactionMonthStatusFailedResponse dataProto =
                pb.transaction.stats.TransactionStatsStatus.TransactionMonthStatusFailedResponse.newBuilder()
                        .setYear("2024").setMonth("6").setTotalFailed(2).setTotalAmount(0).build();

        pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionMonthStatusFailed responseProto =
                pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionMonthStatusFailed.newBuilder()
                        .addData(dataProto).setStatus("success").setMessage("Monthly failed").build();

        when(transactionStatsStatusService.findMonthlyTransactionStatusFailed(any(pb.transaction.Transaction.FindMonthlyTransactionStatus.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponseTransactionMonthStatusFailed result = service.getMonthlyTransactionStatusFailed(2024, 6).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getYearlyTransactionStatusFailed_returnsSuccess() {
        pb.transaction.stats.TransactionStatsStatus.TransactionYearStatusFailedResponse dataProto =
                pb.transaction.stats.TransactionStatsStatus.TransactionYearStatusFailedResponse.newBuilder()
                        .setYear("2024").setTotalFailed(15).setTotalAmount(0).build();

        pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionYearStatusFailed responseProto =
                pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionYearStatusFailed.newBuilder()
                        .addData(dataProto).setStatus("success").setMessage("Yearly failed").build();

        when(transactionStatsStatusService.findYearlyTransactionStatusFailed(any(pb.transaction.Transaction.FindYearTransactionStatus.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponseTransactionYearStatusFailed result = service.getYearlyTransactionStatusFailed(2024).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getMonthlyTransactionStatusSuccessByCardNumber_returnsSuccess() {
        pb.transaction.stats.TransactionStatsStatus.TransactionMonthStatusSuccessResponse dataProto =
                pb.transaction.stats.TransactionStatsStatus.TransactionMonthStatusSuccessResponse.newBuilder()
                        .setYear("2024").setMonth("6").setTotalSuccess(5).setTotalAmount(0).build();

        pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess responseProto =
                pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess.newBuilder()
                        .addData(dataProto).setStatus("success").setMessage("Monthly success by card").build();

        when(transactionStatsStatusService.findMonthlyTransactionStatusSuccessByCardNumber(any(pb.transaction.Transaction.FindMonthlyTransactionStatusCardNumber.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponseTransactionMonthStatusSuccess result =
                service.getMonthlyTransactionStatusSuccessByCardNumber("1234", 2024, 6).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getYearlyTransactionStatusSuccessByCardNumber_returnsSuccess() {
        pb.transaction.stats.TransactionStatsStatus.TransactionYearStatusSuccessResponse dataProto =
                pb.transaction.stats.TransactionStatsStatus.TransactionYearStatusSuccessResponse.newBuilder()
                        .setYear("2024").setTotalSuccess(30).setTotalAmount(0).build();

        pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionYearStatusSuccess responseProto =
                pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionYearStatusSuccess.newBuilder()
                        .addData(dataProto).setStatus("success").setMessage("Yearly success by card").build();

        when(transactionStatsStatusService.findYearlyTransactionStatusSuccessByCardNumber(any(pb.transaction.Transaction.FindYearTransactionStatusCardNumber.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponseTransactionYearStatusSuccess result =
                service.getYearlyTransactionStatusSuccessByCardNumber("1234", 2024).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getMonthlyTransactionStatusFailedByCardNumber_returnsSuccess() {
        pb.transaction.stats.TransactionStatsStatus.TransactionMonthStatusFailedResponse dataProto =
                pb.transaction.stats.TransactionStatsStatus.TransactionMonthStatusFailedResponse.newBuilder()
                        .setYear("2024").setMonth("6").setTotalFailed(1).setTotalAmount(0).build();

        pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionMonthStatusFailed responseProto =
                pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionMonthStatusFailed.newBuilder()
                        .addData(dataProto).setStatus("success").setMessage("Monthly failed by card").build();

        when(transactionStatsStatusService.findMonthlyTransactionStatusFailedByCardNumber(any(pb.transaction.Transaction.FindMonthlyTransactionStatusCardNumber.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponseTransactionMonthStatusFailed result =
                service.getMonthlyTransactionStatusFailedByCardNumber("1234", 2024, 6).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getYearlyTransactionStatusFailedByCardNumber_returnsSuccess() {
        pb.transaction.stats.TransactionStatsStatus.TransactionYearStatusFailedResponse dataProto =
                pb.transaction.stats.TransactionStatsStatus.TransactionYearStatusFailedResponse.newBuilder()
                        .setYear("2024").setTotalFailed(5).setTotalAmount(0).build();

        pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionYearStatusFailed responseProto =
                pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionYearStatusFailed.newBuilder()
                        .addData(dataProto).setStatus("success").setMessage("Yearly failed by card").build();

        when(transactionStatsStatusService.findYearlyTransactionStatusFailedByCardNumber(any(pb.transaction.Transaction.FindYearTransactionStatusCardNumber.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TransactionDto.ApiResponseTransactionYearStatusFailed result =
                service.getYearlyTransactionStatusFailedByCardNumber("1234", 2024).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }
}
