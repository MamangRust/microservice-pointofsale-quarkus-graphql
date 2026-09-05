package com.sanedge.category.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.protobuf.Empty;
import com.sanedge.category.domain.response.CategoryResponse;
import com.sanedge.category.domain.response.CategoryResponseDeleteAt;
import com.sanedge.category.service.CategoryCommandService;
import com.sanedge.common.domain.response.ApiResponse;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.category.Category;
import pb.category.CategoryCommand;

@ExtendWith(MockitoExtension.class)
class CategoryCommandGrpcHandlerTest {

    @Mock
    private CategoryCommandService categoryCommandService;

    private CategoryCommandGrpcHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CategoryCommandGrpcHandler();
        handler.categoryCommandService = categoryCommandService;
    }

    private CategoryResponse createCategoryResponse(Long id) {
        CategoryResponse r = new CategoryResponse();
        r.setId(id);
        r.setName("Test Category");
        r.setDescription("desc");
        r.setSlugCategory("test");
        r.setImageCategory("img.png");
        r.setCreatedAt(LocalDateTime.now().toString());
        r.setUpdatedAt(LocalDateTime.now().toString());
        return r;
    }

    private CategoryResponseDeleteAt createCategoryDeleteAt(Long id) {
        CategoryResponseDeleteAt r = new CategoryResponseDeleteAt();
        r.setId(id);
        r.setName("Trashed");
        r.setDescription("desc");
        r.setSlugCategory("trashed");
        r.setImageCategory("img.png");
        r.setCreatedAt(LocalDateTime.now().toString());
        r.setUpdatedAt(LocalDateTime.now().toString());
        r.setDeletedAt(LocalDateTime.now().toString());
        return r;
    }

    @Test
    @DisplayName("create - success")
    void create_Success() {
        CategoryCommand.CreateCategoryRequest request = CategoryCommand.CreateCategoryRequest.newBuilder()
                .setName("New Category")
                .setDescription("A new category")
                .build();

        CategoryResponse data = createCategoryResponse(1L);
        ApiResponse<CategoryResponse> apiResp = ApiResponse.success("Created", data);
        when(categoryCommandService.createCategory(any())).thenReturn(Uni.createFrom().item(apiResp));

        Category.ApiResponseCategory response = handler.create(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getName()).isEqualTo("Test Category");
        assertThat(response.getData().getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("create - error")
    void create_Error() {
        when(categoryCommandService.createCategory(any()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.create(CategoryCommand.CreateCategoryRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("update - success")
    void update_Success() {
        CategoryCommand.UpdateCategoryRequest request = CategoryCommand.UpdateCategoryRequest.newBuilder()
                .setCategoryId(1)
                .setName("Updated Name")
                .setDescription("Updated desc")
                .build();

        CategoryResponse data = createCategoryResponse(1L);
        data.setName("Updated Name");
        ApiResponse<CategoryResponse> apiResp = ApiResponse.success("Updated", data);
        when(categoryCommandService.updateCategory(any())).thenReturn(Uni.createFrom().item(apiResp));

        Category.ApiResponseCategory response = handler.update(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getName()).isEqualTo("Updated Name");
    }

    @Test
    @DisplayName("update - error")
    void update_Error() {
        when(categoryCommandService.updateCategory(any()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.update(CategoryCommand.UpdateCategoryRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("trashedCategory - success")
    void trashed_Success() {
        Category.FindByIdCategoryRequest request = Category.FindByIdCategoryRequest.newBuilder().setId(1).build();
        CategoryResponseDeleteAt data = createCategoryDeleteAt(1L);
        ApiResponse<CategoryResponseDeleteAt> apiResp = ApiResponse.success("Trashed", data);
        when(categoryCommandService.trashedCategory(anyInt())).thenReturn(Uni.createFrom().item(apiResp));

        Category.ApiResponseCategoryDeleteAt response = handler.trashedCategory(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().hasDeletedAt()).isTrue();
    }

    @Test
    @DisplayName("restoreCategory - success")
    void restore_Success() {
        Category.FindByIdCategoryRequest request = Category.FindByIdCategoryRequest.newBuilder().setId(1).build();
        CategoryResponseDeleteAt data = createCategoryDeleteAt(1L);
        data.setDeletedAt(null);
        ApiResponse<CategoryResponseDeleteAt> apiResp = ApiResponse.success("Restored", data);
        when(categoryCommandService.restoreCategory(anyInt())).thenReturn(Uni.createFrom().item(apiResp));

        Category.ApiResponseCategoryDeleteAt response = handler.restoreCategory(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().hasDeletedAt()).isFalse();
    }

    @Test
    @DisplayName("deleteCategoryPermanent - success")
    void deletePermanent_Success() {
        Category.FindByIdCategoryRequest request = Category.FindByIdCategoryRequest.newBuilder().setId(1).build();
        ApiResponse<Boolean> apiResp = ApiResponse.success("Permanently deleted", true);
        when(categoryCommandService.deleteCategoryPermanent(anyInt())).thenReturn(Uni.createFrom().item(apiResp));

        CategoryCommand.ApiResponseCategoryDelete response = handler.deleteCategoryPermanent(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Permanently deleted");
    }

    @Test
    @DisplayName("restoreAllCategory - success")
    void restoreAll_Success() {
        ApiResponse<Boolean> apiResp = ApiResponse.success("All restored", true);
        when(categoryCommandService.restoreAllCategories()).thenReturn(Uni.createFrom().item(apiResp));

        CategoryCommand.ApiResponseCategoryAll response = handler.restoreAllCategory(Empty.getDefaultInstance()).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("All restored");
    }

    @Test
    @DisplayName("deleteAllCategoryPermanent - success")
    void deleteAll_Success() {
        ApiResponse<Boolean> apiResp = ApiResponse.success("All permanently deleted", true);
        when(categoryCommandService.deleteAllCategoriesPermanent()).thenReturn(Uni.createFrom().item(apiResp));

        CategoryCommand.ApiResponseCategoryAll response = handler.deleteAllCategoryPermanent(Empty.getDefaultInstance()).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("All permanently deleted");
    }

    @Test
    @DisplayName("create - null data")
    void create_NullData() {
        when(categoryCommandService.createCategory(any()))
                .thenReturn(Uni.createFrom().item(ApiResponse.success("Created", null)));
        Category.ApiResponseCategory response = handler.create(
                CategoryCommand.CreateCategoryRequest.newBuilder().build()).await().indefinitely();
        assertThat(response.hasData()).isFalse();
    }
}