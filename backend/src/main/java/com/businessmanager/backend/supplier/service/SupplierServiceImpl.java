package com.businessmanager.backend.supplier.service;

import com.businessmanager.backend.common.audit.annotation.AuditAction;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.fund.entity.FundTransaction;
import com.businessmanager.backend.fund.repository.FundTransactionRepository;
import com.businessmanager.backend.purchasing.entity.PurchaseOrder;
import com.businessmanager.backend.purchasing.repository.PurchaseOrderRepository;
import com.businessmanager.backend.supplier.dto.SupplierCreateDto;
import com.businessmanager.backend.supplier.dto.SupplierReconciliationDto;
import com.businessmanager.backend.supplier.dto.SupplierResponseDto;
import com.businessmanager.backend.supplier.dto.SupplierStatementResponse;
import com.businessmanager.backend.supplier.dto.SupplierSummaryDto;
import com.businessmanager.backend.supplier.dto.SupplierUpdateDto;
import com.businessmanager.backend.supplier.entity.Supplier;
import com.businessmanager.backend.supplier.enums.SupplierStatus;
import com.businessmanager.backend.supplier.mapper.SupplierMapper;
import com.businessmanager.backend.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final FundTransactionRepository fundTransactionRepository;

    @Override
    @Transactional
    @AuditAction(action = "CREATE", module = "SUPPLIER")
    public SupplierResponseDto createSupplier(SupplierCreateDto dto) {
        // Validate unique supplier_code constraint
        if (supplierRepository.findBySupplierCode(dto.getSupplierCode()).isPresent()) {
            throw new BusinessRuleException("Supplier code '" + dto.getSupplierCode() + "' is already in use.");
        }

        Supplier supplier = supplierMapper.toEntity(dto);
        supplier.setStatus(SupplierStatus.ACTIVE);
        supplier.setRunningBalance(dto.getOpeningBalance());
        Supplier savedSupplier = supplierRepository.save(supplier);
        return supplierMapper.toDto(savedSupplier);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponseDto getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + id));
        return supplierMapper.toDto(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponseDto getSupplierByCode(String code) {
        Supplier supplier = supplierRepository.findBySupplierCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with code: " + code));
        return supplierMapper.toDto(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierSummaryDto> searchSuppliers(String name, String code, String phone, SupplierStatus status, Pageable pageable) {
        Page<Supplier> suppliers = supplierRepository.searchSuppliers(name, code, phone, status, pageable);
        return suppliers.map(supplierMapper::toSummaryDto);
    }

    @Override
    @Transactional
    @AuditAction(action = "UPDATE", module = "SUPPLIER")
    public SupplierResponseDto updateSupplier(Long id, SupplierUpdateDto dto) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + id));

        supplierMapper.updateEntityFromDto(dto, supplier);
        Supplier updatedSupplier = supplierRepository.save(supplier);
        return supplierMapper.toDto(updatedSupplier);
    }

    @Override
    @Transactional
    @AuditAction(action = "DEACTIVATE", module = "SUPPLIER")
    public void deactivateSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + id));

        supplier.setStatus(SupplierStatus.INACTIVE);
        supplierRepository.save(supplier);
    }

    @Override
    @Transactional
    @AuditAction(action = "DELETE", module = "SUPPLIER")
    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + id));

        // Enforce SUPP-050: prevent deletion of a supplier with transaction history
        if (hasTransactionHistory(supplier)) {
            throw new BusinessRuleException("Supplier has transaction history and cannot be deleted. Deactivate instead.");
        }

        supplierRepository.delete(supplier);
    }

    /**
     * Helper to verify if the supplier has existing purchase transactions.
     */
    private boolean hasTransactionHistory(Supplier supplier) {
        List<PurchaseOrder> pos = purchaseOrderRepository.findBySupplierIdOrderByOrderDateDesc(supplier.getId());
        return !pos.isEmpty();
    }

    @Override
    @Transactional
    public void adjustBalance(Long id, BigDecimal amount) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + id));
        supplier.setRunningBalance(supplier.getRunningBalance().add(amount));
        supplierRepository.save(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierReconciliationDto reconcileBalance(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + id));

        BigDecimal transactionsSum = getRecomputedTransactionsSum(supplier);
        BigDecimal recomputed = supplier.getOpeningBalance().add(transactionsSum);
        
        BigDecimal stored = supplier.getRunningBalance();
        BigDecimal drift = stored.subtract(recomputed);
        boolean reconciled = drift.compareTo(BigDecimal.ZERO) == 0;

        return SupplierReconciliationDto.builder()
                .supplierCode(supplier.getSupplierCode())
                .supplierName(supplier.getName())
                .storedBalance(stored)
                .recomputedBalance(recomputed)
                .drift(drift)
                .reconciled(reconciled)
                .build();
    }

    private BigDecimal getRecomputedTransactionsSum(Supplier supplier) {
        List<PurchaseOrder> pos = purchaseOrderRepository.findBySupplierIdOrderByOrderDateDesc(supplier.getId());
        return pos.stream()
                .filter(po -> po.getStatus() == com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus.RECEIVED || 
                              po.getStatus() == com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus.PARTIALLY_RECEIVED)
                .map(PurchaseOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierStatementResponse getStatement(
            Long supplierId, 
            LocalDate startDate, 
            LocalDate endDate
    ) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + supplierId));

        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(30);

        List<PurchaseOrder> pos = purchaseOrderRepository.findBySupplierIdOrderByOrderDateDesc(supplierId);
        List<FundTransaction> fundTxs = fundTransactionRepository
                .findByReferenceDocumentTypeAndReferenceDocumentIdOrderByTransactionDateDesc("SUPPLIER", supplierId);

        BigDecimal initialOpening = supplier.getOpeningBalance() != null ? supplier.getOpeningBalance() : BigDecimal.ZERO;

        // Calculate period opening balance (initial opening + pre-period purchases - pre-period settlements)
        BigDecimal prePurchased = pos.stream()
                .filter(po -> po.getStatus() == com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus.RECEIVED || 
                              po.getStatus() == com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus.PARTIALLY_RECEIVED)
                .filter(po -> po.getOrderDate() != null && po.getOrderDate().isBefore(start))
                .map(po -> po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal prePaid = fundTxs.stream()
                .filter(tx -> tx.getTransactionDate() != null && tx.getTransactionDate().isBefore(start))
                .map(tx -> tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal periodOpeningBalance = initialOpening.add(prePurchased).subtract(prePaid);

        List<SupplierStatementResponse.StatementEntry> entries = new ArrayList<>();
        BigDecimal totalPurchasedInPeriod = BigDecimal.ZERO;
        BigDecimal totalSettledInPeriod = BigDecimal.ZERO;

        for (PurchaseOrder po : pos) {
            if ((po.getStatus() == com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus.RECEIVED || 
                 po.getStatus() == com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus.PARTIALLY_RECEIVED) &&
                po.getOrderDate() != null &&
                !po.getOrderDate().isBefore(start) &&
                !po.getOrderDate().isAfter(end)) {

                BigDecimal poTotal = po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO;
                totalPurchasedInPeriod = totalPurchasedInPeriod.add(poTotal);

                entries.add(SupplierStatementResponse.StatementEntry.builder()
                        .date(po.getOrderDate())
                        .documentCode(po.getPoNumber())
                        .description("Invoiced - PO No: " + po.getPoNumber())
                        .type("PURCHASE_ORDER")
                        .amount(poTotal)
                        .build());
            }
        }

        for (FundTransaction tx : fundTxs) {
            if (tx.getTransactionDate() != null &&
                !tx.getTransactionDate().isBefore(start) &&
                !tx.getTransactionDate().isAfter(end)) {

                BigDecimal amount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
                totalSettledInPeriod = totalSettledInPeriod.add(amount);

                String payDesc = "Payment Paid";
                if (tx.getDescription() != null && !tx.getDescription().isBlank()) {
                    String desc = tx.getDescription().replace("#", "");
                    if (desc.toLowerCase().contains("po") || desc.toLowerCase().contains("invoice") || desc.toLowerCase().contains("order")) {
                        payDesc = "Payment Paid - Against " + desc;
                    } else {
                        payDesc = "Payment Paid - " + desc;
                    }
                }

                entries.add(SupplierStatementResponse.StatementEntry.builder()
                        .date(tx.getTransactionDate())
                        .documentCode("SETTLE-" + tx.getId())
                        .description(payDesc)
                        .type("SETTLEMENT")
                        .amount(amount.negate())
                        .build());
            }
        }

        entries.sort((a, b) -> {
            int comp = a.getDate().compareTo(b.getDate());
            return comp != 0 ? comp : a.getDocumentCode().compareTo(b.getDocumentCode());
        });

        BigDecimal closingBalance = periodOpeningBalance.add(totalPurchasedInPeriod).subtract(totalSettledInPeriod);

        return SupplierStatementResponse.builder()
                .supplierCode(supplier.getSupplierCode())
                .supplierName(supplier.getName())
                .bankAccountDetails(supplier.getBankAccountDetails())
                .openingBalance(periodOpeningBalance)
                .closingBalance(closingBalance)
                .startDate(start)
                .endDate(end)
                .entries(entries)
                .totalUnpaidInvoices(totalPurchasedInPeriod)
                .totalUnappliedDebits(totalSettledInPeriod)
                .reconciledBalance(closingBalance)
                .build();
    }
}
