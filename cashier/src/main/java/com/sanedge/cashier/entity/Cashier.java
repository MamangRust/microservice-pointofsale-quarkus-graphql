package com.sanedge.cashier.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "cashiers", schema = "pos_merchant")
@AttributeOverride(name = "id", column = @Column(name = "cashier_id"))
public class Cashier extends BaseModel {

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    public Long getCashierId() {
        return this.id;
    }

    public void setCashierId(Long cashierId) {
        this.id = cashierId;
    }
}
