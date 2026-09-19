package com.jgomes.logistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** A shipment with everything recorded against it, for the administration screen. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentDetailDTO {

    private Long id;
    private String reference;
    private String customer;

    private List<AmountLineDTO> incomes;
    private List<AmountLineDTO> costs;

    /** Shown as a running preview; the stored figure only comes from Calculate Profit. */
    private BigDecimal totalIncome;
    private BigDecimal totalCosts;
}
