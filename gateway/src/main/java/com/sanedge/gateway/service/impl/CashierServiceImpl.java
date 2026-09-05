package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.CashierDto;
import com.sanedge.gateway.service.CashierService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CashierServiceImpl implements CashierService {

    private static final Logger LOG = Logger.getLogger(CashierServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("cashier")
    pb.cashier.MutinyCashierServiceGrpc.MutinyCashierServiceStub cashierQueryService;

    @GrpcClient("cashier")
    pb.cashier.MutinyCashierCommandServiceGrpc.MutinyCashierCommandServiceStub cashierCommandService;

    @GrpcClient("stats-reader")
    pb.cashier.stats.MutinyCashierTotalSalesServiceGrpc.MutinyCashierTotalSalesServiceStub cashierTotalSalesServiceStub;

    @GrpcClient("stats-reader")
    pb.cashier.stats.MutinyCashierSalesServiceGrpc.MutinyCashierSalesServiceStub cashierSalesServiceStub;

    @Override
    public Uni<CashierDto.ApiResponsePaginationCashier> listCashiers(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("cashier.listCashiers", () -> cashierQueryService.findAll(pb.cashier.Cashier.FindAllCashierRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(CashierDto.ApiResponsePaginationCashier::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list cashiers: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CashierDto.ApiResponsePaginationCashierDeleteAt> getActiveCashiers(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("cashier.getActiveCashiers", () -> cashierQueryService.findByActive(pb.cashier.Cashier.FindAllCashierRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(CashierDto.ApiResponsePaginationCashierDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list active cashiers: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CashierDto.ApiResponsePaginationCashierDeleteAt> getTrashedCashiers(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("cashier.getTrashedCashiers", () -> cashierQueryService.findByTrashed(pb.cashier.Cashier.FindAllCashierRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(CashierDto.ApiResponsePaginationCashierDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list trashed cashiers: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CashierDto.ApiResponseCashier> getCashier(int id) {
        return telemetryHelper.traceAndMetric("cashier.getCashier", () -> cashierQueryService.findById(pb.cashier.Cashier.FindByIdCashierRequest.newBuilder()
                .setId(id)
                .build())
                .map(CashierDto.ApiResponseCashier::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get cashier " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CashierDto.ApiResponsePaginationCashier> getCashiersByMerchant(int merchantId, int page, int size, String search) {
        return telemetryHelper.traceAndMetric("cashier.getCashiersByMerchant", () -> cashierQueryService.findByMerchant(pb.cashier.Cashier.FindByMerchantCashierRequest.newBuilder()
                .setMerchantId(merchantId)
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(CashierDto.ApiResponsePaginationCashier::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get cashiers by merchant " + merchantId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CashierDto.ApiResponseCashier> createCashier(CashierDto.CreateCashierRequest body) {
        return telemetryHelper.traceAndMetric("cashier.createCashier", () -> cashierCommandService.createCashier(pb.cashier.Cashier.CreateCashierRequest.newBuilder()
                .setMerchantId(body.merchantId())
                .setUserId(body.userId())
                .setName(body.name())
                .build())
                .map(CashierDto.ApiResponseCashier::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create cashier: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CashierDto.ApiResponseCashier> updateCashier(int id, CashierDto.UpdateCashierRequest body) {
        return telemetryHelper.traceAndMetric("cashier.updateCashier", () -> cashierCommandService.updateCashier(pb.cashier.Cashier.UpdateCashierRequest.newBuilder()
                .setCashierId(id)
                .setName(body.name())
                .build())
                .map(CashierDto.ApiResponseCashier::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update cashier " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CashierDto.ApiResponseCashierDeleteAt> deleteCashier(int id) {
        return telemetryHelper.traceAndMetric("cashier.deleteCashier", () -> cashierCommandService.trashedCashier(pb.cashier.Cashier.FindByIdCashierRequest.newBuilder()
                .setId(id)
                .build())
                .map(CashierDto.ApiResponseCashierDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete cashier " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CashierDto.ApiResponseCashierDeleteAt> restoreCashier(int id) {
        return telemetryHelper.traceAndMetric("cashier.restoreCashier", () -> cashierCommandService.restoreCashier(pb.cashier.Cashier.FindByIdCashierRequest.newBuilder()
                .setId(id)
                .build())
                .map(CashierDto.ApiResponseCashierDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore cashier " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CashierDto.ApiResponseCashierDelete> deleteCashierPermanent(int id) {
        return telemetryHelper.traceAndMetric("cashier.deleteCashierPermanent", () -> cashierCommandService.deleteCashierPermanent(pb.cashier.Cashier.FindByIdCashierRequest.newBuilder()
                .setId(id)
                .build())
                .map(CashierDto.ApiResponseCashierDelete::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete cashier " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CashierDto.ApiResponseCashierAll> restoreAllCashier() {
        return telemetryHelper.traceAndMetric("cashier.restoreAllCashier", () -> cashierCommandService.restoreAllCashier(com.google.protobuf.Empty.getDefaultInstance())
                .map(CashierDto.ApiResponseCashierAll::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all cashiers: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CashierDto.ApiResponseCashierAll> deleteAllCashierPermanent() {
        return telemetryHelper.traceAndMetric("cashier.deleteAllCashierPermanent", () -> cashierCommandService.deleteAllCashierPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .map(CashierDto.ApiResponseCashierAll::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all cashiers: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CashierDto.ApiResponseCashierMonthlyTotalSales> getMonthlyTotalSales(int year, int month) {
        return telemetryHelper.traceAndMetric("cashier.getMonthlyTotalSales", () -> cashierTotalSalesServiceStub.findMonthlyTotalSales(pb.cashier.Cashier.FindYearMonthTotalSales.newBuilder()
                .setYear(year)
                .setMonth(month)
                .build())
                .map(CashierDto.ApiResponseCashierMonthlyTotalSales::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly total sales: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CashierDto.ApiResponseCashierYearlyTotalSales> getYearlyTotalSales(int year) {
        return telemetryHelper.traceAndMetric("cashier.getYearlyTotalSales", () -> cashierTotalSalesServiceStub.findYearlyTotalSales(pb.cashier.Cashier.FindYearTotalSales.newBuilder()
                .setYear(year)
                .build())
                .map(CashierDto.ApiResponseCashierYearlyTotalSales::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly total sales: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CashierDto.ApiResponseCashierMonthlyTotalSales> getMonthlyTotalSalesById(int cashierId, int year, int month) {
        return telemetryHelper.traceAndMetric("cashier.getMonthlyTotalSalesById", () -> cashierTotalSalesServiceStub.findMonthlyTotalSalesById(pb.cashier.Cashier.FindYearMonthTotalSalesById.newBuilder()
                .setCashierId(cashierId)
                .setYear(year)
                .setMonth(month)
                .build())
                .map(CashierDto.ApiResponseCashierMonthlyTotalSales::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly total sales for cashier " + cashierId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CashierDto.ApiResponseCashierYearlyTotalSales> getYearlyTotalSalesById(int cashierId, int year) {
        return telemetryHelper.traceAndMetric("cashier.getYearlyTotalSalesById", () -> cashierTotalSalesServiceStub.findYearlyTotalSalesById(pb.cashier.Cashier.FindYearTotalSalesById.newBuilder()
                .setCashierId(cashierId)
                .setYear(year)
                .build())
                .map(CashierDto.ApiResponseCashierYearlyTotalSales::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly total sales for cashier " + cashierId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CashierDto.ApiResponseCashierMonthlyTotalSales> getMonthlyTotalSalesByMerchant(int merchantId, int year, int month) {
        return telemetryHelper.traceAndMetric("cashier.getMonthlyTotalSalesByMerchant", () -> cashierTotalSalesServiceStub.findMonthlyTotalSalesByMerchant(pb.cashier.Cashier.FindYearMonthTotalSalesByMerchant.newBuilder()
                .setMerchantId(merchantId)
                .setYear(year)
                .setMonth(month)
                .build())
                .map(CashierDto.ApiResponseCashierMonthlyTotalSales::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly total sales for merchant " + merchantId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CashierDto.ApiResponseCashierYearlyTotalSales> getYearlyTotalSalesByMerchant(int merchantId, int year) {
        return telemetryHelper.traceAndMetric("cashier.getYearlyTotalSalesByMerchant", () -> cashierTotalSalesServiceStub.findYearlyTotalSalesByMerchant(pb.cashier.Cashier.FindYearTotalSalesByMerchant.newBuilder()
                .setMerchantId(merchantId)
                .setYear(year)
                .build())
                .map(CashierDto.ApiResponseCashierYearlyTotalSales::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly total sales for merchant " + merchantId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CashierDto.ApiResponseCashierMonthSales> getMonthSales(int year) {
        return telemetryHelper.traceAndMetric("cashier.getMonthSales", () -> cashierSalesServiceStub.findMonthSales(pb.cashier.Cashier.FindYearCashier.newBuilder()
                .setYear(year)
                .build())
                .map(CashierDto.ApiResponseCashierMonthSales::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly sales: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CashierDto.ApiResponseCashierYearSales> getYearSales(int year) {
        return telemetryHelper.traceAndMetric("cashier.getYearSales", () -> cashierSalesServiceStub.findYearSales(pb.cashier.Cashier.FindYearCashier.newBuilder()
                .setYear(year)
                .build())
                .map(CashierDto.ApiResponseCashierYearSales::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly sales: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CashierDto.ApiResponseCashierMonthSales> getMonthSalesByMerchant(int merchantId, int year) {
        return telemetryHelper.traceAndMetric("cashier.getMonthSalesByMerchant", () -> cashierSalesServiceStub.findMonthSalesByMerchant(pb.cashier.Cashier.FindYearCashierByMerchant.newBuilder()
                .setMerchantId(merchantId)
                .setYear(year)
                .build())
                .map(CashierDto.ApiResponseCashierMonthSales::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly sales for merchant " + merchantId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CashierDto.ApiResponseCashierYearSales> getYearSalesByMerchant(int merchantId, int year) {
        return telemetryHelper.traceAndMetric("cashier.getYearSalesByMerchant", () -> cashierSalesServiceStub.findYearSalesByMerchant(pb.cashier.Cashier.FindYearCashierByMerchant.newBuilder()
                .setMerchantId(merchantId)
                .setYear(year)
                .build())
                .map(CashierDto.ApiResponseCashierYearSales::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly sales for merchant " + merchantId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CashierDto.ApiResponseCashierMonthSales> getMonthSalesById(int cashierId, int year) {
        return telemetryHelper.traceAndMetric("cashier.getMonthSalesById", () -> cashierSalesServiceStub.findMonthSalesById(pb.cashier.Cashier.FindYearCashierById.newBuilder()
                .setCashierId(cashierId)
                .setYear(year)
                .build())
                .map(CashierDto.ApiResponseCashierMonthSales::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly sales for cashier " + cashierId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CashierDto.ApiResponseCashierYearSales> getYearSalesById(int cashierId, int year) {
        return telemetryHelper.traceAndMetric("cashier.getYearSalesById", () -> cashierSalesServiceStub.findYearSalesById(pb.cashier.Cashier.FindYearCashierById.newBuilder()
                .setCashierId(cashierId)
                .setYear(year)
                .build())
                .map(CashierDto.ApiResponseCashierYearSales::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly sales for cashier " + cashierId + ": " + throwable.getMessage(), throwable)));
    }
}
