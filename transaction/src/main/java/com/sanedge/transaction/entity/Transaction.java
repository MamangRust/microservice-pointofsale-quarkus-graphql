package com.sanedge.transaction.entity;

import com.sanedge.common.enums.PaymentStatus;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EqualsAndHashCode(callSuper = true)
@Table(name = "transactions", schema = "pos_transaction")
@AttributeOverride(name = "id", column = @Column(name = "transaction_id"))
public class Transaction extends BaseModel {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "change_amount")
    private Integer changeAmount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    public Long getTransactionId() {
        return this.id;
    }

    public void setTransactionId(Long transactionId) {
        this.id = transactionId;
    }
}
