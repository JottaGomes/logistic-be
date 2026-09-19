package com.adrianojlt.logistics.service;

import com.adrianojlt.logistics.dto.CalculateProfitRequestDTO;
import com.adrianojlt.logistics.dto.ProfitCalculationResponseDTO;
import com.adrianojlt.logistics.dto.ShipmentResponseDTO;
import com.adrianojlt.logistics.entity.Cost;
import com.adrianojlt.logistics.entity.Income;
import com.adrianojlt.logistics.entity.ProfitCalculation;
import com.adrianojlt.logistics.entity.Shipment;
import com.adrianojlt.logistics.exception.ShipmentNotFoundException;
import com.adrianojlt.logistics.mapper.ProfitCalculationMapper;
import com.adrianojlt.logistics.repository.CostRepository;
import com.adrianojlt.logistics.repository.IncomeRepository;
import com.adrianojlt.logistics.repository.ProfitCalculationRepository;
import com.adrianojlt.logistics.repository.ShipmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * The Calculate Profit use case.
 *
 * The amounts are never supplied by the caller: they are read from the income and
 * cost records already held against the shipment, which is what the use case
 * describes and what stops two callers reporting different profits for the same
 * shipment.
 */
@Slf4j
@Service
public class ProfitCalculationService {

    /** Hard ceiling, so a crafted request cannot ask for the whole table. */
    private static final int MAX_SHIPMENTS = 100;

    private final ShipmentRepository shipmentRepository;
    private final IncomeRepository incomeRepository;
    private final CostRepository costRepository;
    private final ProfitCalculationRepository calculationRepository;
    private final ProfitCalculationMapper mapper;

    public ProfitCalculationService(
            ShipmentRepository shipmentRepository,
            IncomeRepository incomeRepository,
            CostRepository costRepository,
            ProfitCalculationRepository calculationRepository,
            ProfitCalculationMapper mapper) {
        this.shipmentRepository = shipmentRepository;
        this.incomeRepository = incomeRepository;
        this.costRepository = costRepository;
        this.calculationRepository = calculationRepository;
        this.mapper = mapper;
    }

    @Transactional
    public ProfitCalculationResponseDTO calculate(CalculateProfitRequestDTO request) {

        String reference = request.getShipmentReference().trim();

        // step 1: the request names a shipment that must already exist
        Shipment shipment = shipmentRepository.findByReference(reference)
                .orElseThrow(() -> new ShipmentNotFoundException(reference));

        // steps 2 and 3: retrieve what was recorded, summed in the database
        BigDecimal totalIncome = incomeRepository.sumAmountByShipmentId(shipment.getId());
        BigDecimal totalCosts = costRepository.sumAmountByShipmentId(shipment.getId());

        // step 4: Profit or Loss = Total Income - Total Costs
        BigDecimal profitOrLoss = totalIncome.subtract(totalCosts);

        // step 5: store the outcome
        ProfitCalculation saved = calculationRepository.save(ProfitCalculation.builder()
                .shipment(shipment)
                .totalIncome(totalIncome)
                .totalCosts(totalCosts)
                .profitOrLoss(profitOrLoss)
                .calculatedBy(currentUsername())
                .build());

        log.info("calculation_saved shipment={} totalIncome={} totalCosts={} profitOrLoss={}",
                reference, totalIncome, totalCosts, profitOrLoss);

        // step 6: hand it back, with the lines it was derived from
        ProfitCalculationResponseDTO response = mapper.toResponseDTO(saved);

        List<Income> incomes = incomeRepository.findByShipmentId(shipment.getId());
        List<Cost> costs = costRepository.findByShipmentId(shipment.getId());

        response.setIncomes(mapper.toIncomeLines(incomes));
        response.setCosts(mapper.toCostLines(costs));

        return response;
    }

    @Transactional(readOnly = true)
    public Page<ProfitCalculationResponseDTO> findAll(String search, Pageable pageable) {

        if (search == null || search.isBlank()) {
            return calculationRepository.findAll(pageable).map(mapper::toResponseDTO);
        }

        String pattern = "%" + search.trim().toLowerCase() + "%";

        return calculationRepository.findByShipmentMatching(pattern, pageable).map(mapper::toResponseDTO);
    }

    /**
     * The shipments offered for selection: at most {@code limit}, optionally
     * narrowed by a search term. Never the whole table — see ShipmentRepository.
     */
    @Transactional(readOnly = true)
    public List<ShipmentResponseDTO> findShipments(String search, int limit) {

        Pageable slice = PageRequest.of(0, Math.min(Math.max(limit, 1), MAX_SHIPMENTS));

        List<Shipment> shipments = search == null || search.isBlank()
                ? shipmentRepository.findSlice(slice)
                : shipmentRepository.search("%" + search.trim().toLowerCase() + "%", slice);

        return mapper.toShipmentDTOs(shipments);
    }

    /** Falls back to "system" when authentication is switched off. */
    private String currentUsername() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null && authentication.isAuthenticated()
                ? authentication.getName()
                : "system";
    }
}
