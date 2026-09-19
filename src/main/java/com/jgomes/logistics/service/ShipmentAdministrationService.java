package com.jgomes.logistics.service;

import com.jgomes.logistics.dto.CreateShipmentRequestDTO;
import com.jgomes.logistics.dto.RecordAmountRequestDTO;
import com.jgomes.logistics.dto.ShipmentDetailDTO;
import com.jgomes.logistics.dto.ShipmentResponseDTO;
import com.jgomes.logistics.entity.Cost;
import com.jgomes.logistics.entity.CostType;
import com.jgomes.logistics.entity.Income;
import com.jgomes.logistics.entity.IncomeType;
import com.jgomes.logistics.entity.Shipment;
import com.jgomes.logistics.exception.DuplicateShipmentException;
import com.jgomes.logistics.exception.ShipmentNotFoundException;
import com.jgomes.logistics.mapper.ProfitCalculationMapper;
import com.jgomes.logistics.repository.CostRepository;
import com.jgomes.logistics.repository.IncomeRepository;
import com.jgomes.logistics.repository.ShipmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Recording the data the Calculate Profit use case depends on.
 *
 * Its pre-condition is that income and cost data is already in the system; this
 * is what puts it there, and it is what the requirements call customer payment
 * administration and operational cost administration (section 1.4).
 *
 * Deliberately separate from {@link ProfitCalculationService}: recording an amount
 * and evaluating a shipment are different responsibilities, and only the second is
 * the use case under assessment.
 */
@Slf4j
@Service
public class ShipmentAdministrationService {

    private final ShipmentRepository shipmentRepository;
    private final IncomeRepository incomeRepository;
    private final CostRepository costRepository;
    private final ProfitCalculationMapper mapper;

    public ShipmentAdministrationService(
            ShipmentRepository shipmentRepository,
            IncomeRepository incomeRepository,
            CostRepository costRepository,
            ProfitCalculationMapper mapper) {
        this.shipmentRepository = shipmentRepository;
        this.incomeRepository = incomeRepository;
        this.costRepository = costRepository;
        this.mapper = mapper;
    }

    @Transactional
    public ShipmentResponseDTO createShipment(CreateShipmentRequestDTO request) {

        String reference = request.getReference().trim();

        if (shipmentRepository.findByReference(reference).isPresent()) {
            throw new DuplicateShipmentException(reference);
        }

        Shipment saved = shipmentRepository.save(Shipment.builder()
                .reference(reference)
                .customer(request.getCustomer().trim())
                .build());

        log.info("shipment_created reference={} customer={}", reference, saved.getCustomer());

        return mapper.toShipmentDTO(saved);
    }

    @Transactional
    public ShipmentDetailDTO recordIncome(String reference, RecordAmountRequestDTO request) {

        Shipment shipment = require(reference);

        incomeRepository.save(Income.builder()
                .shipment(shipment)
                .incomeType(parse(IncomeType.class, request.getType()))
                .amount(request.getAmount())
                .description(request.getDescription())
                .build());

        log.info("income_recorded shipment={} type={} amount={}",
                shipment.getReference(), request.getType(), request.getAmount());

        return detail(shipment);
    }

    @Transactional
    public ShipmentDetailDTO recordCost(String reference, RecordAmountRequestDTO request) {

        Shipment shipment = require(reference);

        costRepository.save(Cost.builder()
                .shipment(shipment)
                .costType(parse(CostType.class, request.getType()))
                .amount(request.getAmount())
                .description(request.getDescription())
                .build());

        log.info("cost_recorded shipment={} type={} amount={}",
                shipment.getReference(), request.getType(), request.getAmount());

        return detail(shipment);
    }

    @Transactional(readOnly = true)
    public ShipmentDetailDTO findDetail(String reference) {
        return detail(require(reference));
    }

    private Shipment require(String reference) {
        String trimmed = reference.trim();
        return shipmentRepository.findByReference(trimmed)
                .orElseThrow(() -> new ShipmentNotFoundException(trimmed));
    }

    private ShipmentDetailDTO detail(Shipment shipment) {

        List<Income> incomes = incomeRepository.findByShipmentId(shipment.getId());
        List<Cost> costs = costRepository.findByShipmentId(shipment.getId());

        return new ShipmentDetailDTO(
                shipment.getId(),
                shipment.getReference(),
                shipment.getCustomer(),
                mapper.toIncomeLines(incomes),
                mapper.toCostLines(costs),
                incomeRepository.sumAmountByShipmentId(shipment.getId()),
                costRepository.sumAmountByShipmentId(shipment.getId()));
    }

    /** Turns an unknown type into a 400 naming the accepted values, not a 500. */
    private <E extends Enum<E>> E parse(Class<E> type, String value) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "type: must be one of " + String.join(", ",
                            java.util.Arrays.stream(type.getEnumConstants()).map(Enum::name).toList()));
        }
    }
}
