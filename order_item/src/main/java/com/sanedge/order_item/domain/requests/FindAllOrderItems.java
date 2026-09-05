package com.sanedge.order_item.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "Request for paginating order items")
public class FindAllOrderItems {
    @Schema(description = "Search keyword", example = "")
    private String search = "";

    @Min(1)
    @Schema(description = "Page number", example = "1")
    private Integer page = 1;

    @Min(1)
    @Schema(description = "Page size", example = "10")
    private Integer pageSize = 10;
}
