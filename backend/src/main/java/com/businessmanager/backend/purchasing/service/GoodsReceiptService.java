package com.businessmanager.backend.purchasing.service;

import com.businessmanager.backend.common.audit.annotation.AuditAction;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.product.entity.Product;
import com.businessmanager.backend.product.entity.InventoryTransaction;
import com.businessmanager.backend.product.repository.ProductRepository;
import com.businessmanager.backend.product.repository.InventoryTransactionRepository;
import com.businessmanager.backend.purchasing.dto.GoodsReceiptRequest;
import com.businessmanager.backend.purchasing.entity.GoodsReceipt;
import com.businessmanager.backend.purchasing.entity.PurchaseLine;
import com.businessmanager.backend.purchasing.entity.PurchaseOrder;
import com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus;
import com.businessmanager.backend.purchasing.repository.GoodsReceiptRepository;
import com.businessmanager.backend.purchasing.repository.PurchaseLineRepository;
import com.businessmanager.backend.purchasing.repository.PurchaseOrderRepository;
import com.businessmanager.backend.accounting.entity.Account;
import com.businessmanager.backend.accounting.entity.AccountMapping;
import com.businessmanager.backend.accounting.entity.JournalEntry;
import com.businessmanager.backend.accounting.entity.JournalLine;
import com.businessmanager.backend.accounting.repository.AccountMappingRepository;
import com.businessmanager.backend.accounting.repository.JournalEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class GoodsReceiptService {
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseLineRepository purchaseLineRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final AccountMappingRepository accountMappingRepository;
    private final JournalEntryRepository journalEntryRepository;

    @Transactional
    @AuditAction(action = "RECEIVE_GOODS", module = "PURCHASING")
    public void receiveGoods(GoodsReceiptRequest request) {
        PurchaseOrder po = purchaseOrderRepository.findById(request.getPoId())
                .orElseThrow(() -> new BusinessRuleException("Purchase Order not found with ID: " + request.getPoId()));
                
        if (po.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot receive goods for a cancelled Purchase Order.");
        }

        BigDecimal totalReceivedThisTime = BigDecimal.ZERO;
        BigDecimal totalReceivedValue = BigDecimal.ZERO;

        for (GoodsReceiptRequest.GoodsReceiptLineRequest lineReq : request.getLines()) {
            if (lineReq.getReceivedQty().compareTo(BigDecimal.ZERO) <= 0) continue;
            
            PurchaseLine line = purchaseLineRepository.findById(lineReq.getPurchaseLineId())
                    .orElseThrow(() -> new BusinessRuleException("Purchase Line not found with ID: " + lineReq.getPurchaseLineId()));
                    
            if (!line.getPurchaseOrder().getId().equals(po.getId())) {
                 throw new BusinessRuleException("Line ID " + line.getId() + " does not belong to PO ID " + po.getId());
            }
                    
            GoodsReceipt receipt = new GoodsReceipt();
            receipt.setPurchaseOrder(po);
            receipt.setPurchaseLine(line);
            receipt.setReceivedDate(request.getReceivedDate());
            receipt.setReceivedQty(lineReq.getReceivedQty());
            goodsReceiptRepository.save(receipt);
            
            // Stock maintenance disabled - no stock updates or inventory transactions saved.
            
            totalReceivedThisTime = totalReceivedThisTime.add(lineReq.getReceivedQty());
            totalReceivedValue = totalReceivedValue.add(lineReq.getReceivedQty().multiply(line.getCostPrice()));
        }
        
        if (totalReceivedThisTime.compareTo(BigDecimal.ZERO) > 0) {
            if (po.getStatus() == PurchaseOrderStatus.DRAFT || po.getStatus() == PurchaseOrderStatus.SUBMITTED) {
                po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
                purchaseOrderRepository.save(po);
            }
            
            // Create Journal Entry for Stock Accrual
            generateReceiptJournalEntry(po, request.getReceivedDate(), totalReceivedValue);
        }
    }

    private void generateReceiptJournalEntry(PurchaseOrder po, java.time.LocalDate receivedDate, BigDecimal totalReceivedValue) {
        if (totalReceivedValue.compareTo(BigDecimal.ZERO) <= 0) return;

        Account inventoryAccount = resolveAccount("INVENTORY_ASSET");
        Account accruedPurchasesAccount = resolveAccount("ACCRUED_PURCHASES");

        JournalEntry je = new JournalEntry();
        je.setEntryDate(receivedDate != null ? receivedDate : java.time.LocalDate.now());
        je.setReferenceType("GOODS_RECEIPT");
        je.setReferenceId(po.getId());
        je.setReferenceNumber("GR-" + po.getPoNumber());
        je.setDescription("Goods Receipt Accrual for PO: " + po.getPoNumber());

        // DR Inventory Asset
        JournalLine invDebit = new JournalLine();
        invDebit.setAccount(inventoryAccount);
        invDebit.setDebitAmount(totalReceivedValue);
        invDebit.setCreditAmount(BigDecimal.ZERO);
        invDebit.setDescription("Inventory Increase — " + po.getPoNumber());
        je.addLine(invDebit);

        // CR Accrued Purchases
        JournalLine accruedCredit = new JournalLine();
        accruedCredit.setAccount(accruedPurchasesAccount);
        accruedCredit.setDebitAmount(BigDecimal.ZERO);
        accruedCredit.setCreditAmount(totalReceivedValue);
        accruedCredit.setDescription("Accrued Liability — " + po.getPoNumber());
        je.addLine(accruedCredit);

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
