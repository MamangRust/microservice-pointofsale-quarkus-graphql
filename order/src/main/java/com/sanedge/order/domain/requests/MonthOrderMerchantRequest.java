package com.sanedge.order.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request untuk mengambil statistik order bulanan dari merchant tertentu")
public class MonthOrderMerchantRequest {

    @NotNull(message = "ID merchant wajib diisi")
    @Schema(description = "ID merchant", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer merchantId;

    @NotNull(message = "Tahun wajib diisi")
    @Schema(description = "Tahun (misalnya: 2024)", example = "2024", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer year;
}
