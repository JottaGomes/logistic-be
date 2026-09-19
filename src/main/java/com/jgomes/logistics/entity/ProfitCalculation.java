package com.jgomes.logistics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * The stored outcome of the Calculate Profit use case.
 *
 * Kept apart from {@link Shipment} on purpose: it is a point-in-time result, and
 * recalculating a shipment after new incomes or costs arrive must not overwrite
 * what was reported before.
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "profit_calculation")
public class ProfitCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "total_income", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalIncome;

    @Column(name = "total_costs", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCosts;

    @Column(name = "profit_or_loss", nullable = false, precision = 12, scale = 2)
    private BigDecimal profitOrLoss;

    @Column(name = "calculated_by", nullable = false, length = 50)
    @Builder.Default
    private String calculatedBy = "system";

    @Column(name = "calculated_at", nullable = false)
    @Builder.Default
    private LocalDateTime calculatedAt = LocalDateTime.now();
}
