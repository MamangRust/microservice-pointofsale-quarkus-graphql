package com.sanedge.merchant.domain.response.stats.method;

import com.sanedge.merchant.entity.MerchantMonthlyPaymentMethod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantResponseMonthlyPaymentMethod {
    private String monthName;
    private String paymentMethod;
    private Long totalAmount;

    public static MerchantResponseMonthlyPaymentMethod from(MerchantMonthlyPaymentMethod dto) {
        return MerchantResponseMonthlyPaymentMethod.builder()
                .monthName(dto.getMonth())
                .paymentMethod(dto.getPaymentMethod())
                .totalAmount(dto.getTotalAmount().longValue())
                .build();
    }
}