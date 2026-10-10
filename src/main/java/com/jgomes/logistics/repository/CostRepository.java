package com.jgomes.logistics.repository;

import com.jgomes.logistics.entity.Cost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CostRepository extends JpaRepository<Cost, Long> {

    List<Cost> findByShipmentId(Long shipmentId);

    boolean existsByIdAndShipmentId(Long id, Long shipmentId);

    /**
     * Corrects one cost amount with optimistic locking, as a single conditional
     * UPDATE: the row changes only if it belongs to the shipment and still carries
     * {@code expectedVersion}, and the version is bumped in the same statement.
     *
     * Checking and writing in one statement leaves no gap between reading the
     * version and changing the row, which a read-then-save would have. Returns the
     * number of rows changed: 1 on success, 0 when the row is missing, belongs to
     * another shipment, or was changed by someone else since it was read.
     *
     * The persistence context is flushed before and cleared after, so entities
     * loaded later in the same transaction see the new amount and version.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Cost c
               SET c.amount = :amount,
                   c.description = :description,
                   c.version = c.version + 1
             WHERE c.id = :id
               AND c.shipment.id = :shipmentId
               AND c.version = :expectedVersion
            """)
    int correctAmount(
            @Param("id") Long id,
            @Param("shipmentId") Long shipmentId,
            @Param("amount") BigDecimal amount,
            @Param("description") String description,
            @Param("expectedVersion") Long expectedVersion);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Cost c WHERE c.shipment.id = :shipmentId")
    BigDecimal sumAmountByShipmentId(@Param("shipmentId") Long shipmentId);
}
