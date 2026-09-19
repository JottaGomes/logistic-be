package com.adrianojlt.logistics.dto;

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
}
