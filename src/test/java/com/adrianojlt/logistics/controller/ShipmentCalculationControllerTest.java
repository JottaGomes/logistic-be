package com.adrianojlt.logistics.controller;

import com.adrianojlt.logistics.dto.ShipmentCalculationRequestDTO;
import com.adrianojlt.logistics.dto.ShipmentCalculationResponseDTO;
import com.adrianojlt.logistics.security.DatabaseUserDetailsService;
import com.adrianojlt.logistics.security.JwtUtil;
import com.adrianojlt.logistics.service.ShipmentCalculationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ShipmentCalculationController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
class ShipmentCalculationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private ShipmentCalculationService service;

    @MockBean
    private DatabaseUserDetailsService userDetailsService;

    @Test
    void calculate_withValidRequest_returns201WithCalculatedResult() throws Exception {
        ShipmentCalculationRequestDTO request = ShipmentCalculationRequestDTO.builder()
                .income(new BigDecimal("1000.00"))
                .cost(new BigDecimal("200.00"))
                .additionalCost(new BigDecimal("50.00"))
                .build();

        ShipmentCalculationResponseDTO response = ShipmentCalculationResponseDTO.builder()
                .id(1L)
                .income(new BigDecimal("1000.00"))
                .cost(new BigDecimal("200.00"))
                .additionalCost(new BigDecimal("50.00"))
                .totalCosts(new BigDecimal("250.00"))
                .profitOrLoss(new BigDecimal("750.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(service.calculate(any())).thenReturn(response);

        mockMvc.perform(post("/api/shipments/calculate")
                        .with(user("adriano").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.totalCosts").value(250.00))
                .andExpect(jsonPath("$.data.profitOrLoss").value(750.00));
    }

    @Test
    void calculate_withMissingRequiredFields_returns400() throws Exception {
        mockMvc.perform(post("/api/shipments/calculate")
                        .with(user("adriano").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void calculate_withNegativeCost_returns400() throws Exception {
        ShipmentCalculationRequestDTO request = ShipmentCalculationRequestDTO.builder()
                .income(new BigDecimal("1000.00"))
                .cost(new BigDecimal("-1.00"))
                .build();

        mockMvc.perform(post("/api/shipments/calculate")
                        .with(user("adriano").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void findAll_returns200WithPagedResults() throws Exception {
        ShipmentCalculationResponseDTO dto = ShipmentCalculationResponseDTO.builder()
                .id(1L)
                .income(new BigDecimal("1000.00"))
                .cost(new BigDecimal("200.00"))
                .additionalCost(BigDecimal.ZERO)
                .totalCosts(new BigDecimal("200.00"))
                .profitOrLoss(new BigDecimal("800.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(service.findAll(any())).thenReturn(
                new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/shipments")
                        .with(user("adriano").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void calculate_withoutAuthentication_returns403() throws Exception {
        ShipmentCalculationRequestDTO request = ShipmentCalculationRequestDTO.builder()
                .income(new BigDecimal("1000.00"))
                .cost(new BigDecimal("200.00"))
                .build();

        mockMvc.perform(post("/api/shipments/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
