package com.sanedge.order.domain.requests;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request untuk memperbarui order")
public class UpdateOrderRequest {
    @NotNull
    @Schema(description = "ID order yang akan diperbarui", example = "1")
    private Integer orderId;

    @NotNull
    @Schema(description = "ID cashier yang membuat order", example = "10")
    private Integer cashierId;

    @Valid
    @NotNull
    @Schema(description = "Daftar item order yang diperbarui")
    private List<UpdateOrderItemRequest> items;
}
