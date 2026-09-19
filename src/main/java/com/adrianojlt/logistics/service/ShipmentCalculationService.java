package com.adrianojlt.logistics.service;

import com.adrianojlt.logistics.dto.ShipmentCalculationRequestDTO;
import com.adrianojlt.logistics.dto.ShipmentCalculationResponseDTO;
import com.adrianojlt.logistics.entity.ShipmentCalculation;
import com.adrianojlt.logistics.mapper.ShipmentCalculationMapper;
import com.adrianojlt.logistics.repository.ShipmentCalculationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
public class ShipmentCalculationService {

    private final ShipmentCalculationMapper mapper;
    private final ShipmentCalculationRepository repository;

    public ShipmentCalculationService(
            ShipmentCalculationMapper mapper,
            ShipmentCalculationRepository repository) {
        this.mapper = mapper;
        this.repository = repository;
    }

    @Transactional
    public ShipmentCalculationResponseDTO calculate(ShipmentCalculationRequestDTO request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user in security context");
        }

        String createdBy = authentication.getName();

        BigDecimal additionalCost = request.getAdditionalCost() != null
                ? request.getAdditionalCost()
                : BigDecimal.ZERO;

        BigDecimal totalCosts = request.getCost().add(additionalCost);
        BigDecimal profitOrLoss = request.getIncome().subtract(totalCosts);

        ShipmentCalculation entity = mapper.toEntity(request);

        entity.setAdditionalCost(additionalCost);
        entity.setTotalCosts(totalCosts);
        entity.setProfitOrLoss(profitOrLoss);
        entity.setCreatedBy(createdBy);

        ShipmentCalculation saved = repository.save(entity);

        log.info("calculation_saved user={} income={} totalCosts={} profitOrLoss={}",
                createdBy, request.getIncome(), totalCosts, profitOrLoss);

        return mapper.toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public Page<ShipmentCalculationResponseDTO> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponseDTO);
    }
}
