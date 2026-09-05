package com.sanedge.category.domain.response;

import com.sanedge.category.entity.Category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponseDeleteAt {
    private Long id;
    private String name;
    private String description;
    private String slugCategory;
    private String imageCategory;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;

    public static CategoryResponseDeleteAt from(Category category) {
        return CategoryResponseDeleteAt.builder()
                .id(category.getCategoryId())
                .name(category.getName())
                .description(category.getDescription())
                .slugCategory(category.getSlugCategory())
                .createdAt(category.getCreatedAt() != null ? category.getCreatedAt().toString() : null)
                .updatedAt(category.getUpdatedAt() != null ? category.getUpdatedAt().toString() : null)
                .deletedAt(category.getDeletedAt() != null ? category.getDeletedAt().toString() : null)
                .build();
    }
}
