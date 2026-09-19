package com.adrianojlt.logistics.repository;

import com.adrianojlt.logistics.entity.Cost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CostRepository extends JpaRepository<Cost, Long> {

    List<Cost> findByShipmentId(Long shipmentId);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Cost c WHERE c.shipment.id = :shipmentId")
    BigDecimal sumAmountByShipmentId(@Param("shipmentId") Long shipmentId);
}
