package com.sanedge.category.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.category.domain.requests.FindAllCategory;
import com.sanedge.category.domain.response.CategoryResponse;
import com.sanedge.category.domain.response.CategoryResponseDeleteAt;
import com.sanedge.category.entity.Category;
import com.sanedge.category.repository.CategoryQueryRepository;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class CategoryQueryServiceImplTest {

    @Mock
    private CategoryQueryRepository categoryQueryRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    private CategoryQueryServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new CategoryQueryServiceImpl(
                categoryQueryRepository,
                redisService,
                objectMapper,
                tracingMetrics);

        // Lenient stubs to execute the supplier directly
        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(3);
            return supplier.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());
    }

    private Category createMockCategory(Long id) {
        Category cat = new Category();
        cat.setCategoryId(id);
        cat.setName("Test Category");
        cat.setDescription("Test desc");
        cat.setSlugCategory("test-category");
        cat.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        cat.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return cat;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private FindAllCategory findAllReq(int page, int size, String search) {
        FindAllCategory req = new FindAllCategory();
        req.setPage(page);
        req.setPageSize(size);
        req.setSearch(search == null ? "" : search);
        return req;
    }

    @Nested
    @DisplayName("findAll tests")
    class FindAllTests {
        @Test
        void cacheMiss_fetchesFromDb() {
            FindAllCategory req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(categoryQueryRepository.findCategories(any(FindAllCategory.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockCategory(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<CategoryResponse>> result = service.findAll(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
            assertThat(result.data().get(0).getName()).isEqualTo("Test Category");
        }

        @Test
        void cacheHit_returnsCached() {
            FindAllCategory req = findAllReq(1, 10, "");
            ApiResponsePagination<List<CategoryResponse>> cached = new ApiResponsePagination<>(
                    "success", "Categories retrieved successfully",
                    List.of(CategoryResponse.from(createMockCategory(1L))), null);
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().item(toJson(cached)));

            ApiResponsePagination<List<CategoryResponse>> result = service.findAll(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findByActive tests")
    class FindByActiveTests {
        @Test
        void cacheMiss_fetchesFromDb() {
            FindAllCategory req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(categoryQueryRepository.findActiveCategories(any(FindAllCategory.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockCategory(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<CategoryResponseDeleteAt>> result = service.findByActive(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findByTrashed tests")
    class FindByTrashedTests {
        @Test
        void cacheMiss_fetchesFromDb() {
            FindAllCategory req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(categoryQueryRepository.findTrashedCategories(any(FindAllCategory.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockCategory(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<CategoryResponseDeleteAt>> result = service.findByTrashed(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findById tests")
    class FindByIdTests {
        @Test
        void cacheMiss_fetchesFromDb() {
            Integer id = 1;
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(categoryQueryRepository.findCategoryById(anyLong()))
                    .thenReturn(Uni.createFrom().item(createMockCategory(id.longValue())));
            when(redisService.setReactive(anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

            ApiResponse<CategoryResponse> result = service.findById(id).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data().getId()).isEqualTo(id.longValue());
        }

        @Test
        void notFound_returnsError() {
            Integer id = 999;
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(categoryQueryRepository.findCategoryById(anyLong())).thenReturn(Uni.createFrom().nullItem());

            ApiResponse<CategoryResponse> result = service.findById(id).await().indefinitely();
            assertThat(result.status()).isEqualTo("error");
            assertThat(result.message()).contains("Category not found");
        }
    }
}