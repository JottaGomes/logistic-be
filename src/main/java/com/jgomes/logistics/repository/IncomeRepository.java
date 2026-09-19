package com.jgomes.logistics.repository;

import com.jgomes.logistics.entity.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface IncomeRepository extends JpaRepository<Income, Long> {

    List<Income> findByShipmentId(Long shipmentId);

    /**
     * Sums in the database rather than loading every row into memory, so the cost
     * of a calculation does not grow with the number of records on the shipment.
     * COALESCE keeps the result at zero for a shipment with no income yet.
     */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Income i WHERE i.shipment.id = :shipmentId")
    BigDecimal sumAmountByShipmentId(@Param("shipmentId") Long shipmentId);
}
