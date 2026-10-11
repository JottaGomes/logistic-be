package com.jgomes.logistics.dto;

import java.math.BigDecimal;

/**
 * Portfolio-wide figures, taken from the most recent stored calculation of each
 * shipment. A shipment never calculated counts in {@code shipments} only.
 *
 * @param shipments           every shipment registered
 * @param shipmentsCalculated shipments with at least one stored calculation
 * @param shipmentsAtLoss     of those, the ones whose latest result is negative
 * @param totalProfitOrLoss   sum of the latest result of every calculated shipment, in EUR
 */
public record ProfitSummaryDTO(
        long shipments,
        long shipmentsCalculated,
        long shipmentsAtLoss,
        BigDecimal totalProfitOrLoss) {
}
