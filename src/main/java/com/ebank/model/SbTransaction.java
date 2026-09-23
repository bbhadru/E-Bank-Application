package com.ebank.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sb_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SbTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "txn_id")
    private Long txnId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sb_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SavingsAccount savingsAccount;

    @Column(name = "txn_type", nullable = false, length = 20)
    private String txnType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "txn_date")
    @Builder.Default
    private LocalDateTime txnDate = LocalDateTime.now();

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "narration", length = 255)
    private String narration;

    @Column(name = "reference_number", length = 50)
    private String referenceNumber;

    @Column(name = "created_by")
    private Long createdBy;
}