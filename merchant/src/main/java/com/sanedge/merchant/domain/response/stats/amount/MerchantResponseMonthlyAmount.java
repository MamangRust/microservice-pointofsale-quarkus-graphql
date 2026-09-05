package com.sanedge.merchant.domain.response.stats.amount;

import com.sanedge.merchant.entity.MerchantMonthlyAmount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantResponseMonthlyAmount {
    private String monthName;
    private Long totalAmount;

    public static MerchantResponseMonthlyAmount from(MerchantMonthlyAmount dto) {
        return MerchantResponseMonthlyAmount.builder()
                .monthName(dto.getMonth())
                .totalAmount(dto.getTotalAmount().longValue())
                .build();
    }
}