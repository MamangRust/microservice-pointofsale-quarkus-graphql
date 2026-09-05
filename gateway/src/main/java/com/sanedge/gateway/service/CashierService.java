package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.CashierDto;
import io.smallrye.mutiny.Uni;

public interface CashierService {
    Uni<CashierDto.ApiResponsePaginationCashier> listCashiers(int page, int size, String search);
    Uni<CashierDto.ApiResponsePaginationCashierDeleteAt> getActiveCashiers(int page, int size, String search);
    Uni<CashierDto.ApiResponsePaginationCashierDeleteAt> getTrashedCashiers(int page, int size, String search);
    Uni<CashierDto.ApiResponseCashier> getCashier(int id);
    Uni<CashierDto.ApiResponsePaginationCashier> getCashiersByMerchant(int merchantId, int page, int size, String search);
    Uni<CashierDto.ApiResponseCashier> createCashier(CashierDto.CreateCashierRequest body);
    Uni<CashierDto.ApiResponseCashier> updateCashier(int id, CashierDto.UpdateCashierRequest body);
    Uni<CashierDto.ApiResponseCashierDeleteAt> deleteCashier(int id);
    Uni<CashierDto.ApiResponseCashierDeleteAt> restoreCashier(int id);
    Uni<CashierDto.ApiResponseCashierDelete> deleteCashierPermanent(int id);
    Uni<CashierDto.ApiResponseCashierAll> restoreAllCashier();
    Uni<CashierDto.ApiResponseCashierAll> deleteAllCashierPermanent();

    // Statistics
    Uni<CashierDto.ApiResponseCashierMonthlyTotalSales> getMonthlyTotalSales(int year, int month);
    Uni<CashierDto.ApiResponseCashierYearlyTotalSales> getYearlyTotalSales(int year);
    Uni<CashierDto.ApiResponseCashierMonthlyTotalSales> getMonthlyTotalSalesById(int cashierId, int year, int month);
    Uni<CashierDto.ApiResponseCashierYearlyTotalSales> getYearlyTotalSalesById(int cashierId, int year);
    Uni<CashierDto.ApiResponseCashierMonthlyTotalSales> getMonthlyTotalSalesByMerchant(int merchantId, int year, int month);
    Uni<CashierDto.ApiResponseCashierYearlyTotalSales> getYearlyTotalSalesByMerchant(int merchantId, int year);
    Uni<CashierDto.ApiResponseCashierMonthSales> getMonthSales(int year);
    Uni<CashierDto.ApiResponseCashierYearSales> getYearSales(int year);
    Uni<CashierDto.ApiResponseCashierMonthSales> getMonthSalesByMerchant(int merchantId, int year);
    Uni<CashierDto.ApiResponseCashierYearSales> getYearSalesByMerchant(int merchantId, int year);
    Uni<CashierDto.ApiResponseCashierMonthSales> getMonthSalesById(int cashierId, int year);
    Uni<CashierDto.ApiResponseCashierYearSales> getYearSalesById(int cashierId, int year);
}
