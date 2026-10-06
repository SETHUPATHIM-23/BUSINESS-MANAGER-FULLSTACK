package com.businessmanager.backend.truck.service;

import com.businessmanager.backend.truck.entity.DeliveryAssignment;
import com.businessmanager.backend.truck.enums.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeliveryAssignmentService {
    DeliveryAssignment createAssignment(DeliveryAssignment assignment);
    DeliveryAssignment getAssignmentById(Long id);
    DeliveryAssignment updateAssignmentStatus(Long id, DeliveryStatus status);
    void deleteAssignment(Long id);
    Page<DeliveryAssignment> searchAssignments(Long truckId, Long invoiceId, DeliveryStatus status, Pageable pageable);
}
