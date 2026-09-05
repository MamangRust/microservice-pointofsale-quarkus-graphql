package com.sanedge.transaction.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.protobuf.Empty;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.transaction.domain.response.TransactionResponse;
import com.sanedge.transaction.domain.response.TransactionResponseDeleteAt;
import com.sanedge.transaction.service.TransactionCommandService;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.transaction.Transaction;
import pb.transaction.TransactionCommand;

@ExtendWith(MockitoExtension.class)
class TransactionCommandGrpcHandlerTest {

    @Mock
    private TransactionCommandService transactionCommandService;

    private TransactionCommandGrpcHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TransactionCommandGrpcHandler();
        handler.transactionCommandService = transactionCommandService;
    }

    // helpers
    private TransactionResponse createTransactionResponse(Long id) {
        TransactionResponse r = new TransactionResponse();
        r.setId(id);
        r.setOrderId(500);
        r.setMerchantId(10);
        r.setPaymentMethod("CREDIT");
        r.setAmount(100000);
        r.setCreatedAt(LocalDateTime.now().toString());
        r.setUpdatedAt(LocalDateTime.now().toString());
        return r;
    }

    private TransactionResponseDeleteAt createTransactionDeleteAt(Long id) {
        TransactionResponseDeleteAt r = new TransactionResponseDeleteAt();
        r.setId(id);
        r.setOrderId(500);
        r.setMerchantId(10);
        r.setPaymentMethod("CREDIT");
        r.setAmount(100000);
        r.setCreatedAt(LocalDateTime.now().toString());
        r.setUpdatedAt(LocalDateTime.now().toString());
        r.setDeletedAt(LocalDateTime.now().toString());
        return r;
    }

    // createTransaction
    @Test
    @DisplayName("createTransaction - success")
    void create_Success() {
        TransactionCommand.CreateTransactionRequest request = TransactionCommand.CreateTransactionRequest.newBuilder()
                .setCardNumber("1")          // used as order id
                .setMerchantId(10)
                .setPaymentMethod("CREDIT")
                .setAmount(22000)
                .build();

        TransactionResponse data = createTransactionResponse(1L);
        data.setAmount(22000);
        ApiResponse<TransactionResponse> apiResp = ApiResponse.success("Transaction created", data);
        when(transactionCommandService.create(any())).thenReturn(Uni.createFrom().item(apiResp));

        Transaction.ApiResponseTransaction response = handler.createTransaction(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getId()).isEqualTo(1);
        assertThat(response.getData().getAmount()).isEqualTo(22000);
    }

    @Test
    @DisplayName("createTransaction - error")
    void create_Error() {
        when(transactionCommandService.create(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.createTransaction(TransactionCommand.CreateTransactionRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // updateTransaction
    @Test
    @DisplayName("updateTransaction - success")
    void update_Success() {
        TransactionCommand.UpdateTransactionRequest request = TransactionCommand.UpdateTransactionRequest.newBuilder()
                .setTransactionId(1)
                .setCardNumber("1")
                .setMerchantId(10)
                .setPaymentMethod("DEBIT")
                .setAmount(33000)
                .build();

        TransactionResponse data = createTransactionResponse(1L);
        data.setAmount(33000);
        data.setPaymentMethod("DEBIT");
        ApiResponse<TransactionResponse> apiResp = ApiResponse.success("Transaction updated", data);
        when(transactionCommandService.update(any())).thenReturn(Uni.createFrom().item(apiResp));

        Transaction.ApiResponseTransaction response = handler.updateTransaction(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getAmount()).isEqualTo(33000);
        assertThat(response.getData().getPaymentMethod()).isEqualTo("DEBIT");
    }

    @Test
    @DisplayName("updateTransaction - error")
    void update_Error() {
        when(transactionCommandService.update(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.updateTransaction(TransactionCommand.UpdateTransactionRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // trashedTransaction
    @Test
    @DisplayName("trashedTransaction - success")
    void trashed_Success() {
        Transaction.FindByIdTransactionRequest request = Transaction.FindByIdTransactionRequest.newBuilder().setTransactionId(1).build();
        TransactionResponseDeleteAt data = createTransactionDeleteAt(1L);
        ApiResponse<TransactionResponseDeleteAt> apiResp = ApiResponse.success("Trashed", data);
        when(transactionCommandService.trash(anyInt())).thenReturn(Uni.createFrom().item(apiResp));

        Transaction.ApiResponseTransactionDeleteAt response = handler.trashedTransaction(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().hasDeletedAt()).isTrue();
    }

    @Test
    @DisplayName("trashedTransaction - error")
    void trashed_Error() {
        when(transactionCommandService.trash(anyInt())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.trashedTransaction(Transaction.FindByIdTransactionRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // restoreTransaction
    @Test
    @DisplayName("restoreTransaction - success")
    void restore_Success() {
        Transaction.FindByIdTransactionRequest request = Transaction.FindByIdTransactionRequest.newBuilder().setTransactionId(1).build();
        TransactionResponseDeleteAt data = createTransactionDeleteAt(1L);
        data.setDeletedAt(null);
        ApiResponse<TransactionResponseDeleteAt> apiResp = ApiResponse.success("Restored", data);
        when(transactionCommandService.restore(anyInt())).thenReturn(Uni.createFrom().item(apiResp));

        Transaction.ApiResponseTransactionDeleteAt response = handler.restoreTransaction(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().hasDeletedAt()).isFalse();
    }

    @Test
    @DisplayName("restoreTransaction - error")
    void restore_Error() {
        when(transactionCommandService.restore(anyInt())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.restoreTransaction(Transaction.FindByIdTransactionRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // deleteTransactionPermanent
    @Test
    @DisplayName("deleteTransactionPermanent - success")
    void deletePermanent_Success() {
        Transaction.FindByIdTransactionRequest request = Transaction.FindByIdTransactionRequest.newBuilder().setTransactionId(1).build();
        ApiResponse<Boolean> apiResp = ApiResponse.success("Permanently deleted", true);
        when(transactionCommandService.delete(anyInt())).thenReturn(Uni.createFrom().item(apiResp));

        TransactionCommand.ApiResponseTransactionDelete response = handler.deleteTransactionPermanent(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Permanently deleted");
    }

    @Test
    @DisplayName("deleteTransactionPermanent - error")
    void deletePermanent_Error() {
        when(transactionCommandService.delete(anyInt())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.deleteTransactionPermanent(Transaction.FindByIdTransactionRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // restoreAllTransaction
    @Test
    @DisplayName("restoreAllTransaction - success")
    void restoreAll_Success() {
        ApiResponse<Boolean> apiResp = ApiResponse.success("All restored", true);
        when(transactionCommandService.restoreAll()).thenReturn(Uni.createFrom().item(apiResp));

        TransactionCommand.ApiResponseTransactionAll response = handler.restoreAllTransaction(Empty.getDefaultInstance()).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("All restored");
    }

    @Test
    @DisplayName("restoreAllTransaction - error")
    void restoreAll_Error() {
        when(transactionCommandService.restoreAll()).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.restoreAllTransaction(Empty.getDefaultInstance()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // deleteAllTransactionPermanent
    @Test
    @DisplayName("deleteAllTransactionPermanent - success")
    void deleteAll_Success() {
        ApiResponse<Boolean> apiResp = ApiResponse.success("All permanently deleted", true);
        when(transactionCommandService.deleteAll()).thenReturn(Uni.createFrom().item(apiResp));

        TransactionCommand.ApiResponseTransactionAll response = handler.deleteAllTransactionPermanent(Empty.getDefaultInstance()).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("All permanently deleted");
    }

    @Test
    @DisplayName("deleteAllTransactionPermanent - error")
    void deleteAll_Error() {
        when(transactionCommandService.deleteAll()).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.deleteAllTransactionPermanent(Empty.getDefaultInstance()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // edge cases
    @Test
    @DisplayName("createTransaction - null data")
    void create_NullData() {
        when(transactionCommandService.create(any())).thenReturn(Uni.createFrom().item(ApiResponse.success("Created", null)));
        Transaction.ApiResponseTransaction response = handler.createTransaction(
                TransactionCommand.CreateTransactionRequest.newBuilder().build()).await().indefinitely();
        assertThat(response.hasData()).isFalse();
    }

    @Test
    @DisplayName("updateTransaction - null data")
    void update_NullData() {
        when(transactionCommandService.update(any())).thenReturn(Uni.createFrom().item(ApiResponse.success("Updated", null)));
        Transaction.ApiResponseTransaction response = handler.updateTransaction(
                TransactionCommand.UpdateTransactionRequest.newBuilder().build()).await().indefinitely();
        assertThat(response.hasData()).isFalse();
    }
}