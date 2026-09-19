package com.jgomes.logistics.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * One amount to record against a shipment. Used for both income and cost: they
 * carry the same fields and differ only in which table they land in.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordAmountRequestDTO {

    /** An IncomeType or a CostType, depending on the endpoint. */
    @NotBlank(message = "must not be blank")
    private String type;

    @NotNull(message = "must not be null")
    @DecimalMin(value = "0.00", message = "must not be negative")
    private BigDecimal amount;

    @Size(max = 255, message = "must be at most 255 characters")
    private String description;
}
