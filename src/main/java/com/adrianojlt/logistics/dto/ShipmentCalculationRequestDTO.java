package com.adrianojlt.logistics.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentCalculationRequestDTO {

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal cost;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal income;

    @DecimalMin("0.0")
    private BigDecimal additionalCost;
}
