package com.jgomes.logistics.repository;

import com.jgomes.logistics.entity.ProfitCalculation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProfitCalculationRepository extends JpaRepository<ProfitCalculation, Long> {

    /**
     * Count, losses and total over the latest calculation of each shipment, in one
     * query. Earlier calculations of the same shipment are history and must not be
     * counted twice; ids are assigned in insertion order, so the highest id per
     * shipment is its latest calculation.
     *
     * Returns one row: [calculated shipments, shipments at a loss, total profit or loss].
     */
    @Query("""
            SELECT COUNT(c),
                   COALESCE(SUM(CASE WHEN c.profitOrLoss < 0 THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(c.profitOrLoss), 0)
              FROM ProfitCalculation c
             WHERE c.id IN (SELECT MAX(l.id) FROM ProfitCalculation l GROUP BY l.shipment.id)
            """)
    List<Object[]> summarizeLatest();

    /** Fetches the shipment alongside each row, so listing does not trigger N+1 queries. */
    @Override
    @EntityGraph(attributePaths = "shipment")
    Page<ProfitCalculation> findAll(Pageable pageable);

    /**
     * Filters on the shipment the calculation belongs to. Done here rather than in
     * the browser because the listing is paged: filtering the page that happens to
     * be loaded would hide matches sitting on every other page.
     */
    @EntityGraph(attributePaths = "shipment")
    @Query("""
            SELECT c FROM ProfitCalculation c
            WHERE LOWER(c.shipment.reference) LIKE :pattern
               OR LOWER(c.shipment.customer) LIKE :pattern
            """)
    Page<ProfitCalculation> findByShipmentMatching(@Param("pattern") String pattern, Pageable pageable);
}
