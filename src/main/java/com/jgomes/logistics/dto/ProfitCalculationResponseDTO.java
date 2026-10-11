package com.jgomes.logistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Step 6: what the Finance Department gets back. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfitCalculationResponseDTO {

    private Long id;
    private String shipmentReference;
    private String customer;
    private BigDecimal totalIncome;
    private BigDecimal totalCosts;
    private BigDecimal profitOrLoss;
    private LocalDateTime calculatedAt;

    /**
     * Set by a calculation request only: true when a new calculation was stored,
     * false when the totals matched the shipment's latest calculation and that one
     * was returned instead. Null when listing history.
     */
    private Boolean created;

    /** Only populated on a fresh calculation, not when listing history. */
    private List<AmountLineDTO> incomes;
    private List<AmountLineDTO> costs;
}
