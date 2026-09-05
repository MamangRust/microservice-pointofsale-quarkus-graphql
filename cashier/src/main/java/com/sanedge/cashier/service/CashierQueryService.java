package com.sanedge.cashier.service;

import java.util.List;

import com.sanedge.cashier.domain.requests.FindAllCashierMerchant;
import com.sanedge.cashier.domain.requests.FindAllCashiers;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.cashier.domain.response.CashierResponse;
import com.sanedge.cashier.domain.response.CashierResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface CashierQueryService {
    Uni<ApiResponsePagination<List<CashierResponse>>> findAll(FindAllCashiers req);
    Uni<ApiResponse<CashierResponse>> findById(Long cashierId);
    Uni<ApiResponsePagination<List<CashierResponseDeleteAt>>> findByActive(FindAllCashiers req);
    Uni<ApiResponsePagination<List<CashierResponseDeleteAt>>> findByTrashed(FindAllCashiers req);
    Uni<ApiResponsePagination<List<CashierResponse>>> findByMerchant(FindAllCashierMerchant req);
}
