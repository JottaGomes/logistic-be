package com.jgomes.logistics.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A correction to an income or cost already recorded. The type is not part of it:
 * a line recorded with the wrong type is a different line, not a corrected amount.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CorrectAmountRequestDTO {

    @NotNull(message = "must not be null")
    @DecimalMin(value = "0.00", message = "must not be negative")
    private BigDecimal amount;

    @Size(max = 255, message = "must be at most 255 characters")
    private String description;

    /** The version of the line as the caller last read it, from AmountLineDTO. */
    @NotNull(message = "must not be null")
    private Long version;
}
