package com.businessmanager.backend.purchasing.service;

import com.businessmanager.backend.common.audit.annotation.AuditAction;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.product.entity.Product;
import com.businessmanager.backend.product.repository.ProductRepository;
import com.businessmanager.backend.purchasing.dto.SupplierInvoiceRequest;
import com.businessmanager.backend.purchasing.entity.PurchaseLine;
import com.businessmanager.backend.purchasing.entity.PurchaseOrder;
import com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus;
import com.businessmanager.backend.purchasing.repository.GoodsReceiptRepository;
import com.businessmanager.backend.purchasing.repository.PurchaseOrderRepository;
import com.businessmanager.backend.accounting.entity.Account;
import com.businessmanager.backend.accounting.entity.AccountMapping;
import com.businessmanager.backend.accounting.entity.JournalEntry;
import com.businessmanager.backend.accounting.entity.JournalLine;
import com.businessmanager.backend.accounting.repository.AccountMappingRepository;
import com.businessmanager.backend.accounting.repository.JournalEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ThreeWayMatchServiceImpl implements ThreeWayMatchService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final ProductRepository productRepository;
    private final AccountMappingRepository accountMappingRepository;
    private final JournalEntryRepository journalEntryRepository;

    @Value("${purchasing.tolerance.amount:10.00}")
    private BigDecimal toleranceAmount;

    @Override
    @Transactional
    @AuditAction(action = "THREE_WAY_MATCH", module = "PURCHASING")
    public void processSupplierInvoice(SupplierInvoiceRequest request) {
        PurchaseOrder po = purchaseOrderRepository.findById(request.getPurchaseOrderId())
                .orElseThrow(() -> new BusinessRuleException("Purchase Order not found with ID: " + request.getPurchaseOrderId()));

        BigDecimal calculatedExpectedSubtotal = BigDecimal.ZERO;
        BigDecimal totalOrderedQty = BigDecimal.ZERO;
        BigDecimal totalReceivedQty = BigDecimal.ZERO;

        for (PurchaseLine line : po.getLines()) {
            BigDecimal receivedQty = goodsReceiptRepository.calculateTotalReceivedQtyForLine(line.getId());
            totalOrderedQty = totalOrderedQty.add(line.getOrderedQty());
            totalReceivedQty = totalReceivedQty.add(receivedQty);
            
            BigDecimal expectedLineTotal = receivedQty.multiply(line.getCostPrice());
            calculatedExpectedSubtotal = calculatedExpectedSubtotal.add(expectedLineTotal);
        }

        // 1. Three-Way Match Check (Ordered vs Received vs Invoiced)
        if (totalReceivedQty.compareTo(BigDecimal.ZERO) == 0) {
             throw new BusinessRuleException("Cannot process supplier invoice. No goods have been received for this PO yet.");
        }

        BigDecimal difference = request.getSupplierInvoicedSubtotal().subtract(calculatedExpectedSubtotal).abs();
        
        // Flag mismatch beyond configurable tolerance for manual approval
        if (difference.compareTo(toleranceAmount) > 0 && !request.isForceApprove()) {
            throw new BusinessRuleException(
                String.format("Three-way match failed. Expected subtotal based on received quantities: $%s. Supplier invoiced: $%s. Difference ($%s) exceeds tolerance of $%s. Manual approval required (set forceApprove=true).",
                calculatedExpectedSubtotal, request.getSupplierInvoicedSubtotal(), difference, toleranceAmount)
            );
        }

        // 2. Landed Cost Allocation (Freight, Duties) - PURCH-080
        BigDecimal totalLandedCosts = request.getFreightAmount().add(request.getDutiesAmount());
        
        if (calculatedExpectedSubtotal.compareTo(BigDecimal.ZERO) > 0) {
            for (PurchaseLine line : po.getLines()) {
                BigDecimal receivedQty = goodsReceiptRepository.calculateTotalReceivedQtyForLine(line.getId());
                if (receivedQty.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal expectedLineTotal = receivedQty.multiply(line.getCostPrice());
                    
                    // Apportion landed cost by relative value of the received line
                    BigDecimal allocationFactor = expectedLineTotal.divide(calculatedExpectedSubtotal, 4, RoundingMode.HALF_UP);
                    BigDecimal lineLandedCost = totalLandedCosts.multiply(allocationFactor);
                    
                    // Apportion any invoice variance to the line cost as well
                    // variance = (supplierInvoicedSubtotal - expectedSubtotal) * allocationFactor
                    BigDecimal varianceTotal = request.getSupplierInvoicedSubtotal().subtract(calculatedExpectedSubtotal);
                    BigDecimal lineVariance = varianceTotal.multiply(allocationFactor);
                    
                    BigDecimal totalCostAdjustment = lineLandedCost.add(lineVariance);
                    
                    // Update Product effective cost price (Moving Average Cost adjustment)
                    Product product = line.getProduct();
                    BigDecimal currentStock = product.getStockOnHand();
                    
                    if (currentStock.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal currentTotalValue = currentStock.multiply(product.getCostPrice());
                        BigDecimal newTotalValue = currentTotalValue.add(totalCostAdjustment);
                        
                        BigDecimal newMac = newTotalValue.divide(currentStock, 4, RoundingMode.HALF_UP);
                        
                        if (newMac.compareTo(BigDecimal.ZERO) < 0) {
                             newMac = BigDecimal.ZERO;
                        }
                        
                        product.setCostPrice(newMac);
                        productRepository.save(product);
                    }
                }
            }
        }
        
        // 3. Finalize PO Status
        if (totalReceivedQty.compareTo(totalOrderedQty) >= 0) {
             po.setStatus(PurchaseOrderStatus.RECEIVED);
        } else {
             po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }
        
        purchaseOrderRepository.save(po);

        // 4. Generate Journal Entry for Supplier Invoice (Accounts Payable)
        generateInvoiceJournalEntry(po, calculatedExpectedSubtotal, request);
    }

    private void generateInvoiceJournalEntry(PurchaseOrder po, BigDecimal accruedSubtotal, SupplierInvoiceRequest request) {
        Account accruedPurchasesAccount = resolveAccount("ACCRUED_PURCHASES");
        Account apAccount = resolveAccount("ACCOUNTS_PAYABLE");
        Account inventoryAccount = resolveAccount("INVENTORY_ASSET");
        // We might also need a Freight/Duties expense account if we didn't capitalize it, 
        // but since we capitilized landed costs to inventory (MAC adjustment), we debit Inventory for it!
        
        JournalEntry je = new JournalEntry();
        je.setEntryDate(request.getInvoiceDate() != null ? request.getInvoiceDate() : java.time.LocalDate.now());
        je.setReferenceType("SUPPLIER_INVOICE");
        je.setReferenceId(po.getId());
        je.setReferenceNumber(request.getSupplierInvoiceNumber());
        je.setDescription("Supplier Invoice for PO: " + po.getPoNumber() + " from " + po.getSupplier().getName());

        // DR Accrued Purchases (Clear the GRNI accrual)
        if (accruedSubtotal.compareTo(BigDecimal.ZERO) > 0) {
            JournalLine accruedDebit = new JournalLine();
            accruedDebit.setAccount(accruedPurchasesAccount);
            accruedDebit.setDebitAmount(accruedSubtotal);
            accruedDebit.setCreditAmount(BigDecimal.ZERO);
            accruedDebit.setDescription("Clear Accrual — " + po.getPoNumber());
            je.addLine(accruedDebit);
        }

        // Variance & Landed Costs were capitalized into Inventory MAC during match
        BigDecimal totalLandedCostsAndVariance = request.getFreightAmount().add(request.getDutiesAmount())
                .add(request.getSupplierInvoicedSubtotal().subtract(accruedSubtotal));
        
        if (totalLandedCostsAndVariance.compareTo(BigDecimal.ZERO) != 0) {
            JournalLine varianceLine = new JournalLine();
            varianceLine.setAccount(inventoryAccount);
            varianceLine.setDescription("Capitalized Landed Costs / Variance — " + po.getPoNumber());
            if (totalLandedCostsAndVariance.compareTo(BigDecimal.ZERO) > 0) {
                varianceLine.setDebitAmount(totalLandedCostsAndVariance);
                varianceLine.setCreditAmount(BigDecimal.ZERO);
            } else {
                varianceLine.setDebitAmount(BigDecimal.ZERO);
                varianceLine.setCreditAmount(totalLandedCostsAndVariance.abs());
            }
            je.addLine(varianceLine);
        }

        // CR Accounts Payable (Total liability to supplier)
        BigDecimal totalPayable = request.getSupplierInvoicedSubtotal().add(request.getFreightAmount()).add(request.getDutiesAmount());
        if (totalPayable.compareTo(BigDecimal.ZERO) > 0) {
            JournalLine apCredit = new JournalLine();
            apCredit.setAccount(apAccount);
            apCredit.setDebitAmount(BigDecimal.ZERO);
            apCredit.setCreditAmount(totalPayable);
            apCredit.setDescription("Accounts Payable — " + request.getSupplierInvoiceNumber());
            je.addLine(apCredit);
        }

        // Verify balance
        BigDecimal totalDebits = je.getLines().stream().map(JournalLine::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = je.getLines().stream().map(JournalLine::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalDebits.compareTo(totalCredits) != 0) {
             throw new BusinessRuleException("Unbalanced journal entry generated for Supplier Invoice! DR: " + totalDebits + ", CR: " + totalCredits);
        }

        journalEntryRepository.save(je);
    }

    private Account resolveAccount(String mappingKey) {
        AccountMapping mapping = accountMappingRepository.findByMappingKey(mappingKey)
                .orElseThrow(() -> new BusinessRuleException(
                        "Account mapping not configured for key: " + mappingKey
                        + ". Please configure in account_mappings."));
        return mapping.getAccount();
    }
}
