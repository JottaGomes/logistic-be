package com.adrianojlt.logistics.dto;

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

    /** Only populated on a fresh calculation, not when listing history. */
    private List<AmountLineDTO> incomes;
    private List<AmountLineDTO> costs;
}
