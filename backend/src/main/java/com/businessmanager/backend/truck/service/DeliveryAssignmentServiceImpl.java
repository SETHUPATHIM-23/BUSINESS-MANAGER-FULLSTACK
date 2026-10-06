package com.businessmanager.backend.truck.service;

import com.businessmanager.backend.common.audit.annotation.AuditAction;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.truck.entity.DeliveryAssignment;
import com.businessmanager.backend.truck.enums.DeliveryStatus;
import com.businessmanager.backend.truck.repository.DeliveryAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryAssignmentServiceImpl implements DeliveryAssignmentService {

    private final DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Override
    @Transactional
    @AuditAction(action = "CREATE", module = "DELIVERY_ASSIGNMENT")
    public DeliveryAssignment createAssignment(DeliveryAssignment assignment) {
        return deliveryAssignmentRepository.save(assignment);
    }

    @Override
    public DeliveryAssignment getAssignmentById(Long id) {
        return deliveryAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery assignment not found with ID: " + id));
    }

    @Override
    @Transactional
    @AuditAction(action = "UPDATE_STATUS", module = "DELIVERY_ASSIGNMENT")
    public DeliveryAssignment updateAssignmentStatus(Long id, DeliveryStatus status) {
        DeliveryAssignment assignment = getAssignmentById(id);
        assignment.setStatus(status);
        return deliveryAssignmentRepository.save(assignment);
    }

    @Override
    @Transactional
    @AuditAction(action = "DELETE", module = "DELIVERY_ASSIGNMENT")
    public void deleteAssignment(Long id) {
        DeliveryAssignment assignment = getAssignmentById(id);
        deliveryAssignmentRepository.delete(assignment);
    }

    @Override
    public Page<DeliveryAssignment> searchAssignments(Long truckId, Long invoiceId, DeliveryStatus status, Pageable pageable) {
        return deliveryAssignmentRepository.searchAssignments(truckId, invoiceId, status, pageable);
    }
}
