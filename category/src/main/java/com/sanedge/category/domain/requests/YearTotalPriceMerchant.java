package com.sanedge.category.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "YearTotalPriceMerchant", description = "Total harga tahunan per merchant")
public class YearTotalPriceMerchant {
    @NotNull
    @Schema(description = "ID merchant", example = "2001")
    private Integer merchantId;

    @NotNull
    @Schema(description = "Tahun", example = "2025")
    private Integer year;
}
