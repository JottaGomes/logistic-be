package com.adrianojlt.logistics.mapper;

import com.adrianojlt.logistics.dto.ShipmentCalculationRequestDTO;
import com.adrianojlt.logistics.dto.ShipmentCalculationResponseDTO;
import com.adrianojlt.logistics.entity.ShipmentCalculation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShipmentCalculationMapper {

    ShipmentCalculationResponseDTO toResponseDTO(ShipmentCalculation entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "totalCosts", ignore = true)
    @Mapping(target = "profitOrLoss", ignore = true)
    ShipmentCalculation toEntity(ShipmentCalculationRequestDTO dto);
}
