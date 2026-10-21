package com.sanedge.transaction.entity;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "outbox")
public class Outbox {

    /** Domain discriminator of the outbox rows owned by the transaction module. */
    public static final String DOMAIN_TRANSACTION = "transaction";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_key", length = 200)
    private String eventKey;

    @Column(nullable = false, length = 200)
    private String topic;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(name = "next_attempt_at")
    private Timestamp nextAttemptAt;

    @Column(name = "claimed_at")
    private Timestamp claimedAt;

    @Column(name = "claim_token", length = 100)
    private String claimToken;

    @Column(name = "domain", length = 50)
    private String domain;

    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "processed_at")
    private Timestamp processedAt;

    @Column(name = "last_error")
    private String lastError;

    @PrePersist
    protected void onCreate() {
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }
}
