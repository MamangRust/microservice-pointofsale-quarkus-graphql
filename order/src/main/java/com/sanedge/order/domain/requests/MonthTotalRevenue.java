package com.sanedge.order.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "MonthTotalRevenue", description = "Total harga bulanan")
public class MonthTotalRevenue {
    @NotNull
    @Schema(description = "Tahun", example = "2025")
    private Integer year;

    @NotNull
    @Schema(description = "Bulan", example = "9")
    private Integer month;
}
