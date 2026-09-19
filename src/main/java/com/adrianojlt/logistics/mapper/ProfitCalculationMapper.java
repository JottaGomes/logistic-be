package com.adrianojlt.logistics.mapper;

import com.adrianojlt.logistics.dto.AmountLineDTO;
import com.adrianojlt.logistics.dto.ProfitCalculationResponseDTO;
import com.adrianojlt.logistics.dto.ShipmentResponseDTO;
import com.adrianojlt.logistics.entity.Cost;
import com.adrianojlt.logistics.entity.Income;
import com.adrianojlt.logistics.entity.ProfitCalculation;
import com.adrianojlt.logistics.entity.Shipment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Keeps the entities out of the web layer. Declared rather than hand-written so
 * a new field on an entity is a compile error here, not a silently missing value.
 */
@Mapper(componentModel = "spring")
public interface ProfitCalculationMapper {

    @Mapping(target = "shipmentReference", source = "shipment.reference")
    @Mapping(target = "customer", source = "shipment.customer")
    @Mapping(target = "incomes", ignore = true)
    @Mapping(target = "costs", ignore = true)
    ProfitCalculationResponseDTO toResponseDTO(ProfitCalculation entity);

    @Mapping(target = "type", source = "incomeType")
    AmountLineDTO toLineDTO(Income income);

    @Mapping(target = "type", source = "costType")
    AmountLineDTO toLineDTO(Cost cost);

    List<AmountLineDTO> toIncomeLines(List<Income> incomes);

    List<AmountLineDTO> toCostLines(List<Cost> costs);

    ShipmentResponseDTO toShipmentDTO(Shipment shipment);

    List<ShipmentResponseDTO> toShipmentDTOs(List<Shipment> shipments);
}
