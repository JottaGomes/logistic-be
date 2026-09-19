package com.adrianojlt.logistics.repository;

import com.adrianojlt.logistics.entity.ProfitCalculation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfitCalculationRepository extends JpaRepository<ProfitCalculation, Long> {

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
