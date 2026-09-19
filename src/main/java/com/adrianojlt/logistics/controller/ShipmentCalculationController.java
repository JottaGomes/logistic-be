package com.adrianojlt.logistics.controller;

import com.adrianojlt.logistics.dto.ApiResponse;
import com.adrianojlt.logistics.dto.ShipmentCalculationRequestDTO;
import com.adrianojlt.logistics.dto.ShipmentCalculationResponseDTO;
import com.adrianojlt.logistics.service.ShipmentCalculationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentCalculationController {

    private final ShipmentCalculationService service;

    public ShipmentCalculationController(ShipmentCalculationService service) {
        this.service = service;
    }

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<ShipmentCalculationResponseDTO>> calculate(
            @Valid @RequestBody ShipmentCalculationRequestDTO request) {
        ShipmentCalculationResponseDTO result = service.calculate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ShipmentCalculationResponseDTO>>> findAll(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ShipmentCalculationResponseDTO> results = service.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }
}
