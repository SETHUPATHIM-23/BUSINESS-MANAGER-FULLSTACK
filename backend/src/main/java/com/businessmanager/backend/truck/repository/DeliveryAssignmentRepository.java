package com.businessmanager.backend.truck.repository;

import com.businessmanager.backend.truck.entity.DeliveryAssignment;
import com.businessmanager.backend.truck.enums.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Long> {

    List<DeliveryAssignment> findByTruckId(Long truckId);

    List<DeliveryAssignment> findByInvoiceId(Long invoiceId);

    boolean existsByTruckId(Long truckId);

    @Query("SELECT d FROM DeliveryAssignment d WHERE " +
            "(:truckId IS NULL OR d.truck.id = :truckId) AND " +
            "(:invoiceId IS NULL OR d.invoice.id = :invoiceId) AND " +
            "(:status IS NULL OR d.status = :status)")
    Page<DeliveryAssignment> searchAssignments(
            @Param("truckId") Long truckId,
            @Param("invoiceId") Long invoiceId,
            @Param("status") DeliveryStatus status,
            Pageable pageable
    );
}
