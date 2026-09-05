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
@Schema(name = "CashierResponseDeleteAt", description = "Response kasir dengan soft delete info")
public class CashierResponseDeleteAt {
    private Integer id;
    private Integer merchantId;
    private String name;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;

    public static CashierResponseDeleteAt from(Cashier cashier) {
        return CashierResponseDeleteAt.builder()
                .id(cashier.getCashierId().intValue())
                .merchantId(cashier.getMerchantId().intValue())
                .name(cashier.getName())
                .createdAt(cashier.getCreatedAt().toString())
                .updatedAt(cashier.getUpdatedAt().toString())
                .deletedAt(cashier.getDeletedAt() != null ? cashier.getDeletedAt().toString() : null)
                .build();
    }
}
