package com.jgomes.logistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A shipment the user can pick from, for the calculation form. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentResponseDTO {

    private Long id;
    private String reference;
    private String customer;
}
