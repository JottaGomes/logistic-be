package com.jgomes.logistics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDTO {

    @NotBlank(message = "must not be blank")
    @Size(min = 3, max = 50, message = "must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "must not be blank")
    @Size(min = 8, max = 100, message = "must be between 8 and 100 characters")
    private String password;
}
