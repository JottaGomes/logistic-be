package com.adrianojlt.logistics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateShipmentRequestDTO {

    @NotBlank(message = "must not be blank")
    @Size(max = 30, message = "must be at most 30 characters")
    private String reference;

    @NotBlank(message = "must not be blank")
    @Size(max = 100, message = "must be at most 100 characters")
    private String customer;
}
