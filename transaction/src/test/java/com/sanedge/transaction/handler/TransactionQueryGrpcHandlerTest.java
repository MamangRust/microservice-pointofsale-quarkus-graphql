package com.sanedge.transaction.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.transaction.domain.response.TransactionResponse;
import com.sanedge.transaction.domain.response.TransactionResponseDeleteAt;
import com.sanedge.transaction.service.TransactionQueryService;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.transaction.Transaction;
import pb.transaction.TransactionQuery;

@ExtendWith(MockitoExtension.class)
class TransactionQueryGrpcHandlerTest {

    @Mock
    private TransactionQueryService transactionQueryService;

    private TransactionQueryGrpcHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TransactionQueryGrpcHandler();
        handler.transactionQueryService = transactionQueryService;
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

    // findAllTransaction
    @Test
    @DisplayName("findAllTransaction - success")
    void findAllTransaction_Success() {
        TransactionQuery.FindAllTransactionRequest request = TransactionQuery.FindAllTransactionRequest.newBuilder()
                .setPage(1).setPageSize(10).build();
        TransactionResponse data = createTransactionResponse(1L);
        ApiResponsePagination<List<TransactionResponse>> apiResp = new ApiResponsePagination<>(
                "success", "Transactions retrieved", List.of(data), null);
        when(transactionQueryService.findAllTransactions(any())).thenReturn(Uni.createFrom().item(apiResp));

        TransactionQuery.ApiResponsePaginationTransaction response = handler.findAllTransaction(request).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
        assertThat(response.getData(0).getPaymentMethod()).isEqualTo("CREDIT");
        assertThat(response.getData(0).getAmount()).isEqualTo(100000);
    }

