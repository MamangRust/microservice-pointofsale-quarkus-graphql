package com.sanedge.gateway.resource;

import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

import com.sanedge.gateway.dto.CategoryDto;
import com.sanedge.gateway.service.CategoryService;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

@GraphQLApi
public class CategoryResource {

        @Inject
        CategoryService categoryService;

        @Query("categories")
        @Description("List all categories")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<CategoryDto.ApiResponsePaginationCategory> listCategories(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return categoryService.listCategories(page, size, search);
        }

        @Query("category")
        @Description("Get category by ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<CategoryDto.ApiResponseCategory> getCategory(@Name("id") int id) {
                return categoryService.getCategory(id);
        }

        @Query("activeCategories")
        @Description("Get active categories")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<CategoryDto.ApiResponsePaginationCategoryDeleteAt> getActiveCategories(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return categoryService.getActiveCategories(page, size, search);
        }

        @Query("trashedCategories")
        @Description("Get trashed categories")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<CategoryDto.ApiResponsePaginationCategoryDeleteAt> getTrashedCategories(
                        @Name("page") @DefaultValue("1") int page,
                        @Name("size") @DefaultValue("20") int size,
                        @Name("search") String search) {
                return categoryService.getTrashedCategories(page, size, search);
        }

        @Mutation("createCategory")
        @Description("Create a new category")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CategoryDto.ApiResponseCategory> createCategory(
                        @Name("body") CategoryDto.CreateCategoryRequest body) {
                return categoryService.createCategory(body);
        }

        @Mutation("updateCategory")
        @Description("Update category")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CategoryDto.ApiResponseCategory> updateCategory(@Name("id") int id,
                        @Name("body") CategoryDto.UpdateCategoryRequest body) {
                return categoryService.updateCategory(id, body);
        }

        @Mutation("deleteCategory")
        @Description("Soft-delete a category")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CategoryDto.ApiResponseCategoryDeleteAt> deleteCategory(@Name("id") int id) {
                return categoryService.deleteCategory(id);
        }

        @Mutation("restoreCategory")
        @Description("Restore a soft-deleted category")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF" })
        public Uni<CategoryDto.ApiResponseCategoryDeleteAt> restoreCategory(@Name("id") int id) {
                return categoryService.restoreCategory(id);
        }

        @Mutation("deleteCategoryPermanent")
        @Description("Permanently delete a category")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<CategoryDto.ApiResponseCategoryDelete> deleteCategoryPermanent(@Name("id") int id) {
                return categoryService.deleteCategoryPermanent(id);
        }

        @Mutation("restoreAllCategories")
        @Description("Restore all soft-deleted categories")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<CategoryDto.ApiResponseCategoryAll> restoreAllCategory() {
                return categoryService.restoreAllCategory();
        }

        @Mutation("deleteAllCategoriesPermanent")
        @Description("Permanently delete all categories")
        @RolesAllowed({ "ROLE_ADMIN" })
        public Uni<CategoryDto.ApiResponseCategoryAll> deleteAllCategoryPermanent() {
                return categoryService.deleteAllCategoryPermanent();
        }

        @Query("categoryMonthlyTotalPrices")
        @Description("Get monthly total prices stats")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<CategoryDto.ApiResponseCategoryMonthlyTotalPrice> getMonthlyTotalPrices(
                        @Name("year") int year,
                        @Name("month") int month) {
                return categoryService.getMonthlyTotalPrices(year, month);
        }

        @Query("categoryYearlyTotalPrices")
        @Description("Get yearly total prices stats")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<CategoryDto.ApiResponseCategoryYearlyTotalPrice> getYearlyTotalPrices(@Name("year") int year) {
                return categoryService.getYearlyTotalPrices(year);
        }

        @Query("categoryMonthlyTotalPricesById")
        @Description("Get monthly total prices stats by category ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<CategoryDto.ApiResponseCategoryMonthlyTotalPrice> getMonthlyTotalPricesById(
                        @Name("categoryId") int categoryId,
                        @Name("year") int year,
                        @Name("month") int month) {
                return categoryService.getMonthlyTotalPricesById(categoryId, year, month);
        }

        @Query("categoryYearlyTotalPricesById")
        @Description("Get yearly total prices stats by category ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<CategoryDto.ApiResponseCategoryYearlyTotalPrice> getYearlyTotalPricesById(
                        @Name("categoryId") int categoryId,
                        @Name("year") int year) {
                return categoryService.getYearlyTotalPricesById(categoryId, year);
        }

        @Query("categoryMonthlyTotalPricesByMerchant")
        @Description("Get monthly total prices stats by merchant ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<CategoryDto.ApiResponseCategoryMonthlyTotalPrice> getMonthlyTotalPricesByMerchant(
                        @Name("merchantId") int merchantId,
                        @Name("year") int year,
                        @Name("month") int month) {
                return categoryService.getMonthlyTotalPricesByMerchant(merchantId, year, month);
        }

        @Query("categoryYearlyTotalPricesByMerchant")
        @Description("Get yearly total prices stats by merchant ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<CategoryDto.ApiResponseCategoryYearlyTotalPrice> getYearlyTotalPricesByMerchant(
                        @Name("merchantId") int merchantId,
                        @Name("year") int year) {
                return categoryService.getYearlyTotalPricesByMerchant(merchantId, year);
        }

        @Query("categoryMonthlyPrices")
        @Description("Get monthly prices stats")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<CategoryDto.ApiResponseCategoryMonthPrice> getMonthlyPrices(@Name("year") int year) {
                return categoryService.getMonthlyPrices(year);
        }

        @Query("categoryYearlyPrices")
        @Description("Get yearly prices stats")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<CategoryDto.ApiResponseCategoryYearPrice> getYearlyPrices(@Name("year") int year) {
                return categoryService.getYearlyPrices(year);
        }

        @Query("categoryMonthlyPricesByMerchant")
        @Description("Get monthly prices stats by merchant ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<CategoryDto.ApiResponseCategoryMonthPrice> getMonthlyPricesByMerchant(
                        @Name("merchantId") int merchantId,
                        @Name("year") int year) {
                return categoryService.getMonthlyPricesByMerchant(merchantId, year);
        }

        @Query("categoryYearlyPricesByMerchant")
        @Description("Get yearly prices stats by merchant ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<CategoryDto.ApiResponseCategoryYearPrice> getYearlyPricesByMerchant(
                        @Name("merchantId") int merchantId,
                        @Name("year") int year) {
                return categoryService.getYearlyPricesByMerchant(merchantId, year);
        }

        @Query("categoryMonthlyPricesById")
        @Description("Get monthly prices stats by category ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<CategoryDto.ApiResponseCategoryMonthPrice> getMonthlyPricesById(
                        @Name("categoryId") int categoryId,
                        @Name("year") int year) {
                return categoryService.getMonthlyPricesById(categoryId, year);
        }

        @Query("categoryYearlyPricesById")
        @Description("Get yearly prices stats by category ID")
        @RolesAllowed({ "ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER" })
        public Uni<CategoryDto.ApiResponseCategoryYearPrice> getYearlyPricesById(
                        @Name("categoryId") int categoryId,
                        @Name("year") int year) {
                return categoryService.getYearlyPricesById(categoryId, year);
        }
}
