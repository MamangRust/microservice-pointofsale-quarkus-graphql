package com.sanedge.transaction.service;

import java.util.List;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.transaction.domain.requests.FindAllTransactionByMerchantRequest;
import com.sanedge.transaction.domain.requests.FindAllTransactionRequest;
import com.sanedge.transaction.domain.response.TransactionResponse;
import com.sanedge.transaction.domain.response.TransactionResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface TransactionQueryService {
    Uni<ApiResponsePagination<List<TransactionResponse>>> findAllTransactions(FindAllTransactionRequest req);
    Uni<ApiResponsePagination<List<TransactionResponseDeleteAt>>> findByActive(FindAllTransactionRequest req);
    Uni<ApiResponsePagination<List<TransactionResponseDeleteAt>>> findByTrashed(FindAllTransactionRequest req);
    Uni<ApiResponsePagination<List<TransactionResponse>>> findByMerchant(FindAllTransactionByMerchantRequest req);
    Uni<ApiResponse<TransactionResponse>> findById(Integer id);
    Uni<ApiResponse<TransactionResponse>> findByOrderId(Integer id);
}