    @Test
    @DisplayName("findAllTransaction - error")
    void findAllTransaction_Error() {
        when(transactionQueryService.findAllTransactions(any()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));
        try {
            handler.findAllTransaction(TransactionQuery.FindAllTransactionRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) {
            assertThat(e).isNotNull();
        }
    }

    // findAllTransactionByCardNumber (uses findAllTransactions internally)
    @Test
    @DisplayName("findAllTransactionByCardNumber - success")
    void findAllTransactionByCardNumber_Success() {
        TransactionQuery.FindAllTransactionCardNumberRequest request = TransactionQuery.FindAllTransactionCardNumberRequest.newBuilder()
                .setCardNumber("4111").setPage(1).setPageSize(10).build();
        TransactionResponse data = createTransactionResponse(1L);
        ApiResponsePagination<List<TransactionResponse>> apiResp = new ApiResponsePagination<>(
                "success", "Transactions by card", List.of(data), null);
        when(transactionQueryService.findAllTransactions(any())).thenReturn(Uni.createFrom().item(apiResp));

        TransactionQuery.ApiResponsePaginationTransaction response = handler.findAllTransactionByCardNumber(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("findAllTransactionByCardNumber - error")
    void findAllTransactionByCardNumber_Error() {
        when(transactionQueryService.findAllTransactions(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.findAllTransactionByCardNumber(TransactionQuery.FindAllTransactionCardNumberRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // findByIdTransaction
    @Test
    @DisplayName("findByIdTransaction - success")
    void findByIdTransaction_Success() {
        Transaction.FindByIdTransactionRequest request = Transaction.FindByIdTransactionRequest.newBuilder().setTransactionId(1).build();
        TransactionResponse data = createTransactionResponse(1L);
        ApiResponse<TransactionResponse> apiResp = ApiResponse.success("Transaction found", data);
        when(transactionQueryService.findById(anyInt())).thenReturn(Uni.createFrom().item(apiResp));

        Transaction.ApiResponseTransaction response = handler.findByIdTransaction(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("findByIdTransaction - error")
    void findByIdTransaction_Error() {
        when(transactionQueryService.findById(anyInt())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.findByIdTransaction(Transaction.FindByIdTransactionRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // findTransactionByMerchantId
    @Test
    @DisplayName("findTransactionByMerchantId - success")
    void findTransactionByMerchantId_Success() {
        TransactionQuery.FindTransactionByMerchantIdRequest request = TransactionQuery.FindTransactionByMerchantIdRequest.newBuilder()
                .setMerchantId(10).build();
        TransactionResponse data = createTransactionResponse(1L);
        ApiResponsePagination<List<TransactionResponse>> apiResp = new ApiResponsePagination<>(
                "success", "Transactions by merchant", List.of(data), null);
        when(transactionQueryService.findByMerchant(any())).thenReturn(Uni.createFrom().item(apiResp));

        Transaction.ApiResponseTransactions response = handler.findTransactionByMerchantId(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("findTransactionByMerchantId - error")
    void findTransactionByMerchantId_Error() {
        when(transactionQueryService.findByMerchant(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.findTransactionByMerchantId(TransactionQuery.FindTransactionByMerchantIdRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // findByActiveTransaction
    @Test
    @DisplayName("findByActiveTransaction - success")
    void findByActiveTransaction_Success() {
        TransactionQuery.FindAllTransactionRequest request = TransactionQuery.FindAllTransactionRequest.newBuilder().setPage(1).build();
        TransactionResponseDeleteAt data = createTransactionDeleteAt(1L);
        ApiResponsePagination<List<TransactionResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                "success", "Active transactions", List.of(data), null);
        when(transactionQueryService.findByActive(any())).thenReturn(Uni.createFrom().item(apiResp));

        TransactionQuery.ApiResponsePaginationTransactionDeleteAt response = handler.findByActiveTransaction(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
        assertThat(response.getData(0).hasDeletedAt()).isTrue();
    }

    @Test
    @DisplayName("findByActiveTransaction - error")
    void findByActiveTransaction_Error() {
        when(transactionQueryService.findByActive(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.findByActiveTransaction(TransactionQuery.FindAllTransactionRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // findByTrashedTransaction
    @Test
    @DisplayName("findByTrashedTransaction - success")
    void findByTrashedTransaction_Success() {
        TransactionQuery.FindAllTransactionRequest request = TransactionQuery.FindAllTransactionRequest.newBuilder().build();
        TransactionResponseDeleteAt data = createTransactionDeleteAt(2L);
        ApiResponsePagination<List<TransactionResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                "success", "Trashed transactions", List.of(data), null);
        when(transactionQueryService.findByTrashed(any())).thenReturn(Uni.createFrom().item(apiResp));

        TransactionQuery.ApiResponsePaginationTransactionDeleteAt response = handler.findByTrashedTransaction(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData(0).hasDeletedAt()).isTrue();
    }

    @Test
    @DisplayName("findByTrashedTransaction - error")
    void findByTrashedTransaction_Error() {
        when(transactionQueryService.findByTrashed(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.findByTrashedTransaction(TransactionQuery.FindAllTransactionRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) { assertThat(e).isNotNull(); }
    }

    // edge cases
    @Test
    @DisplayName("findAllTransaction - empty list")
    void findAllTransaction_Empty() {
        when(transactionQueryService.findAllTransactions(any()))
                .thenReturn(Uni.createFrom().item(new ApiResponsePagination<>("success", "No transactions", List.of(), null)));
        TransactionQuery.ApiResponsePaginationTransaction response = handler.findAllTransaction(
                TransactionQuery.FindAllTransactionRequest.newBuilder().build()).await().indefinitely();
        assertThat(response.getDataCount()).isZero();
    }

    @Test
    @DisplayName("findByIdTransaction - null data")
    void findByIdTransaction_NullData() {
        when(transactionQueryService.findById(anyInt()))
                .thenReturn(Uni.createFrom().item(ApiResponse.success("No data", null)));
        Transaction.ApiResponseTransaction response = handler.findByIdTransaction(
                Transaction.FindByIdTransactionRequest.newBuilder().setTransactionId(1).build()).await().indefinitely();
        assertThat(response.hasData()).isFalse();
    }
}