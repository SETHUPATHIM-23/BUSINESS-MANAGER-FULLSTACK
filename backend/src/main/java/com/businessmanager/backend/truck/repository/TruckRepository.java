package com.businessmanager.backend.truck.repository;

import com.businessmanager.backend.truck.entity.Truck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TruckRepository extends JpaRepository<Truck, Long> {

    /**
     * Lookup truck by unique registration number.
     */
    Optional<Truck> findByRegistrationNumber(String registrationNumber);

    /**
     * Check if registration number exists.
     */
    boolean existsByRegistrationNumber(String registrationNumber);

    /**
     * Filtered and paginated search for truck records.
     */
    @Query("SELECT t FROM Truck t WHERE " +
            "(:registrationNumber IS NULL OR LOWER(t.registrationNumber) LIKE LOWER(CONCAT('%', :registrationNumber, '%'))) AND " +
            "(:make IS NULL OR LOWER(t.make) LIKE LOWER(CONCAT('%', :make, '%'))) AND " +
            "(:model IS NULL OR LOWER(t.model) LIKE LOWER(CONCAT('%', :model, '%'))) AND " +
            "(:driverId IS NULL OR t.driver.id = :driverId)")
    Page<Truck> searchTrucks(
            @Param("registrationNumber") String registrationNumber,
            @Param("make") String make,
            @Param("model") String model,
            @Param("driverId") Long driverId,
            Pageable pageable
    );
}
