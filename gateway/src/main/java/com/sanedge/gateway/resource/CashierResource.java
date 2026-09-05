package com.sanedge.gateway.resource;

import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

import com.sanedge.gateway.dto.CashierDto;
import com.sanedge.gateway.service.CashierService;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

@GraphQLApi
public class CashierResource {

        @Inject
        CashierService cashierService;

        @Query("cashiers")
        @Description("List all cashiers")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CashierDto.ApiResponsePaginationCashier> listCashiers(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return cashierService.listCashiers(page, size, search);
        }

        @Query("activeCashiers")
        @Description("Get active cashiers")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CashierDto.ApiResponsePaginationCashierDeleteAt> getActiveCashiers(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return cashierService.getActiveCashiers(page, size, search);
        }

        @Query("trashedCashiers")
        @Description("Get trashed cashiers")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CashierDto.ApiResponsePaginationCashierDeleteAt> getTrashedCashiers(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return cashierService.getTrashedCashiers(page, size, search);
        }

        @Query("cashier")
        @Description("Get cashier by ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CashierDto.ApiResponseCashier> getCashier(@Name("id") int id) {
                return cashierService.getCashier(id);
        }

        @Query("cashiersByMerchant")
        @Description("Get cashiers by merchant ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CashierDto.ApiResponsePaginationCashier> getCashiersByMerchant(
                        @Name("merchantId") int merchantId,
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return cashierService.getCashiersByMerchant(merchantId, page, size, search);
        }

        @Mutation("createCashier")
        @Description("Create a new cashier")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CashierDto.ApiResponseCashier> createCashier(@Name("body") CashierDto.CreateCashierRequest body) {
                return cashierService.createCashier(body);
        }

        @Mutation("updateCashier")
        @Description("Update cashier")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CashierDto.ApiResponseCashier> updateCashier(@Name("id") int id,
                        @Name("body") CashierDto.UpdateCashierRequest body) {
                return cashierService.updateCashier(id, body);
        }

        @Mutation("deleteCashier")
        @Description("Soft-delete a cashier")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CashierDto.ApiResponseCashierDeleteAt> deleteCashier(@Name("id") int id) {
                return cashierService.deleteCashier(id);
        }

        @Mutation("restoreCashier")
        @Description("Restore a soft-deleted cashier")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CashierDto.ApiResponseCashierDeleteAt> restoreCashier(@Name("id") int id) {
                return cashierService.restoreCashier(id);
        }

        @Mutation("deleteCashierPermanent")
        @Description("Permanently delete a cashier")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<CashierDto.ApiResponseCashierDelete> deleteCashierPermanent(@Name("id") int id) {
                return cashierService.deleteCashierPermanent(id);
        }

        @Mutation("restoreAllCashiers")
        @Description("Restore all soft-deleted cashiers")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<CashierDto.ApiResponseCashierAll> restoreAllCashier() {
                return cashierService.restoreAllCashier();
        }

        @Mutation("deleteAllCashiersPermanent")
        @Description("Permanently delete all cashiers")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<CashierDto.ApiResponseCashierAll> deleteAllCashierPermanent() {
                return cashierService.deleteAllCashierPermanent();
        }

        @Query("cashierMonthlyTotalSales")
        @Description("Get monthly total sales stats")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CashierDto.ApiResponseCashierMonthlyTotalSales> getMonthlyTotalSales(
                        @Name("year") int year,
                        @Name("month") int month) {
                return cashierService.getMonthlyTotalSales(year, month);
        }

        @Query("cashierYearlyTotalSales")
        @Description("Get yearly total sales stats")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CashierDto.ApiResponseCashierYearlyTotalSales> getYearlyTotalSales(@Name("year") int year) {
                return cashierService.getYearlyTotalSales(year);
        }

        @Query("cashierMonthlyTotalSalesById")
        @Description("Get monthly total sales stats by cashier ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CashierDto.ApiResponseCashierMonthlyTotalSales> getMonthlyTotalSalesById(
                        @Name("cashierId") int cashierId,
                        @Name("year") int year,
                        @Name("month") int month) {
                return cashierService.getMonthlyTotalSalesById(cashierId, year, month);
        }

        @Query("cashierYearlyTotalSalesById")
        @Description("Get yearly total sales stats by cashier ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CashierDto.ApiResponseCashierYearlyTotalSales> getYearlyTotalSalesById(
                        @Name("cashierId") int cashierId,
                        @Name("year") int year) {
                return cashierService.getYearlyTotalSalesById(cashierId, year);
        }

        @Query("cashierMonthlyTotalSalesByMerchant")
        @Description("Get monthly total sales stats by merchant ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CashierDto.ApiResponseCashierMonthlyTotalSales> getMonthlyTotalSalesByMerchant(
                        @Name("merchantId") int merchantId,
                        @Name("year") int year,
                        @Name("month") int month) {
                return cashierService.getMonthlyTotalSalesByMerchant(merchantId, year, month);
        }

        @Query("cashierYearlyTotalSalesByMerchant")
        @Description("Get yearly total sales stats by merchant ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CashierDto.ApiResponseCashierYearlyTotalSales> getYearlyTotalSalesByMerchant(
                        @Name("merchantId") int merchantId,
                        @Name("year") int year) {
                return cashierService.getYearlyTotalSalesByMerchant(merchantId, year);
        }

        @Query("cashierMonthlySales")
        @Description("Get monthly sales stats")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CashierDto.ApiResponseCashierMonthSales> getMonthSales(@Name("year") int year) {
                return cashierService.getMonthSales(year);
        }

        @Query("cashierYearlySales")
        @Description("Get yearly sales stats")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CashierDto.ApiResponseCashierYearSales> getYearSales(@Name("year") int year) {
                return cashierService.getYearSales(year);
        }

        @Query("cashierMonthlySalesByMerchant")
        @Description("Get monthly sales stats by merchant ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CashierDto.ApiResponseCashierMonthSales> getMonthSalesByMerchant(
                        @Name("merchantId") int merchantId,
                        @Name("year") int year) {
                return cashierService.getMonthSalesByMerchant(merchantId, year);
        }

        @Query("cashierYearlySalesByMerchant")
        @Description("Get yearly sales stats by merchant ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CashierDto.ApiResponseCashierYearSales> getYearSalesByMerchant(
                        @Name("merchantId") int merchantId,
                        @Name("year") int year) {
                return cashierService.getYearSalesByMerchant(merchantId, year);
        }

        @Query("cashierMonthlySalesById")
        @Description("Get monthly sales stats by cashier ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CashierDto.ApiResponseCashierMonthSales> getMonthSalesById(
                        @Name("cashierId") int cashierId,
                        @Name("year") int year) {
                return cashierService.getMonthSalesById(cashierId, year);
        }

        @Query("cashierYearlySalesById")
        @Description("Get yearly sales stats by cashier ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CashierDto.ApiResponseCashierYearSales> getYearSalesById(
                        @Name("cashierId") int cashierId,
                        @Name("year") int year) {
                return cashierService.getYearSalesById(cashierId, year);
        }
}
