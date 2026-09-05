package com.sanedge.category.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "YearPriceMerchant", description = "Harga tahunan per merchant")
public class YearPriceMerchant {
    @NotNull
    @Schema(description = "ID merchant", example = "2001")
    private Integer merchantId;

    @NotNull
    @Schema(description = "Tahun", example = "2025")
    private Integer year;
}
