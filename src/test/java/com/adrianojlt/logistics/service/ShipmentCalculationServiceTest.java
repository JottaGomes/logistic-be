package com.adrianojlt.logistics.service;

import com.adrianojlt.logistics.dto.ShipmentCalculationRequestDTO;
import com.adrianojlt.logistics.dto.ShipmentCalculationResponseDTO;
import com.adrianojlt.logistics.entity.ShipmentCalculation;
import com.adrianojlt.logistics.mapper.ShipmentCalculationMapper;
import com.adrianojlt.logistics.repository.ShipmentCalculationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentCalculationServiceTest {

    @Mock
    private ShipmentCalculationMapper mapper;

    @InjectMocks
    private ShipmentCalculationService service;

    @Mock
    private ShipmentCalculationRepository repository;

    @BeforeEach
    void setDefaultSecurityContext() {
        var auth = new UsernamePasswordAuthenticationToken("test-user", null, List.of());
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void calculate_withAdditionalCost_computesCorrectTotals() {
        ShipmentCalculationRequestDTO request = ShipmentCalculationRequestDTO.builder()
                .income(new BigDecimal("1000.00"))
                .cost(new BigDecimal("200.00"))
                .additionalCost(new BigDecimal("50.00"))
                .build();

        ShipmentCalculation entity = new ShipmentCalculation();
        ShipmentCalculation saved = buildEntity(1L, "1000.00", "200.00", "50.00", "250.00", "750.00");
        ShipmentCalculationResponseDTO expectedResponse = buildResponseDTO(1L, "1000.00", "200.00", "50.00", "250.00", "750.00");

        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(saved);
        when(mapper.toResponseDTO(saved)).thenReturn(expectedResponse);

        ShipmentCalculationResponseDTO result = service.calculate(request);

        assertThat(result).isEqualTo(expectedResponse);
        assertThat(entity.getTotalCosts()).isEqualByComparingTo("250.00");
        assertThat(entity.getProfitOrLoss()).isEqualByComparingTo("750.00");
        verify(repository).save(entity);
    }

    @Test
    void calculate_withNullAdditionalCost_defaultsAdditionalCostToZero() {
        ShipmentCalculationRequestDTO request = ShipmentCalculationRequestDTO.builder()
                .income(new BigDecimal("500.00"))
                .cost(new BigDecimal("300.00"))
                .additionalCost(null)
                .build();

        ShipmentCalculation entity = new ShipmentCalculation();

        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponseDTO(entity)).thenReturn(new ShipmentCalculationResponseDTO());

        service.calculate(request);

        assertThat(entity.getAdditionalCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(entity.getTotalCosts()).isEqualByComparingTo("300.00");
        assertThat(entity.getProfitOrLoss()).isEqualByComparingTo("200.00");
    }

    @Test
    void calculate_withLossScenario_computesNegativeProfitOrLoss() {
        ShipmentCalculationRequestDTO request = ShipmentCalculationRequestDTO.builder()
                .income(new BigDecimal("400.00"))
                .cost(new BigDecimal("500.00"))
                .additionalCost(new BigDecimal("100.00"))
                .build();

        ShipmentCalculation entity = new ShipmentCalculation();

        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponseDTO(entity)).thenReturn(new ShipmentCalculationResponseDTO());

        service.calculate(request);

        assertThat(entity.getTotalCosts()).isEqualByComparingTo("600.00");
        assertThat(entity.getProfitOrLoss()).isEqualByComparingTo("-200.00");
    }

    @Test
    void calculate_withAuthenticatedUser_setsCreatedByFromSecurityContext() {
        var authentication = new UsernamePasswordAuthenticationToken("adriano", null, List.of());
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));

        ShipmentCalculationRequestDTO request = ShipmentCalculationRequestDTO.builder()
                .income(new BigDecimal("1000.00"))
                .cost(new BigDecimal("200.00"))
                .build();

        ShipmentCalculation entity = new ShipmentCalculation();

        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponseDTO(entity)).thenReturn(new ShipmentCalculationResponseDTO());

        service.calculate(request);

        assertThat(entity.getCreatedBy()).isEqualTo("adriano");
    }

    @Test
    void calculate_withNoAuthentication_throwsIllegalStateException() {
        SecurityContextHolder.clearContext();

        ShipmentCalculationRequestDTO request = ShipmentCalculationRequestDTO.builder()
                .income(new BigDecimal("1000.00"))
                .cost(new BigDecimal("200.00"))
                .build();

        assertThatThrownBy(() -> service.calculate(request))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void findAll_delegatesToRepositoryAndMapsResults() {
        Pageable pageable = PageRequest.of(0, 10);
        ShipmentCalculation entity = new ShipmentCalculation();
        ShipmentCalculationResponseDTO dto = new ShipmentCalculationResponseDTO();
        Page<ShipmentCalculation> entityPage = new PageImpl<>(List.of(entity), pageable, 1);

        when(repository.findAll(pageable)).thenReturn(entityPage);
        when(mapper.toResponseDTO(entity)).thenReturn(dto);

        Page<ShipmentCalculationResponseDTO> result = service.findAll(pageable);

        assertThat(result.getContent()).containsExactly(dto);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(repository).findAll(pageable);
    }

    private ShipmentCalculation buildEntity(Long id, String income, String cost,
            String additionalCost, String totalCosts, String profitOrLoss) {
        return ShipmentCalculation.builder()
                .id(id)
                .income(new BigDecimal(income))
                .cost(new BigDecimal(cost))
                .additionalCost(new BigDecimal(additionalCost))
                .totalCosts(new BigDecimal(totalCosts))
                .profitOrLoss(new BigDecimal(profitOrLoss))
                .createdBy("local")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private ShipmentCalculationResponseDTO buildResponseDTO(Long id, String income, String cost,
            String additionalCost, String totalCosts, String profitOrLoss) {
        return ShipmentCalculationResponseDTO.builder()
                .id(id)
                .income(new BigDecimal(income))
                .cost(new BigDecimal(cost))
                .additionalCost(new BigDecimal(additionalCost))
                .totalCosts(new BigDecimal(totalCosts))
                .profitOrLoss(new BigDecimal(profitOrLoss))
                .build();
    }
}
