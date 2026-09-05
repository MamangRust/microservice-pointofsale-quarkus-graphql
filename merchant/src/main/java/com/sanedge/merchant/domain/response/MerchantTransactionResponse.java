package com.sanedge.merchant.domain.response;

import com.sanedge.merchant.entity.MerchantTransactions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantTransactionResponse {
    private Long id;
    private String cardNumber;
    private Long amount;
    private String paymentMethod;
    private Long merchantId;
    private String merchantName;
    private String transactionTime;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;

    public static MerchantTransactionResponse from(MerchantTransactions tx) {
        return MerchantTransactionResponse.builder()
                .id(tx.getTransactionId().longValue())
                .cardNumber(tx.getCardNumber())
                .amount(tx.getAmount().longValue())
                .paymentMethod(tx.getPaymentMethod())
                .merchantId(tx.getMerchantId().longValue())
                .merchantName(tx.getMerchantName())
                .transactionTime(tx.getTransactionTime() != null ? tx.getTransactionTime().toString() : null)
                .createdAt(tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : null)
                .updatedAt(tx.getUpdatedAt() != null ? tx.getUpdatedAt().toString() : null)
                .deletedAt(tx.getDeletedAt() != null ? tx.getDeletedAt().toString() : null)
                .build();
    }
}