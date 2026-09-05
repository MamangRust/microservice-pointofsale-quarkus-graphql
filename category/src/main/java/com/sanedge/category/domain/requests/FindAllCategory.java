package com.sanedge.category.domain.requests;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "FindAllCategory", description = "Parameter pencarian kategori (pagination)")
public class FindAllCategory {
    @NotBlank(message = "search is required")
    @Parameter(description = "Kata kunci pencarian kategori", example = "Elektronik")
    private String search;

    @Min(value = 1, message = "page minimal 1")
    @Parameter(description = "Halaman data", example = "1")
    private Integer page;

    @Min(value = 1, message = "page_size minimal 1")
    @Max(value = 100, message = "page_size maksimal 100")
    @Parameter(description = "Jumlah data per halaman", example = "10")
    private Integer pageSize;
}
