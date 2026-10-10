package com.jgomes.logistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** One income or cost record, as shown in the breakdown of a calculation. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AmountLineDTO {

    private Long id;
    private String type;
    private BigDecimal amount;
    private String description;

    /** Sent back unchanged with a correction, so a stale edit can be detected. */
    private Long version;
}
