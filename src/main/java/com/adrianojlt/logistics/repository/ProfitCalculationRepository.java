package com.adrianojlt.logistics.repository;

import com.adrianojlt.logistics.entity.ProfitCalculation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfitCalculationRepository extends JpaRepository<ProfitCalculation, Long> {

    /** Fetches the shipment alongside each row, so listing does not trigger N+1 queries. */
    @Override
    @EntityGraph(attributePaths = "shipment")
    Page<ProfitCalculation> findAll(Pageable pageable);
}
