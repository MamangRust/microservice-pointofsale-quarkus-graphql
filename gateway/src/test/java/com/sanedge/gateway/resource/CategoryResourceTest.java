package com.sanedge.gateway.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.CategoryDto;
import com.sanedge.gateway.service.CategoryService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class CategoryResourceTest {

    @Mock
    CategoryService categoryService;

    CategoryResource resource;

    @BeforeEach
    void setUp() throws Exception {
        resource = new CategoryResource();
        Field f = CategoryResource.class.getDeclaredField("categoryService");
        f.setAccessible(true);
        f.set(resource, categoryService);
    }

    @Test void listCategories_ok() {
        when(categoryService.listCategories(anyInt(), anyInt(), anyString()))
            .thenReturn(Uni.createFrom().item(new CategoryDto.ApiResponsePaginationCategory("success", "ok", List.of(), null)));
        assertThat(resource.listCategories(1, 10, "").await().indefinitely().status()).isEqualTo("success");
    }

    @Test void getCategory_ok() {
        when(categoryService.getCategory(anyInt()))
            .thenReturn(Uni.createFrom().item(new CategoryDto.ApiResponseCategory("success", "ok", null)));
        assertThat(resource.getCategory(1).await().indefinitely().status()).isEqualTo("success");
    }

    @Test void createCategory_ok() {
        when(categoryService.createCategory(any()))
            .thenReturn(Uni.createFrom().item(new CategoryDto.ApiResponseCategory("success", "created", null)));
        assertThat(resource.createCategory(new CategoryDto.CreateCategoryRequest("n", "d", null)).await().indefinitely().message()).isEqualTo("created");
    }

    @Test void deleteCategory_ok() {
        when(categoryService.deleteCategory(anyInt()))
            .thenReturn(Uni.createFrom().item(new CategoryDto.ApiResponseCategoryDeleteAt("success", "trashed", null)));
        assertThat(resource.deleteCategory(1).await().indefinitely().message()).isEqualTo("trashed");
    }

    @Test void restoreCategory_ok() {
        when(categoryService.restoreCategory(anyInt()))
            .thenReturn(Uni.createFrom().item(new CategoryDto.ApiResponseCategoryDeleteAt("success", "restored", null)));
        assertThat(resource.restoreCategory(1).await().indefinitely().message()).isEqualTo("restored");
    }

    @Test void deleteCategoryPermanent_ok() {
        when(categoryService.deleteCategoryPermanent(anyInt()))
            .thenReturn(Uni.createFrom().item(new CategoryDto.ApiResponseCategoryDelete("success", "deleted")));
        assertThat(resource.deleteCategoryPermanent(1).await().indefinitely().status()).isEqualTo("success");
    }
}
