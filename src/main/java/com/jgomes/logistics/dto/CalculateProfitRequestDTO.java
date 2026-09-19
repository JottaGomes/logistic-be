package com.jgomes.logistics.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Step 1 of the use case: the Finance Department asks for a shipment to be
 * evaluated. Only the shipment is named — the amounts are already recorded.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalculateProfitRequestDTO {

    @NotBlank(message = "must not be blank")
    private String shipmentReference;
}
