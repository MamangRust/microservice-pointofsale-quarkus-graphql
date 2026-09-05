package com.sanedge.category.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "YearTotalPriceCategory", description = "Total harga tahunan per kategori")
public class YearTotalPriceCategory {
    @NotNull
    @Schema(description = "ID kategori", example = "101")
    private Integer categoryId;

    @NotNull
    @Schema(description = "Tahun", example = "2025")
    private Integer year;
}
