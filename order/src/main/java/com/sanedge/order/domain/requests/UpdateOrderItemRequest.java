package com.sanedge.order.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Item order untuk update")
public class UpdateOrderItemRequest {

    @NotNull
    @Schema(description = "ID item order", example = "1")
    private Integer orderItemId;

    @NotNull
    @Schema(description = "ID produk", example = "101")
    private Integer productId;

    @NotNull
    @Min(1)
    @Schema(description = "Jumlah produk", example = "2")
    private Integer quantity;

    @Schema(description = "Harga per item (optional — falls back to product price)", example = "50000")
    private Integer price;
}
