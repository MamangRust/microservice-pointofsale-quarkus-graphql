package com.sanedge.cashier.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "UpdateCashierRequest", description = "Request untuk update cashier")
public class UpdateCashierRequest {
    @NotNull
    @Schema(description = "ID cashier", example = "456")
    private Integer cashierId;

    @NotBlank
    @Schema(description = "Nama cashier", example = "Jane Doe")
    private String name;
}
