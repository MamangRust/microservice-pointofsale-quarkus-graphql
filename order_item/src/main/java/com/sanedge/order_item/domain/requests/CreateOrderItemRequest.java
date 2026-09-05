package com.sanedge.order_item.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to create new order item")
public class CreateOrderItemRequest {
    @NotNull
    @Schema(description = "ID order", example = "1")
    private Integer orderId;

    @NotNull
    @Schema(description = "ID produk", example = "101")
    private Integer productId;

    @NotNull
    @Min(1)
    @Schema(description = "Jumlah produk", example = "2")
    private Integer quantity;

    @NotNull
    @Schema(description = "Harga per item", example = "50000")
    private Integer price;
}
