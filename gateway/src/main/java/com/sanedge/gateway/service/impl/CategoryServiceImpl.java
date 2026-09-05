package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.CategoryDto;
import com.sanedge.gateway.service.CategoryService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CategoryServiceImpl implements CategoryService {

    private static final Logger LOG = Logger.getLogger(CategoryServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("category")
    pb.category.MutinyCategoryQueryServiceGrpc.MutinyCategoryQueryServiceStub categoryQueryService;

    @GrpcClient("category")
    pb.category.MutinyCategoryCommandServiceGrpc.MutinyCategoryCommandServiceStub categoryCommandService;

    @GrpcClient("stats-reader")
    pb.category.stats.MutinyCategoryTotalPriceServiceGrpc.MutinyCategoryTotalPriceServiceStub categoryTotalPriceServiceStub;

    @GrpcClient("stats-reader")
    pb.category.stats.MutinyCategoryPriceServiceGrpc.MutinyCategoryPriceServiceStub categoryPriceServiceStub;

    @Override
    public Uni<CategoryDto.ApiResponsePaginationCategory> listCategories(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("category.listCategories", () -> categoryQueryService.findAll(pb.category.Category.FindAllCategoryRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(CategoryDto.ApiResponsePaginationCategory::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list categories: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategory> getCategory(int id) {
        return telemetryHelper.traceAndMetric("category.getCategory", () -> categoryQueryService.findById(pb.category.Category.FindByIdCategoryRequest.newBuilder()
                .setId(id)
                .build())
                .map(CategoryDto.ApiResponseCategory::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get category " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponsePaginationCategoryDeleteAt> getActiveCategories(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("category.getActiveCategories", () -> categoryQueryService.findByActive(pb.category.Category.FindAllCategoryRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(CategoryDto.ApiResponsePaginationCategoryDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list active categories: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponsePaginationCategoryDeleteAt> getTrashedCategories(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("category.getTrashedCategories", () -> categoryQueryService.findByTrashed(pb.category.Category.FindAllCategoryRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(CategoryDto.ApiResponsePaginationCategoryDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list trashed categories: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategory> createCategory(CategoryDto.CreateCategoryRequest body) {
        return telemetryHelper.traceAndMetric("category.createCategory", () -> categoryCommandService.create(pb.category.CategoryCommand.CreateCategoryRequest.newBuilder()
                .setName(body.name())
                .setDescription(body.description())
                .build())
                .map(CategoryDto.ApiResponseCategory::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create category: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategory> updateCategory(int id, CategoryDto.UpdateCategoryRequest body) {
        return telemetryHelper.traceAndMetric("category.updateCategory", () -> categoryCommandService.update(pb.category.CategoryCommand.UpdateCategoryRequest.newBuilder()
                .setCategoryId(id)
                .setName(body.name())
                .setDescription(body.description())
                .build())
                .map(CategoryDto.ApiResponseCategory::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update category " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategoryDeleteAt> deleteCategory(int id) {
        return telemetryHelper.traceAndMetric("category.deleteCategory", () -> categoryCommandService.trashedCategory(pb.category.Category.FindByIdCategoryRequest.newBuilder()
                .setId(id)
                .build())
                .map(CategoryDto.ApiResponseCategoryDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete category " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategoryDeleteAt> restoreCategory(int id) {
        return telemetryHelper.traceAndMetric("category.restoreCategory", () -> categoryCommandService.restoreCategory(pb.category.Category.FindByIdCategoryRequest.newBuilder()
                .setId(id)
                .build())
                .map(CategoryDto.ApiResponseCategoryDeleteAt::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore category " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategoryDelete> deleteCategoryPermanent(int id) {
        return telemetryHelper.traceAndMetric("category.deleteCategoryPermanent", () -> categoryCommandService.deleteCategoryPermanent(pb.category.Category.FindByIdCategoryRequest.newBuilder()
                .setId(id)
                .build())
                .map(CategoryDto.ApiResponseCategoryDelete::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete category " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategoryAll> restoreAllCategory() {
        return telemetryHelper.traceAndMetric("category.restoreAllCategory", () -> categoryCommandService.restoreAllCategory(com.google.protobuf.Empty.getDefaultInstance())
                .map(CategoryDto.ApiResponseCategoryAll::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all categories: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategoryAll> deleteAllCategoryPermanent() {
        return telemetryHelper.traceAndMetric("category.deleteAllCategoryPermanent", () -> categoryCommandService.deleteAllCategoryPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .map(CategoryDto.ApiResponseCategoryAll::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all categories: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategoryMonthlyTotalPrice> getMonthlyTotalPrices(int year, int month) {
        return telemetryHelper.traceAndMetric("category.getMonthlyTotalPrices", () -> categoryTotalPriceServiceStub.findMonthlyTotalPrices(pb.category.Category.FindYearMonthTotalPrices.newBuilder()
                .setYear(year)
                .setMonth(month)
                .build())
                .map(CategoryDto.ApiResponseCategoryMonthlyTotalPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly total prices: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategoryYearlyTotalPrice> getYearlyTotalPrices(int year) {
        return telemetryHelper.traceAndMetric("category.getYearlyTotalPrices", () -> categoryTotalPriceServiceStub.findYearlyTotalPrices(pb.category.Category.FindYearTotalPrices.newBuilder()
                .setYear(year)
                .build())
                .map(CategoryDto.ApiResponseCategoryYearlyTotalPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly total prices: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategoryMonthlyTotalPrice> getMonthlyTotalPricesById(int categoryId, int year, int month) {
        return telemetryHelper.traceAndMetric("category.getMonthlyTotalPricesById", () -> categoryTotalPriceServiceStub.findMonthlyTotalPricesById(pb.category.Category.FindYearMonthTotalPriceById.newBuilder()
                .setCategoryId(categoryId)
                .setYear(year)
                .setMonth(month)
                .build())
                .map(CategoryDto.ApiResponseCategoryMonthlyTotalPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly total prices for category " + categoryId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategoryYearlyTotalPrice> getYearlyTotalPricesById(int categoryId, int year) {
        return telemetryHelper.traceAndMetric("category.getYearlyTotalPricesById", () -> categoryTotalPriceServiceStub.findYearlyTotalPricesById(pb.category.Category.FindYearTotalPriceById.newBuilder()
                .setCategoryId(categoryId)
                .setYear(year)
                .build())
                .map(CategoryDto.ApiResponseCategoryYearlyTotalPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly total prices for category " + categoryId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategoryMonthlyTotalPrice> getMonthlyTotalPricesByMerchant(int merchantId, int year, int month) {
        return telemetryHelper.traceAndMetric("category.getMonthlyTotalPricesByMerchant", () -> categoryTotalPriceServiceStub.findMonthlyTotalPricesByMerchant(pb.category.Category.FindYearMonthTotalPriceByMerchant.newBuilder()
                .setMerchantId(merchantId)
                .setYear(year)
                .setMonth(month)
                .build())
                .map(CategoryDto.ApiResponseCategoryMonthlyTotalPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly total prices for merchant " + merchantId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategoryYearlyTotalPrice> getYearlyTotalPricesByMerchant(int merchantId, int year) {
        return telemetryHelper.traceAndMetric("category.getYearlyTotalPricesByMerchant", () -> categoryTotalPriceServiceStub.findYearlyTotalPricesByMerchant(pb.category.Category.FindYearTotalPriceByMerchant.newBuilder()
                .setMerchantId(merchantId)
                .setYear(year)
                .build())
                .map(CategoryDto.ApiResponseCategoryYearlyTotalPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly total prices for merchant " + merchantId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategoryMonthPrice> getMonthlyPrices(int year) {
        return telemetryHelper.traceAndMetric("category.getMonthlyPrices", () -> categoryPriceServiceStub.findMonthPrice(pb.category.Category.FindYearCategory.newBuilder()
                .setYear(year)
                .build())
                .map(CategoryDto.ApiResponseCategoryMonthPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly prices: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategoryYearPrice> getYearlyPrices(int year) {
        return telemetryHelper.traceAndMetric("category.getYearlyPrices", () -> categoryPriceServiceStub.findYearPrice(pb.category.Category.FindYearCategory.newBuilder()
                .setYear(year)
                .build())
                .map(CategoryDto.ApiResponseCategoryYearPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly prices: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategoryMonthPrice> getMonthlyPricesByMerchant(int merchantId, int year) {
        return telemetryHelper.traceAndMetric("category.getMonthlyPricesByMerchant", () -> categoryPriceServiceStub.findMonthPriceByMerchant(pb.category.Category.FindYearCategoryByMerchant.newBuilder()
                .setMerchantId(merchantId)
                .setYear(year)
                .build())
                .map(CategoryDto.ApiResponseCategoryMonthPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly prices for merchant " + merchantId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategoryYearPrice> getYearlyPricesByMerchant(int merchantId, int year) {
        return telemetryHelper.traceAndMetric("category.getYearlyPricesByMerchant", () -> categoryPriceServiceStub.findYearPriceByMerchant(pb.category.Category.FindYearCategoryByMerchant.newBuilder()
                .setMerchantId(merchantId)
                .setYear(year)
                .build())
                .map(CategoryDto.ApiResponseCategoryYearPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly prices for merchant " + merchantId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategoryMonthPrice> getMonthlyPricesById(int categoryId, int year) {
        return telemetryHelper.traceAndMetric("category.getMonthlyPricesById", () -> categoryPriceServiceStub.findMonthPriceById(pb.category.Category.FindYearCategoryById.newBuilder()
                .setCategoryId(categoryId)
                .setYear(year)
                .build())
                .map(CategoryDto.ApiResponseCategoryMonthPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly prices for category " + categoryId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CategoryDto.ApiResponseCategoryYearPrice> getYearlyPricesById(int categoryId, int year) {
        return telemetryHelper.traceAndMetric("category.getYearlyPricesById", () -> categoryPriceServiceStub.findYearPriceById(pb.category.Category.FindYearCategoryById.newBuilder()
                .setCategoryId(categoryId)
                .setYear(year)
                .build())
                .map(CategoryDto.ApiResponseCategoryYearPrice::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly prices for category " + categoryId + ": " + throwable.getMessage(), throwable)));
    }
}
