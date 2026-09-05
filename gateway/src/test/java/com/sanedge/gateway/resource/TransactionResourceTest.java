package com.sanedge.gateway.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.TransactionDto;
import com.sanedge.gateway.service.TransactionService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class TransactionResourceTest {

    @Mock TransactionService transactionService;
    TransactionResource resource;

    @BeforeEach
    void setUp() throws Exception {
        resource = new TransactionResource();
        Field f = TransactionResource.class.getDeclaredField("transactionService");
        f.setAccessible(true);
        f.set(resource, transactionService);
    }

    @Test void listTransactions_ok() {
        when(transactionService.listTransactions(anyInt(), anyInt(), anyString()))
            .thenReturn(Uni.createFrom().item(new TransactionDto.ApiResponsePaginationTransaction("success", "ok", List.of(), null)));
        assertThat(resource.listTransactions(1, 10, "").await().indefinitely().status()).isEqualTo("success");
    }

    @Test void getTransaction_ok() {
        when(transactionService.getTransaction(anyInt()))
            .thenReturn(Uni.createFrom().item(new TransactionDto.ApiResponseTransaction("success", "ok", null)));
        assertThat(resource.getTransaction(1).await().indefinitely().status()).isEqualTo("success");
    }

    @Test void createTransaction_ok() {
        when(transactionService.createTransaction(any()))
            .thenReturn(Uni.createFrom().item(new TransactionDto.ApiResponseTransaction("success", "created", null)));
        assertThat(resource.createTransaction(new TransactionDto.CreateTransactionRequest("123", 100, "cash", 1)).await().indefinitely().message()).isEqualTo("created");
    }

    @Test void deleteTransaction_ok() {
        when(transactionService.deleteTransaction(anyInt()))
            .thenReturn(Uni.createFrom().item(new TransactionDto.ApiResponseTransactionDeleteAt("success", "trashed", null)));
        assertThat(resource.deleteTransaction(1).await().indefinitely().message()).isEqualTo("trashed");
    }

    @Test void restoreTransaction_ok() {
        when(transactionService.restoreTransaction(anyInt()))
            .thenReturn(Uni.createFrom().item(new TransactionDto.ApiResponseTransactionDeleteAt("success", "restored", null)));
        assertThat(resource.restoreTransaction(1).await().indefinitely().message()).isEqualTo("restored");
    }
}
