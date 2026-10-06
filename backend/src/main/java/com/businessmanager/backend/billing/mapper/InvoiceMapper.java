package com.businessmanager.backend.billing.mapper;

import com.businessmanager.backend.billing.dto.InvoiceCreateRequest;
import com.businessmanager.backend.billing.dto.InvoiceLineRequest;
import com.businessmanager.backend.billing.dto.InvoiceLineResponseDto;
import com.businessmanager.backend.billing.dto.InvoiceResponseDto;
import com.businessmanager.backend.billing.dto.InvoiceSummaryDto;
import com.businessmanager.backend.billing.dto.InvoiceUpdateRequest;
import com.businessmanager.backend.billing.entity.Invoice;
import com.businessmanager.backend.billing.entity.InvoiceLine;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.util.List;

/**
 * MapStruct mapper for the Billing module.
 *
 * <p>Translates between REST DTOs and JPA entities. The service layer handles
 * business logic (product resolution, tax computation, total calculations);
 * this mapper only handles structural conversion.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InvoiceMapper {

    // ── InvoiceCreateRequest → Invoice entity ──────────────────────────

    /**
     * Convert a create-request DTO into a transient Invoice entity.
     * The customer field is set to a shell entity with only the ID so the
     * service layer can resolve the full Customer from the database.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)          // forced to DRAFT by service
    @Mapping(target = "amountPaid", ignore = true)      // starts at 0
    @Mapping(target = "subtotal", ignore = true)        // computed by service
    @Mapping(target = "taxTotal", ignore = true)        // computed by service
    @Mapping(target = "grandTotal", ignore = true)      // computed by service
    @Mapping(target = "customer.id", source = "customerId")
    @Mapping(target = "lines", source = "lines")
    Invoice toEntity(InvoiceCreateRequest dto);

    // ── InvoiceUpdateRequest → Invoice entity (used to carry update data) ─

    /**
     * Convert an update-request DTO into an overlay Invoice entity.
     * The service layer merges this into the persisted entity.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "invoiceNumber", ignore = true)
    @Mapping(target = "amountPaid", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "taxTotal", ignore = true)
    @Mapping(target = "grandTotal", ignore = true)
    @Mapping(target = "customer.id", source = "customerId")
    @Mapping(target = "lines", source = "lines")
    Invoice toEntity(InvoiceUpdateRequest dto);

    // ── InvoiceLineRequest → InvoiceLine entity ─────────────────────────

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoice", ignore = true)         // set by Invoice.addLine()
    @Mapping(target = "lineTotal", ignore = true)       // computed by service
    @Mapping(target = "product.id", source = "productId")
    InvoiceLine toEntity(InvoiceLineRequest dto);

    List<InvoiceLine> toEntityList(List<InvoiceLineRequest> dtos);

    // ── Invoice entity → InvoiceResponseDto ────────────────────────────

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "customerAddress", source = "customer.address")
    @Mapping(target = "customerGSTIN", source = "customer.taxId")
    @Mapping(target = "customerState", source = "customer.state")
    @Mapping(target = "customerStateCode", source = "customer.stateCode")
    @Mapping(target = "amountOutstanding", ignore = true)   // set in @AfterMapping
    InvoiceResponseDto toResponseDto(Invoice entity);

    @AfterMapping
    default void setOutstanding(Invoice entity, @MappingTarget InvoiceResponseDto dto) {
        BigDecimal outstanding = entity.getGrandTotal() == null ? BigDecimal.ZERO
                : entity.getGrandTotal().subtract(
                        entity.getAmountPaid() == null ? BigDecimal.ZERO : entity.getAmountPaid());
        dto.setAmountOutstanding(outstanding);
    }

    List<InvoiceResponseDto> toResponseDtoList(List<Invoice> entities);

    // ── Invoice entity → InvoiceSummaryDto ─────────────────────────────

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "amountOutstanding", ignore = true)
    InvoiceSummaryDto toSummaryDto(Invoice entity);

    @AfterMapping
    default void setSummaryOutstanding(Invoice entity, @MappingTarget InvoiceSummaryDto dto) {
        BigDecimal outstanding = entity.getGrandTotal() == null ? BigDecimal.ZERO
                : entity.getGrandTotal().subtract(
                        entity.getAmountPaid() == null ? BigDecimal.ZERO : entity.getAmountPaid());
        dto.setAmountOutstanding(outstanding);
    }

    List<InvoiceSummaryDto> toSummaryDtoList(List<Invoice> entities);

    // ── InvoiceLine entity → InvoiceLineResponseDto ─────────────────────

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "hsnCode", source = "product.hsnCode")
    InvoiceLineResponseDto toLineResponseDto(InvoiceLine entity);

    List<InvoiceLineResponseDto> toLineResponseDtoList(List<InvoiceLine> entities);
}
