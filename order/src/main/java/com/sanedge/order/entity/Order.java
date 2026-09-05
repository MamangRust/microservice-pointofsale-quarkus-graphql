package com.sanedge.order.entity;

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
@Table(name = "orders", schema = "pos_order")
@AttributeOverride(name = "id", column = @Column(name = "order_id"))
public class Order extends BaseModel {

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "cashier_id", nullable = false)
    private Long cashierId;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    public Long getOrderId() {
        return this.id;
    }

    public void setOrderId(Long orderId) {
        this.id = orderId;
    }
}
