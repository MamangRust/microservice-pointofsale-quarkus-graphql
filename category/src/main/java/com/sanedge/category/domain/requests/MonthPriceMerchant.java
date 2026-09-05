package com.sanedge.category.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "MonthPriceMerchant", description = "Harga bulanan per merchant")
public class MonthPriceMerchant {
    @NotNull
    @Schema(description = "ID merchant", example = "2001")
    private Integer merchantId;

    @NotNull
    @Schema(description = "Tahun", example = "2025")
    private Integer year;
}
