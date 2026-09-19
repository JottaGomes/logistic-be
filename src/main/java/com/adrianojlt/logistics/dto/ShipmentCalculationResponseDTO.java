package com.adrianojlt.logistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentCalculationResponseDTO {

    private Long id;

    private BigDecimal cost;
    private BigDecimal income;
    private BigDecimal additionalCost;

    private BigDecimal totalCosts;
    private BigDecimal profitOrLoss;

    private LocalDateTime createdAt;
}
