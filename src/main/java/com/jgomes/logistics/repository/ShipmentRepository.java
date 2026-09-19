package com.jgomes.logistics.repository;

import com.jgomes.logistics.entity.Shipment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByReference(String reference);

    /**
     * A bounded, searchable slice rather than everything.
     *
     * At the 10,000 shipments a day the requirements ask for, returning the whole
     * table was a 639kB response feeding a dropdown with ten thousand options.
     */
    @Query("""
            SELECT s FROM Shipment s
            WHERE LOWER(s.reference) LIKE :pattern
               OR LOWER(s.customer) LIKE :pattern
            ORDER BY s.reference
            """)
    List<Shipment> search(@Param("pattern") String pattern, Pageable pageable);

    @Query("SELECT s FROM Shipment s ORDER BY s.reference")
    List<Shipment> findSlice(Pageable pageable);
}
