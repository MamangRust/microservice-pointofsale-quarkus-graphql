package com.sanedge.category.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "MonthTotalPriceCategory", description = "Total harga bulanan per kategori")
public class MonthTotalPriceCategory {
    @NotNull
    @Schema(description = "ID kategori", example = "101")
    private Integer categoryId;

    @NotNull
    @Schema(description = "Tahun", example = "2025")
    private Integer year;

    @NotNull
    @Schema(description = "Bulan", example = "8")
    private Integer month;
}
