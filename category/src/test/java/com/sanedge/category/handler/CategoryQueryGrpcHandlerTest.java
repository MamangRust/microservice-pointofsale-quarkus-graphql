package com.sanedge.category.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.category.domain.response.CategoryResponse;
import com.sanedge.category.domain.response.CategoryResponseDeleteAt;
import com.sanedge.category.service.CategoryQueryService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.category.Category;
import pb.category.CategoryQuery;

@ExtendWith(MockitoExtension.class)
class CategoryQueryGrpcHandlerTest {

    @Mock
    private CategoryQueryService categoryQueryService;

    private CategoryQueryGrpcHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CategoryQueryGrpcHandler();
        handler.categoryQueryService = categoryQueryService;
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
    @DisplayName("findAll - success")
    void findAll_Success() {
        Category.FindAllCategoryRequest request = Category.FindAllCategoryRequest.newBuilder()
                .setPage(1).setPageSize(10).build();
        CategoryResponse data = createCategoryResponse(1L);
        ApiResponsePagination<List<CategoryResponse>> apiResp = new ApiResponsePagination<>(
                "success", "Categories retrieved", List.of(data), null);
        when(categoryQueryService.findAll(any())).thenReturn(Uni.createFrom().item(apiResp));

        CategoryQuery.ApiResponsePaginationCategory response = handler.findAll(request).await().indefinitely();

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
        assertThat(response.getData(0).getName()).isEqualTo("Test Category");
    }

    @Test
    @DisplayName("findAll - error")
    void findAll_Error() {
        when(categoryQueryService.findAll(any()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));
        try {
            handler.findAll(Category.FindAllCategoryRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("findById - success")
    void findById_Success() {
        Category.FindByIdCategoryRequest request = Category.FindByIdCategoryRequest.newBuilder().setId(1).build();
        CategoryResponse data = createCategoryResponse(1L);
        ApiResponse<CategoryResponse> apiResp = ApiResponse.success("Category found", data);
        when(categoryQueryService.findById(anyInt())).thenReturn(Uni.createFrom().item(apiResp));

        Category.ApiResponseCategory response = handler.findById(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("findById - error")
    void findById_Error() {
        when(categoryQueryService.findById(anyInt()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.findById(Category.FindByIdCategoryRequest.newBuilder().setId(1).build()).await().indefinitely();
        } catch (StatusRuntimeException e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("findByActive - success")
    void findByActive_Success() {
        Category.FindAllCategoryRequest request = Category.FindAllCategoryRequest.newBuilder().setPage(1).build();
        CategoryResponseDeleteAt data = createCategoryDeleteAt(1L);
        ApiResponsePagination<List<CategoryResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                "success", "Active categories", List.of(data), null);
        when(categoryQueryService.findByActive(any())).thenReturn(Uni.createFrom().item(apiResp));

        CategoryQuery.ApiResponsePaginationCategoryDeleteAt response = handler.findByActive(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
        assertThat(response.getData(0).hasDeletedAt()).isTrue();
    }

    @Test
    @DisplayName("findByActive - error")
    void findByActive_Error() {
        when(categoryQueryService.findByActive(any()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.findByActive(Category.FindAllCategoryRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("findByTrashed - success")
    void findByTrashed_Success() {
        Category.FindAllCategoryRequest request = Category.FindAllCategoryRequest.newBuilder().build();
        CategoryResponseDeleteAt data = createCategoryDeleteAt(2L);
        ApiResponsePagination<List<CategoryResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                "success", "Trashed categories", List.of(data), null);
        when(categoryQueryService.findByTrashed(any())).thenReturn(Uni.createFrom().item(apiResp));

        CategoryQuery.ApiResponsePaginationCategoryDeleteAt response = handler.findByTrashed(request).await().indefinitely();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData(0).hasDeletedAt()).isTrue();
    }

    @Test
    @DisplayName("findByTrashed - error")
    void findByTrashed_Error() {
        when(categoryQueryService.findByTrashed(any()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
        try {
            handler.findByTrashed(Category.FindAllCategoryRequest.newBuilder().build()).await().indefinitely();
        } catch (StatusRuntimeException e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("findAll - empty list")
    void findAll_Empty() {
        when(categoryQueryService.findAll(any()))
                .thenReturn(Uni.createFrom().item(new ApiResponsePagination<>("success", "No categories", List.of(), null)));
        CategoryQuery.ApiResponsePaginationCategory response = handler.findAll(
                Category.FindAllCategoryRequest.newBuilder().build()).await().indefinitely();
        assertThat(response.getDataCount()).isZero();
    }

    @Test
    @DisplayName("findById - null data")
    void findById_NullData() {
        when(categoryQueryService.findById(anyInt()))
                .thenReturn(Uni.createFrom().item(ApiResponse.success("No data", null)));
        Category.ApiResponseCategory response = handler.findById(
                Category.FindByIdCategoryRequest.newBuilder().setId(1).build()).await().indefinitely();
        assertThat(response.hasData()).isFalse();
    }
}