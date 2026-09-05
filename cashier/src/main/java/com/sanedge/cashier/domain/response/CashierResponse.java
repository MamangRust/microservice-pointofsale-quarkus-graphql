package com.sanedge.cashier.domain.response;

import com.sanedge.cashier.entity.Cashier;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "CashierResponse", description = "Response untuk kasir")
public class CashierResponse {
    private Integer id;
    private Integer merchantId;
    private String name;
    private String createdAt;
    private String updatedAt;

    public static CashierResponse from(Cashier cashier) {
        return CashierResponse.builder()
                .id(cashier.getCashierId().intValue())
                .merchantId(cashier.getMerchantId().intValue())
                .name(cashier.getName())
                .createdAt(cashier.getCreatedAt().toString())
                .updatedAt(cashier.getUpdatedAt().toString())
                .build();
    }
}
