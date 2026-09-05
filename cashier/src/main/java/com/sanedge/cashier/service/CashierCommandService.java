package com.sanedge.cashier.service;

import com.sanedge.cashier.domain.requests.CreateCashierRequest;
import com.sanedge.cashier.domain.requests.UpdateCashierRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.cashier.domain.response.CashierResponse;
import com.sanedge.cashier.domain.response.CashierResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface CashierCommandService {
    Uni<ApiResponse<CashierResponse>> createCashier(CreateCashierRequest req);
    Uni<ApiResponse<CashierResponse>> updateCashier(UpdateCashierRequest req);
    Uni<ApiResponse<CashierResponseDeleteAt>> trashedCashier(Long cashierId);
    Uni<ApiResponse<CashierResponseDeleteAt>> restoreCashier(Long cashierId);
    Uni<ApiResponse<Boolean>> deleteCashierPermanent(Long cashierId);
    Uni<ApiResponse<Boolean>> restoreAllCashier();
    Uni<ApiResponse<Boolean>> deleteAllCashierPermanent();
}
