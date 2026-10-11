package com.jgomes.logistics.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jgomes.logistics.dto.ProfitCalculationResponseDTO;
import com.jgomes.logistics.exception.ShipmentNotFoundException;
import com.jgomes.logistics.service.ProfitCalculationService;
import com.jgomes.logistics.service.ShipmentAdministrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * The only way the chat assistant can see data: a fixed set of read-only tools,
 * each backed by an existing service method.
 *
 * The model never writes SQL. It picks a tool and fills in typed arguments, which
 * are validated and bounded here, so it cannot change data, cannot reach tables
 * the tools do not expose (users and password hashes in particular) and cannot
 * request an unbounded result.
 */
@Slf4j
@Component
public class ShipmentChatTools {

    /** Rows one tool call may return, whatever the model asks for. */
    static final int MAX_ROWS = 20;

    private static final Set<String> SORTABLE = Set.of("calculatedAt", "profitOrLoss", "totalIncome", "totalCosts");

    /** OpenAI-compatible tool definitions, sent to the model with every request. */
    private static final String DEFINITIONS = """
            [
              {"type": "function", "function": {
                "name": "search_shipments",
                "description": "Find shipments by part of their reference or customer name. Returns id, reference and customer only.",
                "parameters": {"type": "object", "properties": {
                  "search": {"type": "string", "description": "Part of a shipment reference or customer name. Omit to list the first shipments."},
                  "limit": {"type": "integer", "description": "Maximum rows, 1 to 20. Default 10."}
                }}
              }},
              {"type": "function", "function": {
                "name": "get_shipment",
                "description": "Everything recorded against one shipment: each income and cost line with type, amount and description, plus the current total income and total costs. Use it for questions about one shipment's money.",
                "parameters": {"type": "object", "properties": {
                  "reference": {"type": "string", "description": "Exact shipment reference, for example SHP-2026-0001."}
                }, "required": ["reference"]}
              }},
              {"type": "function", "function": {
                "name": "list_profit_calculations",
                "description": "Stored profit or loss calculations, newest first by default. A shipment can have several, one per time it was calculated. Sort by profitOrLoss ascending to find the biggest losses, descending for the biggest profits.",
                "parameters": {"type": "object", "properties": {
                  "search": {"type": "string", "description": "Part of a shipment reference or customer name."},
                  "sort": {"type": "string", "enum": ["calculatedAt", "profitOrLoss", "totalIncome", "totalCosts"]},
                  "direction": {"type": "string", "enum": ["asc", "desc"]},
                  "limit": {"type": "integer", "description": "Maximum rows, 1 to 20. Default 10."}
                }}
              }},
              {"type": "function", "function": {
                "name": "get_profit_summary",
                "description": "Totals across all shipments: how many exist, how many have been calculated, how many are at a loss, and the summed profit or loss, using the latest calculation of each shipment.",
                "parameters": {"type": "object", "properties": {}}
              }}
            ]
            """;

    private final ShipmentAdministrationService shipmentService;
    private final ProfitCalculationService calculationService;
    private final ObjectMapper mapper;
    private final ArrayNode definitions;

    public ShipmentChatTools(
            ShipmentAdministrationService shipmentService,
            ProfitCalculationService calculationService,
            ObjectMapper mapper) throws JsonProcessingException {
        this.shipmentService = shipmentService;
        this.calculationService = calculationService;
        this.mapper = mapper;
        this.definitions = (ArrayNode) mapper.readTree(DEFINITIONS);
    }

    public ArrayNode definitions() {
        return definitions.deepCopy();
    }

    /**
     * Runs one tool call and returns its result as JSON text for the model.
     *
     * Never throws: a bad argument, an unknown tool or a missing shipment comes
     * back as {@code {"error": "..."}}, which the model can read and recover from
     * (for example by searching for the right reference) instead of the whole
     * answer failing.
     */
    public String execute(String name, String argumentsJson) {
        try {
            JsonNode args = argumentsJson == null || argumentsJson.isBlank()
                    ? mapper.createObjectNode()
                    : mapper.readTree(argumentsJson);

            Object result = switch (name) {
                case "search_shipments" -> shipmentService.findShipments(text(args, "search"), limit(args));
                case "get_shipment" -> getShipment(args);
                case "list_profit_calculations" -> listCalculations(args);
                case "get_profit_summary" -> calculationService.summarize();
                default -> Map.of("error", "Unknown tool " + name);
            };

            return mapper.writeValueAsString(result);

        } catch (JsonProcessingException ex) {
            return "{\"error\": \"The arguments were not valid JSON.\"}";
        } catch (ShipmentNotFoundException | IllegalArgumentException ex) {
            return error(ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("chat_tool_failed tool={}", name, ex);
            return error("The tool failed while reading the data.");
        }
    }

    private Object getShipment(JsonNode args) {

        String reference = text(args, "reference");

        if (reference == null) {
            throw new IllegalArgumentException("reference is required.");
        }

        return shipmentService.findDetail(reference);
    }

    private Object listCalculations(JsonNode args) {

        String sort = SORTABLE.contains(text(args, "sort")) ? text(args, "sort") : "calculatedAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(text(args, "direction")) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Page<ProfitCalculationResponseDTO> page = calculationService.findAll(
                text(args, "search"), PageRequest.of(0, limit(args), Sort.by(direction, sort)));

        return Map.of("calculations", page.getContent(), "totalMatching", page.getTotalElements());
    }

    private static String text(JsonNode args, String field) {
        JsonNode value = args.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText().trim();
    }

    private static int limit(JsonNode args) {
        int requested = args.path("limit").asInt(10);
        return Math.min(Math.max(requested, 1), MAX_ROWS);
    }

    private String error(String message) {
        try {
            return mapper.writeValueAsString(Map.of("error", message == null ? "Unknown error" : message));
        } catch (JsonProcessingException ex) {
            return "{\"error\": \"Unknown error\"}";
        }
    }
}
