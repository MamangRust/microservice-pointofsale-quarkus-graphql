package com.sanedge.gateway.dto;

import java.util.List;
import java.util.stream.Collectors;

public class CategoryDto {

    @org.eclipse.microprofile.graphql.Name("CreateCategoryRequest")
    public record CreateCategoryRequest(
            String name,
            String description,
            String imageCategory) {
    }

    @org.eclipse.microprofile.graphql.Name("UpdateCategoryRequest")
    public record UpdateCategoryRequest(
            String name,
            String description,
            String imageCategory) {
    }

    @org.eclipse.microprofile.graphql.Name("CategoryResponse")
    public record CategoryResponse(
            int id,
            String name,
            String description,
            String slugCategory,
            String imageCategory,
            String createdAt,
            String updatedAt) {
        public static CategoryResponse from(pb.category.Category.CategoryResponse proto) {
            return new CategoryResponse(
                    proto.getId(),
                    proto.getName(),
                    proto.getDescription(),
                    proto.getSlugCategory(),
                    proto.getImageCategory(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt());
        }
    }

    @org.eclipse.microprofile.graphql.Name("CategoryResponseDeleteAt")
    public record CategoryResponseDeleteAt(
            int id,
            String name,
            String description,
            String slugCategory,
            String imageCategory,
            String createdAt,
            String updatedAt,
            String deletedAt) {
        public static CategoryResponseDeleteAt from(pb.category.Category.CategoryResponseDeleteAt proto) {
            return new CategoryResponseDeleteAt(
                    proto.getId(),
                    proto.getName(),
                    proto.getDescription(),
                    proto.getSlugCategory(),
                    proto.getImageCategory(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt(),
                    proto.hasDeletedAt() ? proto.getDeletedAt().getValue() : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("CategoryMonthPriceResponse")
    public record CategoryMonthPriceResponse(
            String month,
            int categoryId,
            String categoryName,
            int orderCount,
            int itemsSold,
            int totalRevenue) {
        public static CategoryMonthPriceResponse from(pb.category.Category.CategoryMonthPriceResponse proto) {
            return new CategoryMonthPriceResponse(
                    proto.getMonth(),
                    proto.getCategoryId(),
                    proto.getCategoryName(),
                    proto.getOrderCount(),
                    proto.getItemsSold(),
                    proto.getTotalRevenue());
        }
    }

    @org.eclipse.microprofile.graphql.Name("CategoryYearPriceResponse")
    public record CategoryYearPriceResponse(
            String year,
            int categoryId,
            String categoryName,
            int orderCount,
            int itemsSold,
            int totalRevenue,
            int uniqueProductsSold) {
        public static CategoryYearPriceResponse from(pb.category.Category.CategoryYearPriceResponse proto) {
            return new CategoryYearPriceResponse(
                    proto.getYear(),
                    proto.getCategoryId(),
                    proto.getCategoryName(),
                    proto.getOrderCount(),
                    proto.getItemsSold(),
                    proto.getTotalRevenue(),
                    proto.getUniqueProductsSold());
        }
    }

    @org.eclipse.microprofile.graphql.Name("CategoriesMonthlyTotalPriceResponse")
    public record CategoriesMonthlyTotalPriceResponse(
            String year,
            String month,
            int totalRevenue) {
        public static CategoriesMonthlyTotalPriceResponse from(
                pb.category.Category.CategoriesMonthlyTotalPriceResponse proto) {
            return new CategoriesMonthlyTotalPriceResponse(
                    proto.getYear(),
                    proto.getMonth(),
                    proto.getTotalRevenue());
        }
    }

    @org.eclipse.microprofile.graphql.Name("CategoriesYearlyTotalPriceResponse")
    public record CategoriesYearlyTotalPriceResponse(
            String year,
            int totalRevenue) {
        public static CategoriesYearlyTotalPriceResponse from(
                pb.category.Category.CategoriesYearlyTotalPriceResponse proto) {
            return new CategoriesYearlyTotalPriceResponse(
                    proto.getYear(),
                    proto.getTotalRevenue());
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseCategory")
    public record ApiResponseCategory(
            String status,
            String message,
            CategoryResponse data) {
        public static ApiResponseCategory from(pb.category.Category.ApiResponseCategory proto) {
            return new ApiResponseCategory(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.hasData() ? CategoryResponse.from(proto.getData()) : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseCategoryDeleteAt")
    public record ApiResponseCategoryDeleteAt(
            String status,
            String message,
            CategoryResponseDeleteAt data) {
        public static ApiResponseCategoryDeleteAt from(pb.category.Category.ApiResponseCategoryDeleteAt proto) {
            return new ApiResponseCategoryDeleteAt(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.hasData() ? CategoryResponseDeleteAt.from(proto.getData()) : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseCategoryMonthPrice")
    public record ApiResponseCategoryMonthPrice(
            String status,
            String message,
            List<CategoryMonthPriceResponse> data) {
        public static ApiResponseCategoryMonthPrice from(pb.category.Category.ApiResponseCategoryMonthPrice proto) {
            return new ApiResponseCategoryMonthPrice(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(CategoryMonthPriceResponse::from).collect(Collectors.toList()));
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseCategoryYearPrice")
    public record ApiResponseCategoryYearPrice(
            String status,
            String message,
            List<CategoryYearPriceResponse> data) {
        public static ApiResponseCategoryYearPrice from(pb.category.Category.ApiResponseCategoryYearPrice proto) {
            return new ApiResponseCategoryYearPrice(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(CategoryYearPriceResponse::from).collect(Collectors.toList()));
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseCategoryMonthlyTotalPrice")
    public record ApiResponseCategoryMonthlyTotalPrice(
            String status,
            String message,
            List<CategoriesMonthlyTotalPriceResponse> data) {
        public static ApiResponseCategoryMonthlyTotalPrice from(
                pb.category.Category.ApiResponseCategoryMonthlyTotalPrice proto) {
            return new ApiResponseCategoryMonthlyTotalPrice(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(CategoriesMonthlyTotalPriceResponse::from)
                            .collect(Collectors.toList()));
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseCategoryYearlyTotalPrice")
    public record ApiResponseCategoryYearlyTotalPrice(
            String status,
            String message,
            List<CategoriesYearlyTotalPriceResponse> data) {
        public static ApiResponseCategoryYearlyTotalPrice from(
                pb.category.Category.ApiResponseCategoryYearlyTotalPrice proto) {
            return new ApiResponseCategoryYearlyTotalPrice(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(CategoriesYearlyTotalPriceResponse::from)
                            .collect(Collectors.toList()));
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponsePaginationCategory")
    public record ApiResponsePaginationCategory(
            String status,
            String message,
            List<CategoryResponse> data,
            PaginationMetaDto paginationMeta) {
        public static ApiResponsePaginationCategory from(
                pb.category.CategoryQuery.ApiResponsePaginationCategory proto) {
            return new ApiResponsePaginationCategory(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(CategoryResponse::from).collect(Collectors.toList()),
                    proto.hasPagination() ? PaginationMetaDto.from(proto.getPagination()) : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponsePaginationCategoryDeleteAt")
    public record ApiResponsePaginationCategoryDeleteAt(
            String status,
            String message,
            List<CategoryResponseDeleteAt> data,
            PaginationMetaDto paginationMeta) {
        public static ApiResponsePaginationCategoryDeleteAt from(
                pb.category.CategoryQuery.ApiResponsePaginationCategoryDeleteAt proto) {
            return new ApiResponsePaginationCategoryDeleteAt(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(CategoryResponseDeleteAt::from).collect(Collectors.toList()),
                    proto.hasPagination() ? PaginationMetaDto.from(proto.getPagination()) : null);
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseCategoryAll")
    public record ApiResponseCategoryAll(
            String status,
            String message) {
        public static ApiResponseCategoryAll from(pb.category.CategoryCommand.ApiResponseCategoryAll proto) {
            return new ApiResponseCategoryAll(
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    @org.eclipse.microprofile.graphql.Name("ApiResponseCategoryDelete")
    public record ApiResponseCategoryDelete(
            String status,
            String message) {
        public static ApiResponseCategoryDelete from(pb.category.CategoryCommand.ApiResponseCategoryDelete proto) {
            return new ApiResponseCategoryDelete(
                    proto.getStatus(),
                    proto.getMessage());
        }
    }
}
