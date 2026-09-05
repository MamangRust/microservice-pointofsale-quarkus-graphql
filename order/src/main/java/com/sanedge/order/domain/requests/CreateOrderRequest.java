package com.sanedge.order.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Request untuk membuat order baru")
public class CreateOrderRequest {
    @NotNull
    @Schema(description = "ID merchant", example = "1")
    private Integer merchantId;

    @NotNull
    @Schema(description = "ID cashier yang membuat order", example = "10")
    private Integer cashierId;

    @Valid
    @NotNull
    @Schema(description = "Daftar item order")
    private List<CreateOrderItemRequest> items;
}
