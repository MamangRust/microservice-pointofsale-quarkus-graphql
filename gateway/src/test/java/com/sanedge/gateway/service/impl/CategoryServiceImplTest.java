package com.sanedge.gateway.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.CategoryDto;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    TelemetryHelper telemetryHelper;

    @Mock
    pb.category.MutinyCategoryQueryServiceGrpc.MutinyCategoryQueryServiceStub categoryQueryService;

    @Mock
    pb.category.MutinyCategoryCommandServiceGrpc.MutinyCategoryCommandServiceStub categoryCommandService;

    @Mock
    pb.category.stats.MutinyCategoryTotalPriceServiceGrpc.MutinyCategoryTotalPriceServiceStub categoryTotalPriceServiceStub;

    @Mock
    pb.category.stats.MutinyCategoryPriceServiceGrpc.MutinyCategoryPriceServiceStub categoryPriceServiceStub;

    CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() throws Exception {
        categoryService = new CategoryServiceImpl();

        setField(categoryService, "telemetryHelper", telemetryHelper);
        setField(categoryService, "categoryQueryService", categoryQueryService);
        setField(categoryService, "categoryCommandService", categoryCommandService);
        setField(categoryService, "categoryTotalPriceServiceStub", categoryTotalPriceServiceStub);
        setField(categoryService, "categoryPriceServiceStub", categoryPriceServiceStub);

        lenient().when(telemetryHelper.traceAndMetric(anyString(), any(java.util.function.Supplier.class)))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    java.util.function.Supplier<Uni<?>> supplier = inv.getArgument(1);
                    return supplier.get();
                });
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void listCategories_returnsSuccess() {
        pb.category.Category.CategoryResponse categoryProto = pb.category.Category.CategoryResponse.newBuilder()
                .setId(1)
                .setName("Category A")
                .setDescription("Desc A")
                .build();

        pb.category.CategoryQuery.ApiResponsePaginationCategory responseProto = 
                pb.category.CategoryQuery.ApiResponsePaginationCategory.newBuilder()
                        .addData(categoryProto)
                        .setStatus("success")
                        .setMessage("Categories found")
                        .build();

        when(categoryQueryService.findAll(any(pb.category.Category.FindAllCategoryRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CategoryDto.ApiResponsePaginationCategory result = 
                categoryService.listCategories(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).name()).isEqualTo("Category A");
    }

    @Test
    void getCategory_returnsSuccess() {
        pb.category.Category.CategoryResponse categoryProto = pb.category.Category.CategoryResponse.newBuilder()
                .setId(1)
                .setName("Category A")
                .build();

        pb.category.Category.ApiResponseCategory responseProto = 
                pb.category.Category.ApiResponseCategory.newBuilder()
                        .setData(categoryProto)
                        .setStatus("success")
                        .setMessage("Category found")
                        .build();

        when(categoryQueryService.findById(any(pb.category.Category.FindByIdCategoryRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CategoryDto.ApiResponseCategory result = categoryService.getCategory(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().id()).isEqualTo(1);
    }

    @Test
    void getActiveCategories_returnsSuccess() {
        pb.category.Category.CategoryResponseDeleteAt categoryProto = pb.category.Category.CategoryResponseDeleteAt.newBuilder()
                .setId(1)
                .setName("Active Category")
                .build();

        pb.category.CategoryQuery.ApiResponsePaginationCategoryDeleteAt responseProto = 
                pb.category.CategoryQuery.ApiResponsePaginationCategoryDeleteAt.newBuilder()
                        .addData(categoryProto)
                        .setStatus("success")
                        .setMessage("Active categories found")
                        .build();

        when(categoryQueryService.findByActive(any(pb.category.Category.FindAllCategoryRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CategoryDto.ApiResponsePaginationCategoryDeleteAt result = 
                categoryService.getActiveCategories(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
    }

    @Test
    void getTrashedCategories_returnsSuccess() {
        pb.category.Category.CategoryResponseDeleteAt categoryProto = pb.category.Category.CategoryResponseDeleteAt.newBuilder()
                .setId(1)
                .setName("Trashed Category")
                .build();

        pb.category.CategoryQuery.ApiResponsePaginationCategoryDeleteAt responseProto = 
                pb.category.CategoryQuery.ApiResponsePaginationCategoryDeleteAt.newBuilder()
                        .addData(categoryProto)
                        .setStatus("success")
                        .setMessage("Trashed categories found")
                        .build();

        when(categoryQueryService.findByTrashed(any(pb.category.Category.FindAllCategoryRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CategoryDto.ApiResponsePaginationCategoryDeleteAt result = 
                categoryService.getTrashedCategories(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
    }

    @Test
    void createCategory_returnsSuccess() {
        pb.category.Category.CategoryResponse categoryProto = pb.category.Category.CategoryResponse.newBuilder()
                .setId(1)
                .setName("New Category")
                .setDescription("Description")
                .build();

        pb.category.Category.ApiResponseCategory responseProto = 
                pb.category.Category.ApiResponseCategory.newBuilder()
                        .setData(categoryProto)
                        .setStatus("success")
                        .setMessage("Category created")
                        .build();

        when(categoryCommandService.create(any(pb.category.CategoryCommand.CreateCategoryRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CategoryDto.CreateCategoryRequest request = new CategoryDto.CreateCategoryRequest("New Category", "Description", "image.png");
        CategoryDto.ApiResponseCategory result = categoryService.createCategory(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().name()).isEqualTo("New Category");
    }

    @Test
    void updateCategory_returnsSuccess() {
        pb.category.Category.CategoryResponse categoryProto = pb.category.Category.CategoryResponse.newBuilder()
                .setId(1)
                .setName("Updated Category")
                .setDescription("Updated Desc")
                .build();

        pb.category.Category.ApiResponseCategory responseProto = 
                pb.category.Category.ApiResponseCategory.newBuilder()
                        .setData(categoryProto)
                        .setStatus("success")
                        .setMessage("Category updated")
                        .build();

        when(categoryCommandService.update(any(pb.category.CategoryCommand.UpdateCategoryRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CategoryDto.UpdateCategoryRequest request = new CategoryDto.UpdateCategoryRequest("Updated Category", "Updated Desc", "updated.png");
        CategoryDto.ApiResponseCategory result = categoryService.updateCategory(1, request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().name()).isEqualTo("Updated Category");
    }

    @Test
    void deleteCategory_returnsSuccess() {
        pb.category.Category.CategoryResponseDeleteAt categoryProto = pb.category.Category.CategoryResponseDeleteAt.newBuilder()
                .setId(1)
                .setName("Category")
                .setDeletedAt(com.google.protobuf.StringValue.of("2024-06-01T00:00:00Z"))
                .build();

        pb.category.Category.ApiResponseCategoryDeleteAt responseProto = 
                pb.category.Category.ApiResponseCategoryDeleteAt.newBuilder()
                        .setData(categoryProto)
                        .setStatus("success")
                        .setMessage("Category trashed")
                        .build();

        when(categoryCommandService.trashedCategory(any(pb.category.Category.FindByIdCategoryRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CategoryDto.ApiResponseCategoryDeleteAt result = categoryService.deleteCategory(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().id()).isEqualTo(1);
    }

    @Test
    void restoreCategory_returnsSuccess() {
        pb.category.Category.CategoryResponseDeleteAt categoryProto = pb.category.Category.CategoryResponseDeleteAt.newBuilder()
                .setId(1)
                .setName("Category")
                .build();

        pb.category.Category.ApiResponseCategoryDeleteAt responseProto = 
                pb.category.Category.ApiResponseCategoryDeleteAt.newBuilder()
                        .setData(categoryProto)
                        .setStatus("success")
                        .setMessage("Category restored")
                        .build();

        when(categoryCommandService.restoreCategory(any(pb.category.Category.FindByIdCategoryRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CategoryDto.ApiResponseCategoryDeleteAt result = categoryService.restoreCategory(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Category restored");
    }

    @Test
    void deleteCategoryPermanent_returnsSuccess() {
        pb.category.CategoryCommand.ApiResponseCategoryDelete responseProto = 
                pb.category.CategoryCommand.ApiResponseCategoryDelete.newBuilder()
                        .setStatus("success")
                        .setMessage("Category permanently deleted")
                        .build();

        when(categoryCommandService.deleteCategoryPermanent(any(pb.category.Category.FindByIdCategoryRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CategoryDto.ApiResponseCategoryDelete result = categoryService.deleteCategoryPermanent(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Category permanently deleted");
    }

    @Test
    void restoreAllCategory_returnsSuccess() {
        pb.category.CategoryCommand.ApiResponseCategoryAll responseProto = 
                pb.category.CategoryCommand.ApiResponseCategoryAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All categories restored")
                        .build();

        when(categoryCommandService.restoreAllCategory(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CategoryDto.ApiResponseCategoryAll result = categoryService.restoreAllCategory().await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("All categories restored");
    }

    @Test
    void getMonthlyTotalPrices_returnsSuccess() {
        pb.category.Category.CategoriesMonthlyTotalPriceResponse dataProto = 
                pb.category.Category.CategoriesMonthlyTotalPriceResponse.newBuilder()
                        .setYear("2024")
                        .setMonth("6")
                        .setTotalRevenue(1500)
                        .build();

        pb.category.Category.ApiResponseCategoryMonthlyTotalPrice responseProto = 
                pb.category.Category.ApiResponseCategoryMonthlyTotalPrice.newBuilder()
                        .addData(dataProto)
                        .setStatus("success")
                        .setMessage("Monthly total prices")
                        .build();

        when(categoryTotalPriceServiceStub.findMonthlyTotalPrices(any(pb.category.Category.FindYearMonthTotalPrices.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CategoryDto.ApiResponseCategoryMonthlyTotalPrice result = 
                categoryService.getMonthlyTotalPrices(2024, 6).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data().get(0).totalRevenue()).isEqualTo(1500);
    }

    @Test
    void getMonthlyPrices_returnsSuccess() {
        pb.category.Category.CategoryMonthPriceResponse dataProto = 
                pb.category.Category.CategoryMonthPriceResponse.newBuilder()
                        .setMonth("6")
                        .setTotalRevenue(2000)
                        .build();

        pb.category.Category.ApiResponseCategoryMonthPrice responseProto = 
                pb.category.Category.ApiResponseCategoryMonthPrice.newBuilder()
                        .addData(dataProto)
                        .setStatus("success")
                        .setMessage("Monthly prices")
                        .build();

        when(categoryPriceServiceStub.findMonthPrice(any(pb.category.Category.FindYearCategory.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CategoryDto.ApiResponseCategoryMonthPrice result = 
                categoryService.getMonthlyPrices(2024).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).totalRevenue()).isEqualTo(2000);
    }
}
