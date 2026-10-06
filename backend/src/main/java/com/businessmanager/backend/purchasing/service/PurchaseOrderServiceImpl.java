package com.businessmanager.backend.purchasing.service;

import com.businessmanager.backend.common.audit.annotation.AuditAction;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.product.entity.Product;
import com.businessmanager.backend.product.repository.ProductRepository;
import com.businessmanager.backend.purchasing.entity.PurchaseLine;
import com.businessmanager.backend.purchasing.entity.PurchaseOrder;
import com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus;
import com.businessmanager.backend.purchasing.repository.GoodsReceiptRepository;
import com.businessmanager.backend.purchasing.repository.PurchaseOrderRepository;
import com.businessmanager.backend.supplier.entity.Supplier;
import com.businessmanager.backend.supplier.repository.SupplierRepository;
import com.businessmanager.backend.accounting.entity.Account;
import com.businessmanager.backend.accounting.entity.JournalEntry;
import com.businessmanager.backend.accounting.entity.JournalLine;
import com.businessmanager.backend.accounting.repository.AccountMappingRepository;
import com.businessmanager.backend.accounting.repository.AccountRepository;
import com.businessmanager.backend.accounting.service.JournalEntryService;
import com.businessmanager.backend.common.event.SupplierPaymentPostedEvent;
import com.businessmanager.backend.fund.repository.FundAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryService journalEntryService;
    private final FundAccountRepository fundAccountRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AccountMappingRepository accountMappingRepository;

    @Override
    @Transactional
    @AuditAction(action = "CREATE", module = "PURCHASING")
    public PurchaseOrder createPurchaseOrder(PurchaseOrder purchaseOrder) {
        if (purchaseOrder.getSupplier() == null || purchaseOrder.getSupplier().getId() == null) {
            throw new BusinessRuleException("Supplier is required when creating a purchase order.");
        }
        Supplier supplier = supplierRepository.findById(purchaseOrder.getSupplier().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + purchaseOrder.getSupplier().getId()));
        
        purchaseOrder.setSupplier(supplier);

        if (purchaseOrder.getPoNumber() == null || purchaseOrder.getPoNumber().isBlank()) {
            purchaseOrder.setPoNumber(generateNextPoNumber("PO-"));
        } else {
            if (purchaseOrderRepository.existsByPoNumber(purchaseOrder.getPoNumber())) {
                throw new BusinessRuleException("PO number already exists: " + purchaseOrder.getPoNumber());
            }
        }

        if (purchaseOrder.getOrderDate() == null) {
            purchaseOrder.setOrderDate(LocalDate.now());
        }

        purchaseOrder.setStatus(PurchaseOrderStatus.DRAFT);

        // Re-add lines via addLine() to ensure the bidirectional purchaseOrder back-reference
        // is set on each line (so po_id is not null on INSERT).
        if (purchaseOrder.getLines() != null && !purchaseOrder.getLines().isEmpty()) {
            List<PurchaseLine> rawLines = new java.util.ArrayList<>(purchaseOrder.getLines());
            purchaseOrder.getLines().clear();
            for (PurchaseLine line : rawLines) {
                purchaseOrder.addLine(line);
            }
        }

        resolveAndComputeLines(purchaseOrder);

        return purchaseOrderRepository.save(purchaseOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrder getPurchaseOrderById(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrder getPurchaseOrderByPoNumber(String poNumber) {
        return purchaseOrderRepository.findByPoNumber(poNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found with PO Number: " + poNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrder> searchPurchaseOrders(String search, Long supplierId, PurchaseOrderStatus status, LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        return purchaseOrderRepository.searchPurchaseOrders(search, supplierId, status, dateFrom, dateTo, pageable);
    }

    @Override
    @Transactional
    @AuditAction(action = "UPDATE", module = "PURCHASING")
    public PurchaseOrder updatePurchaseOrder(Long id, PurchaseOrder updated) {
        PurchaseOrder existing = getPurchaseOrderById(id);

        if (existing.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT purchase orders can be edited. Current status: " + existing.getStatus());
        }

        if (updated.getSupplier() != null && updated.getSupplier().getId() != null) {
            Supplier supplier = supplierRepository.findById(updated.getSupplier().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + updated.getSupplier().getId()));
            existing.setSupplier(supplier);
        }

        if (updated.getOrderDate() != null) {
            existing.setOrderDate(updated.getOrderDate());
        }

        existing.getLines().clear();
        if (updated.getLines() != null) {
            for (PurchaseLine line : updated.getLines()) {
                existing.addLine(line);
            }
        }

        resolveAndComputeLines(existing);

        return purchaseOrderRepository.save(existing);
    }

    @Override
    @Transactional
    @AuditAction(action = "STATUS_CHANGE", module = "PURCHASING")
    public PurchaseOrder changeStatus(Long id, PurchaseOrderStatus newStatus) {
        PurchaseOrder existing = getPurchaseOrderById(id);
        
        if (existing.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot transition status of a CANCELLED purchase order.");
        }
        
        existing.setStatus(newStatus);
        return purchaseOrderRepository.save(existing);
    }

    @Override
    @Transactional
    @AuditAction(action = "CANCEL", module = "PURCHASING")
    public void cancelPurchaseOrder(Long id) {
        PurchaseOrder existing = getPurchaseOrderById(id);
        
        if (existing.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Purchase Order is already cancelled.");
        }
        
        if (existing.getStatus() == PurchaseOrderStatus.RECEIVED || existing.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED) {
             throw new BusinessRuleException("Cannot cancel a Purchase Order that has already been partially or fully received. You must reverse the receipts first.");
        }
        
        existing.setStatus(PurchaseOrderStatus.CANCELLED);
        purchaseOrderRepository.save(existing);
    }

    @Override
    @Transactional
    @AuditAction(action = "DELETE", module = "PURCHASING")
    public void deletePurchaseOrder(Long id) {
        PurchaseOrder existing = getPurchaseOrderById(id);

        // Received POs cannot be deleted — goods receipts must be reversed first
        if (existing.getStatus() == PurchaseOrderStatus.RECEIVED
                || existing.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new BusinessRuleException(
                    "Cannot delete a Purchase Order that has recorded goods receipts. "
                    + "Reverse the receipts first, then delete.");
        }

        // If goods have been received (safety check via receipt repository), also block
        BigDecimal totalReceived = goodsReceiptRepository.calculateTotalReceivedQtyForPo(id);
        if (totalReceived.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException(
                    "Cannot delete a Purchase Order that has recorded goods receipts. "
                    + "Reverse the receipts first, then delete.");
        }

        // Already cancelled — hard-delete directly
        if (existing.getStatus() == PurchaseOrderStatus.CANCELLED) {
            purchaseOrderRepository.delete(existing);
            return;
        }

        // DRAFT: hard-delete directly
        if (existing.getStatus() == PurchaseOrderStatus.DRAFT) {
            purchaseOrderRepository.delete(existing);
            return;
        }

        // Non-DRAFT, non-received (e.g. SENT/ORDERED): cancel first, then hard-delete
        existing.setStatus(PurchaseOrderStatus.CANCELLED);
        purchaseOrderRepository.save(existing);
        purchaseOrderRepository.delete(existing);
    }

    @Override
    @Transactional
    @AuditAction(action = "PAYMENT", module = "PURCHASING")
    public void recordSupplierPayment(Long id, Long paymentAccountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Payment amount must be positive.");
        }

        PurchaseOrder po = getPurchaseOrderById(id);

        if (po.getStatus() == PurchaseOrderStatus.DRAFT || po.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot record payment against a " + po.getStatus() + " purchase order.");
        }

        Account paymentAccount = accountRepository.findById(paymentAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment ledger account not found with ID: " + paymentAccountId));

        Account apAccount = accountMappingRepository.findByMappingKey("ACCOUNTS_PAYABLE")
                .map(com.businessmanager.backend.accounting.entity.AccountMapping::getAccount)
                .orElseThrow(() -> new BusinessRuleException("ACCOUNTS_PAYABLE mapping not found"));

        JournalEntry je = new JournalEntry();
        je.setEntryDate(LocalDate.now());
        je.setReferenceType("SUPPLIER_PAYMENT");
        je.setReferenceId(po.getId());
        je.setReferenceNumber("PAY-" + po.getPoNumber());
        je.setDescription("Payment to Supplier: " + po.getSupplier().getName() + " for PO: " + po.getPoNumber());
        je.setSourceModule("PURCHASING");
        je.setSourceDocumentId(po.getId());

        // DR Accounts Payable
        JournalLine apDebit = new JournalLine();
        apDebit.setAccount(apAccount);
        apDebit.setDebitAmount(amount);
        apDebit.setCreditAmount(BigDecimal.ZERO);
        apDebit.setDescription("AP Reduction — " + po.getPoNumber());
        je.addLine(apDebit);

        // CR Cash
        JournalLine cashCredit = new JournalLine();
        cashCredit.setAccount(paymentAccount);
        cashCredit.setDebitAmount(BigDecimal.ZERO);
        cashCredit.setCreditAmount(amount);
        cashCredit.setDescription("Payment Sent — " + po.getPoNumber());
        je.addLine(cashCredit);

        journalEntryService.postJournalEntry(je);

        // FUND-020: Publish event to trigger Fund Management deduction if the GL account maps to one
        fundAccountRepository.findByGlAccountId(paymentAccount.getId()).ifPresent(fundAccount -> {
            eventPublisher.publishEvent(new SupplierPaymentPostedEvent(
                    fundAccount.getId(),
                    po.getId(),
                    amount,
                    LocalDate.now()
            ));
        });

        // REP-010: Track payment against PO for Aging reports
        BigDecimal currentPaid = po.getAmountPaid() != null ? po.getAmountPaid() : BigDecimal.ZERO;
        po.setAmountPaid(currentPaid.add(amount));
        purchaseOrderRepository.save(po);
    }

    @Override
    @Transactional(readOnly = true)
    public String generateNextPoNumber(String prefix) {
        Optional<String> latest = purchaseOrderRepository.findLatestPoNumberByPrefix(prefix);
        if (latest.isPresent()) {
            String lastNum = latest.get().substring(prefix.length());
            try {
                int next = Integer.parseInt(lastNum) + 1;
                return prefix + String.format("%06d", next);
            } catch (NumberFormatException e) {
                return prefix + System.currentTimeMillis();
            }
        }
        return prefix + "000001";
    }

    private void resolveAndComputeLines(PurchaseOrder purchaseOrder) {
        if (purchaseOrder.getLines() == null || purchaseOrder.getLines().isEmpty()) {
            return; 
        }

        for (PurchaseLine line : purchaseOrder.getLines()) {
            if (line.getProduct() == null || line.getProduct().getId() == null) {
                throw new BusinessRuleException("Each purchase line must reference a product.");
            }
            Product product = productRepository.findById(line.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + line.getProduct().getId()));
            line.setProduct(product);

            if (line.getOrderedQty() == null || line.getOrderedQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException("Ordered quantity must be positive for product: " + product.getSku());
            }

            if (line.getCostPrice() == null || line.getCostPrice().compareTo(BigDecimal.ZERO) == 0) {
                line.setCostPrice(product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO);
            }

            if (line.getTaxAmount() == null) {
                line.setTaxAmount(BigDecimal.ZERO);
            }

            BigDecimal costTotal = line.getOrderedQty().multiply(line.getCostPrice()).setScale(2, RoundingMode.HALF_UP);
            line.setLineTotal(costTotal.add(line.getTaxAmount()).setScale(2, RoundingMode.HALF_UP));
        }
    }
}
