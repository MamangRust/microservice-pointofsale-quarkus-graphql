package com.sanedge.category.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "CreateCategoryRequest", description = "Request untuk membuat kategori")
public class CreateCategoryRequest {
    @NotBlank
    @Schema(description = "Nama kategori", example = "Elektronik")
    private String name;

    @NotBlank
    @Schema(description = "Deskripsi kategori", example = "Semua produk elektronik")
    private String description;

    @Schema(description = "Slug kategori", example = "elektronik")
    private String slugCategory;
}
