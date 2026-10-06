package com.businessmanager.backend.billing.dto;

import com.businessmanager.backend.billing.enums.InvoiceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for the status-transition endpoint
 * {@code PATCH /api/billings/{id}/status}.
 *
 * <p>Allowed transitions are enforced by the service layer; the controller
 * simply delegates the requested target status.
 */
@Data
public class StatusChangeRequest {

    @NotNull(message = "Target status is required")
    private InvoiceStatus status;
}
