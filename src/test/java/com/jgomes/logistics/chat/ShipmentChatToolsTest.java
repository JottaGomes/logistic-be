package com.jgomes.logistics.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jgomes.logistics.dto.ProfitSummaryDTO;
import com.jgomes.logistics.exception.ShipmentNotFoundException;
import com.jgomes.logistics.service.ProfitCalculationService;
import com.jgomes.logistics.service.ShipmentAdministrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ShipmentChatToolsTest {

    private final ShipmentAdministrationService shipmentService = mock(ShipmentAdministrationService.class);
    private final ProfitCalculationService calculationService = mock(ProfitCalculationService.class);
    private final ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

    private ShipmentChatTools tools;

    @BeforeEach
    void setUp() throws Exception {
        tools = new ShipmentChatTools(shipmentService, calculationService, mapper);
    }

    @Test
    void definitions_describeTheFourReadOnlyTools() {
        assertThat(tools.definitions()).extracting(tool -> tool.path("function").path("name").asText())
                .containsExactly("search_shipments", "get_shipment", "list_profit_calculations", "get_profit_summary");
    }

    /** The model may ask for any number of rows; the database is never asked for more than the cap. */
    @Test
    void searchShipments_capsTheLimit() {
        when(shipmentService.findShipments("sonae", ShipmentChatTools.MAX_ROWS)).thenReturn(List.of());

        tools.execute("search_shipments", "{\"search\": \"sonae\", \"limit\": 5000}");

        verify(shipmentService).findShipments("sonae", ShipmentChatTools.MAX_ROWS);
    }

    @Test
    void listCalculations_ignoresAnUnknownSortColumn() {
        when(calculationService.findAll(isNull(), any())).thenReturn(new PageImpl<>(List.of()));

        tools.execute("list_profit_calculations", "{\"sort\": \"password\", \"direction\": \"asc\"}");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(calculationService).findAll(isNull(), pageable.capture());
        Sort.Order order = pageable.getValue().getSort().getOrderFor("calculatedAt");
        assertThat(order).isNotNull();
        assertThat(order.isAscending()).isTrue();
    }

    @Test
    void getProfitSummary_returnsTheFiguresAsJson() {
        when(calculationService.summarize()).thenReturn(new ProfitSummaryDTO(4, 4, 2, new BigDecimal("700.00")));

        String result = tools.execute("get_profit_summary", "{}");

        assertThat(result).contains("\"shipmentsAtLoss\":2").contains("\"totalProfitOrLoss\":700.00");
    }

    /** Errors go back to the model as data it can act on, not as a failed answer. */
    @Test
    void getShipment_unknownReferenceComesBackAsAnError() {
        when(shipmentService.findDetail(eq("SHP-NOPE"))).thenThrow(new ShipmentNotFoundException("SHP-NOPE"));

        assertThat(tools.execute("get_shipment", "{\"reference\": \"SHP-NOPE\"}"))
                .contains("\"error\"").contains("SHP-NOPE");
    }

    @Test
    void execute_rejectsUnknownToolsAndMalformedArguments() {
        assertThat(tools.execute("drop_table", "{}")).contains("Unknown tool drop_table");
        assertThat(tools.execute("get_shipment", "{not json")).contains("not valid JSON");
        assertThat(tools.execute("get_shipment", "{}")).contains("reference is required");
        verifyNoInteractions(shipmentService);
    }
}
