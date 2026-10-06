package com.businessmanager.backend.supplier.repository;

import com.businessmanager.backend.supplier.entity.Supplier;
import com.businessmanager.backend.supplier.enums.SupplierStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    /**
     * Look up supplier by unique business key.
     */
    Optional<Supplier> findBySupplierCode(String supplierCode);

    /**
     * Filtered and paginated search for supplier records.
     */
    @Query("SELECT s FROM Supplier s WHERE " +
            "(:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:code IS NULL OR LOWER(s.supplierCode) LIKE LOWER(CONCAT('%', :code, '%'))) AND " +
            "(:phone IS NULL OR s.phone LIKE CONCAT('%', :phone, '%')) AND " +
            "(:status IS NULL OR s.status = :status)")
    Page<Supplier> searchSuppliers(
            @Param("name") String name,
            @Param("code") String code,
            @Param("phone") String phone,
            @Param("status") SupplierStatus status,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(s.runningBalance), 0) FROM Supplier s WHERE s.runningBalance > 0")
    java.math.BigDecimal calculateTotalOutstandingPayables();
}
