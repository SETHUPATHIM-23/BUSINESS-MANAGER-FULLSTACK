package com.businessmanager.backend.reports.service;

import com.businessmanager.backend.billing.entity.Invoice;
import com.businessmanager.backend.billing.repository.InvoiceRepository;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.customer.entity.Customer;
import com.businessmanager.backend.customer.repository.CustomerRepository;
import com.businessmanager.backend.product.entity.Product;
import com.businessmanager.backend.product.enums.ProductStatus;
import com.businessmanager.backend.product.repository.ProductRepository;
import com.businessmanager.backend.purchasing.entity.PurchaseOrder;
import com.businessmanager.backend.purchasing.repository.PurchaseOrderRepository;
import com.businessmanager.backend.reports.dto.*;
import com.businessmanager.backend.supplier.entity.Supplier;
import com.businessmanager.backend.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.businessmanager.backend.fund.entity.FundTransaction;
import com.businessmanager.backend.fund.repository.FundTransactionRepository;

@Service
@RequiredArgsConstructor
public class OperationalReportServiceImpl implements OperationalReportService {

    private final InvoiceRepository invoiceRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final FundTransactionRepository fundTransactionRepository;
    private final com.businessmanager.backend.employee.repository.EmployeeRepository employeeRepository;
    private final com.businessmanager.backend.employee.repository.EmployeeWorkEntryRepository employeeWorkEntryRepository;
    private final com.businessmanager.backend.employee.repository.EmployeeSettlementRepository employeeSettlementRepository;

    @Autowired
    private com.businessmanager.backend.settings.repository.SystemSettingsRepository systemSettingsRepository;

    private StatementReportDto.CompanyInfo getOurCompanyInfo() {
        var optSettings = systemSettingsRepository.findFirstByOrderByIdAsc();
        String name = optSettings.map(s -> s.getCompanyName()).orElse("");
        String address = optSettings.map(s -> s.getAddress()).orElse("");
        String phone = optSettings.map(s -> s.getMobile() != null ? s.getMobile() : s.getTelephone()).orElse("");
        String email = optSettings.map(s -> s.getEmail()).orElse("");
        String taxId = optSettings.map(s -> s.getGstin()).orElse("");

        return StatementReportDto.CompanyInfo.builder()
                .name(name != null ? name : "")
                .address(address != null ? address : "")
                .phone(phone != null ? phone : "")
                .email(email != null ? email : "")
                .taxId(taxId != null ? taxId : "")
                .build();
    }

    private OutstandingSummaryReportDto.CompanyInfo getOurCompanyInfoForSummary() {
        var optSettings = systemSettingsRepository.findFirstByOrderByIdAsc();
        String name = optSettings.map(s -> s.getCompanyName()).orElse("");
        String address = optSettings.map(s -> s.getAddress()).orElse("");
        String phone = optSettings.map(s -> s.getMobile() != null ? s.getMobile() : s.getTelephone()).orElse("");
        String email = optSettings.map(s -> s.getEmail()).orElse("");
        String taxId = optSettings.map(s -> s.getGstin()).orElse("");

        return OutstandingSummaryReportDto.CompanyInfo.builder()
                .name(name != null ? name : "")
                .address(address != null ? address : "")
                .phone(phone != null ? phone : "")
                .email(email != null ? email : "")
                .taxId(taxId != null ? taxId : "")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SalesReportDto generateSalesReport(LocalDate startDate, LocalDate endDate, Long customerId) {
        List<Invoice> invoices = invoiceRepository.findForSalesReport(customerId, startDate, endDate);

        List<Invoice> validInvoices = invoices.stream()
                .filter(i -> i.getStatus() != null && !i.getStatus().name().equals("CANCELLED") && !i.getStatus().name().equals("VOIDED"))
                .collect(Collectors.toList());

        BigDecimal totalRevenue = validInvoices.stream()
                .map(i -> i.getGrandTotal() != null ? i.getGrandTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<SalesReportDto.SalesLineDto> lines = validInvoices.stream()
                .map(i -> SalesReportDto.SalesLineDto.builder()
                        .invoiceNumber(i.getInvoiceNumber() != null ? i.getInvoiceNumber() : "N/A")
                        .invoiceDate(i.getInvoiceDate() != null ? i.getInvoiceDate() : LocalDate.now())
                        .customerName(i.getCustomer() != null ? i.getCustomer().getName() : "Unknown")
                        .subTotal(i.getSubtotal() != null ? i.getSubtotal() : BigDecimal.ZERO)
                        .taxTotal(i.getTaxTotal() != null ? i.getTaxTotal() : BigDecimal.ZERO)
                        .grandTotal(i.getGrandTotal() != null ? i.getGrandTotal() : BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());

        return SalesReportDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .customerId(customerId)
                .totalRevenue(totalRevenue)
                .invoiceCount(lines.size())
                .ourCompany(getOurCompanyInfo())
                .lines(lines)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PurchasesReportDto generatePurchasesReport(LocalDate startDate, LocalDate endDate, Long supplierId) {
        List<PurchaseOrder> pos = purchaseOrderRepository.findForPurchasesReport(supplierId, startDate, endDate);

        List<PurchaseOrder> validPos = pos.stream()
                .filter(po -> po.getStatus() != null && !po.getStatus().name().equals("CANCELLED"))
                .collect(Collectors.toList());

        BigDecimal totalPurchases = validPos.stream()
                .map(po -> po.getLines() != null ? po.getLines().stream()
                        .map(l -> l.getLineTotal() != null ? l.getLineTotal() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PurchasesReportDto.PurchaseLineDto> lines = validPos.stream()
                .map(po -> PurchasesReportDto.PurchaseLineDto.builder()
                        .poNumber(po.getPoNumber() != null ? po.getPoNumber() : "N/A")
                        .orderDate(po.getOrderDate() != null ? po.getOrderDate() : LocalDate.now())
                        .supplierName(po.getSupplier() != null ? po.getSupplier().getName() : "Unknown")
                        .totalAmount(po.getLines() != null ? po.getLines().stream()
                                .map(l -> l.getLineTotal() != null ? l.getLineTotal() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO)
                        .status(po.getStatus() != null ? po.getStatus().name() : "UNKNOWN")
                        .build())
                .collect(Collectors.toList());

        return PurchasesReportDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .supplierId(supplierId)
                .totalPurchases(totalPurchases)
                .poCount(lines.size())
                .ourCompany(getOurCompanyInfo())
                .lines(lines)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryValuationReportDto generateInventoryValuationReport(Long categoryId) {
        List<Product> products = productRepository.findAll();
        if (categoryId != null) {
            products = products.stream()
                    .filter(p -> p.getCategory() != null && p.getCategory().getId().equals(categoryId))
                    .collect(Collectors.toList());
        }

        BigDecimal totalValuation = BigDecimal.ZERO;
        List<InventoryValuationReportDto.ValuationLineDto> lines = new ArrayList<>();

        for (Product p : products) {
            if (p.getStatus() == ProductStatus.ACTIVE && p.isIncludeInFinancialCalculations()) {
                BigDecimal stock = p.getStockOnHand() != null ? p.getStockOnHand() : BigDecimal.ZERO;
                BigDecimal cost = p.getCostPrice() != null ? p.getCostPrice() : BigDecimal.ZERO;
                BigDecimal val = stock.multiply(cost);

                lines.add(InventoryValuationReportDto.ValuationLineDto.builder()
                        .sku(p.getSku())
                        .name(p.getName())
                        .category(p.getCategory() != null ? p.getCategory().getName() : "Uncategorized")
                        .stockOnHand(stock)
                        .costPrice(cost)
                        .totalValue(val)
                        .build());

                totalValuation = totalValuation.add(val);
            }
        }

        return InventoryValuationReportDto.builder()
                .totalCatalogValuation(totalValuation)
                .activeProductsCount(lines.size())
                .generatedAt(LocalDateTime.now())
                .ourCompany(getOurCompanyInfo())
                .lines(lines)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StatementReportDto generateCustomerStatement(Long customerId, LocalDate startDate, LocalDate endDate) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID is required to generate Customer Statement");
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(30);

        List<Invoice> invoices = invoiceRepository.findByCustomerIdOrderByInvoiceDateDesc(customerId);
        List<FundTransaction> fundTxs = fundTransactionRepository
                .findByReferenceDocumentTypeAndReferenceDocumentIdOrderByTransactionDateDesc("CUSTOMER", customerId);

        BigDecimal initialOpening = customer.getOpeningBalance() != null ? customer.getOpeningBalance() : BigDecimal.ZERO;

        // Pre-period balances (before 'start')
        BigDecimal preBilled = invoices.stream()
                .filter(i -> i.getStatus() != com.businessmanager.backend.billing.enums.InvoiceStatus.CANCELLED && 
                             i.getStatus() != com.businessmanager.backend.billing.enums.InvoiceStatus.VOIDED)
                .filter(i -> i.getInvoiceDate() != null && i.getInvoiceDate().isBefore(start))
                .map(i -> i.getGrandTotal() != null ? i.getGrandTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal prePaid = fundTxs.stream()
                .filter(t -> t.getTransactionDate() != null && t.getTransactionDate().isBefore(start))
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal periodOpeningBalance = initialOpening.add(preBilled).subtract(prePaid);

        List<StatementReportDto.StatementLine> allLines = new ArrayList<>();
        BigDecimal periodBilled = BigDecimal.ZERO;
        BigDecimal periodPaid = BigDecimal.ZERO;

        for (Invoice inv : invoices) {
            if (inv.getStatus() == com.businessmanager.backend.billing.enums.InvoiceStatus.CANCELLED || 
                inv.getStatus() == com.businessmanager.backend.billing.enums.InvoiceStatus.VOIDED) {
                continue;
            }
            if (inv.getInvoiceDate() != null && 
                !inv.getInvoiceDate().isBefore(start) && 
                !inv.getInvoiceDate().isAfter(end)) {

                BigDecimal grandTotal = inv.getGrandTotal() != null ? inv.getGrandTotal() : BigDecimal.ZERO;
                periodBilled = periodBilled.add(grandTotal);

                allLines.add(StatementReportDto.StatementLine.builder()
                        .date(inv.getInvoiceDate())
                        .documentCode(inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : String.valueOf(inv.getId()))
                        .description("Invoiced - Invoice No: " + (inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : inv.getId()))
                        .type("SALE")
                        .billedOrPurchasedAmount(grandTotal)
                        .paidAmount(BigDecimal.ZERO)
                        .runningBalance(BigDecimal.ZERO)
                        .build());
            }
        }

        for (FundTransaction tx : fundTxs) {
            if (tx.getTransactionDate() != null && 
                !tx.getTransactionDate().isBefore(start) && 
                !tx.getTransactionDate().isAfter(end)) {

                BigDecimal amount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
                periodPaid = periodPaid.add(amount);

                String payDesc = "Payment Received";
                if (tx.getDescription() != null && !tx.getDescription().isBlank()) {
                    String desc = tx.getDescription().replace("#", "");
                    payDesc = "Payment Received - " + desc;
                }

                allLines.add(StatementReportDto.StatementLine.builder()
                        .date(tx.getTransactionDate())
                        .documentCode("PAY-" + tx.getId())
                        .description(payDesc)
                        .type("PAYMENT")
                        .billedOrPurchasedAmount(BigDecimal.ZERO)
                        .paidAmount(amount)
                        .runningBalance(BigDecimal.ZERO)
                        .build());
            }
        }

        allLines.sort(Comparator.comparing(StatementReportDto.StatementLine::getDate)
                .thenComparing(StatementReportDto.StatementLine::getDocumentCode));

        BigDecimal currentRunning = periodOpeningBalance;
        List<StatementReportDto.StatementLine> processedLines = new ArrayList<>();
        for (StatementReportDto.StatementLine line : allLines) {
            if ("SALE".equals(line.getType())) {
                currentRunning = currentRunning.add(line.getBilledOrPurchasedAmount());
            } else if ("PAYMENT".equals(line.getType())) {
                currentRunning = currentRunning.subtract(line.getPaidAmount());
            }
            processedLines.add(StatementReportDto.StatementLine.builder()
                    .date(line.getDate())
                    .documentCode(line.getDocumentCode())
                    .description(line.getDescription())
                    .type(line.getType())
                    .billedOrPurchasedAmount(line.getBilledOrPurchasedAmount())
                    .paidAmount(line.getPaidAmount())
                    .runningBalance(currentRunning)
                    .build());
        }

        BigDecimal closingBalance = periodOpeningBalance.add(periodBilled).subtract(periodPaid);

        return StatementReportDto.builder()
                .statementType("CUSTOMER")
                .ourCompany(getOurCompanyInfo())
                .partner(StatementReportDto.PartnerInfo.builder()
                        .id(customer.getId())
                        .code(customer.getCustomerCode())
                        .name(customer.getName())
                        .phone(customer.getPhone())
                        .email(customer.getEmail())
                        .address(customer.getAddress())
                        .taxIdOrBank(customer.getTaxId())
                        .build())
                .startDate(start)
                .endDate(end)
                .generatedAt(LocalDateTime.now())
                .openingBalance(periodOpeningBalance)
                .totalBilledOrPurchased(periodBilled)
                .totalPaidOrSettled(periodPaid)
                .closingBalance(closingBalance)
                .lines(processedLines)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StatementReportDto generateSupplierStatement(Long supplierId, LocalDate startDate, LocalDate endDate) {
        if (supplierId == null) {
            throw new IllegalArgumentException("Supplier ID is required to generate Supplier Statement");
        }
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + supplierId));

        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(30);

        List<PurchaseOrder> pos = purchaseOrderRepository.findBySupplierIdOrderByOrderDateDesc(supplierId);
        List<FundTransaction> fundTxs = fundTransactionRepository
                .findByReferenceDocumentTypeAndReferenceDocumentIdOrderByTransactionDateDesc("SUPPLIER", supplierId);

        BigDecimal initialOpening = supplier.getOpeningBalance() != null ? supplier.getOpeningBalance() : BigDecimal.ZERO;

        // Pre-period balances (before 'start')
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

        List<StatementReportDto.StatementLine> allLines = new ArrayList<>();
        BigDecimal periodPurchased = BigDecimal.ZERO;
        BigDecimal periodPaid = BigDecimal.ZERO;

        for (PurchaseOrder po : pos) {
            if (po.getStatus() == com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus.CANCELLED) {
                continue;
            }
            if (po.getOrderDate() != null && 
                !po.getOrderDate().isBefore(start) && 
                !po.getOrderDate().isAfter(end)) {

                BigDecimal poTotal = po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO;
                periodPurchased = periodPurchased.add(poTotal);

                allLines.add(StatementReportDto.StatementLine.builder()
                        .date(po.getOrderDate())
                        .documentCode(po.getPoNumber())
                        .description("Invoiced - PO No: " + po.getPoNumber())
                        .type("PURCHASE")
                        .billedOrPurchasedAmount(poTotal)
                        .paidAmount(BigDecimal.ZERO)
                        .runningBalance(BigDecimal.ZERO)
                        .build());
            }
        }

        for (FundTransaction tx : fundTxs) {
            if (tx.getTransactionDate() != null && 
                !tx.getTransactionDate().isBefore(start) && 
                !tx.getTransactionDate().isAfter(end)) {

                BigDecimal amount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
                periodPaid = periodPaid.add(amount);

                String payDesc = "Payment Paid";
                if (tx.getDescription() != null && !tx.getDescription().isBlank()) {
                    String desc = tx.getDescription().replace("#", "");
                    payDesc = "Payment Paid - " + desc;
                }

                allLines.add(StatementReportDto.StatementLine.builder()
                        .date(tx.getTransactionDate())
                        .documentCode("SETTLE-" + tx.getId())
                        .description(payDesc)
                        .type("PAYMENT")
                        .billedOrPurchasedAmount(BigDecimal.ZERO)
                        .paidAmount(amount)
                        .runningBalance(BigDecimal.ZERO)
                        .build());
            }
        }

        allLines.sort(Comparator.comparing(StatementReportDto.StatementLine::getDate)
                .thenComparing(StatementReportDto.StatementLine::getDocumentCode));

        BigDecimal currentRunning = periodOpeningBalance;
        List<StatementReportDto.StatementLine> processedLines = new ArrayList<>();
        for (StatementReportDto.StatementLine line : allLines) {
            if ("PURCHASE".equals(line.getType())) {
                currentRunning = currentRunning.add(line.getBilledOrPurchasedAmount());
            } else if ("PAYMENT".equals(line.getType())) {
                currentRunning = currentRunning.subtract(line.getPaidAmount());
            }
            processedLines.add(StatementReportDto.StatementLine.builder()
                    .date(line.getDate())
                    .documentCode(line.getDocumentCode())
                    .description(line.getDescription())
                    .type(line.getType())
                    .billedOrPurchasedAmount(line.getBilledOrPurchasedAmount())
                    .paidAmount(line.getPaidAmount())
                    .runningBalance(currentRunning)
                    .build());
        }

        BigDecimal closingBalance = periodOpeningBalance.add(periodPurchased).subtract(periodPaid);

        return StatementReportDto.builder()
                .statementType("SUPPLIER")
                .ourCompany(getOurCompanyInfo())
                .partner(StatementReportDto.PartnerInfo.builder()
                        .id(supplier.getId())
                        .code(supplier.getSupplierCode())
                        .name(supplier.getName())
                        .phone(supplier.getPhone())
                        .email(supplier.getEmail())
                        .address(supplier.getAddress())
                        .taxIdOrBank(supplier.getBankAccountDetails() != null ? supplier.getBankAccountDetails() : supplier.getTaxId())
                        .build())
                .startDate(start)
                .endDate(end)
                .generatedAt(LocalDateTime.now())
                .openingBalance(periodOpeningBalance)
                .totalBilledOrPurchased(periodPurchased)
                .totalPaidOrSettled(periodPaid)
                .closingBalance(closingBalance)
                .lines(processedLines)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OutstandingSummaryReportDto generateCustomerOutstandingSummary(LocalDate asOfDate) {
        LocalDate asOf = asOfDate != null ? asOfDate : LocalDate.now();
        List<Customer> customers = customerRepository.findAll();
        List<OutstandingSummaryReportDto.OutstandingLine> lines = new ArrayList<>();
        BigDecimal totalReceivable = BigDecimal.ZERO;
        int customersWithDue = 0;

        for (Customer c : customers) {
            List<Invoice> invoices = invoiceRepository.findByCustomerIdOrderByInvoiceDateDesc(c.getId());
            BigDecimal totalBilled = invoices.stream()
                    .filter(i -> i.getStatus() != com.businessmanager.backend.billing.enums.InvoiceStatus.CANCELLED && 
                                 i.getStatus() != com.businessmanager.backend.billing.enums.InvoiceStatus.VOIDED)
                    .filter(i -> i.getInvoiceDate() != null && !i.getInvoiceDate().isAfter(asOf))
                    .map(i -> i.getGrandTotal() != null ? i.getGrandTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<FundTransaction> fundTxs = fundTransactionRepository
                    .findByReferenceDocumentTypeAndReferenceDocumentIdOrderByTransactionDateDesc("CUSTOMER", c.getId());

            BigDecimal totalPaid = fundTxs.stream()
                    .filter(tx -> tx.getTransactionDate() != null && !tx.getTransactionDate().isAfter(asOf))
                    .map(tx -> tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal opening = c.getOpeningBalance() != null ? c.getOpeningBalance() : BigDecimal.ZERO;
            BigDecimal balance = opening.add(totalBilled).subtract(totalPaid);

            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                totalReceivable = totalReceivable.add(balance);
                customersWithDue++;
            }

            lines.add(OutstandingSummaryReportDto.OutstandingLine.builder()
                    .partnerId(c.getId())
                    .partnerCode(c.getCustomerCode())
                    .partnerName(c.getName())
                    .phone(c.getPhone())
                    .email(c.getEmail())
                    .totalBilledOrPurchased(totalBilled)
                    .totalPaid(totalPaid)
                    .outstandingBalance(balance)
                    .status(c.getStatus() != null ? c.getStatus().name() : "ACTIVE")
                    .build());
        }

        return OutstandingSummaryReportDto.builder()
                .reportType("CUSTOMER_OUTSTANDING")
                .ourCompany(getOurCompanyInfoForSummary())
                .asOfDate(asOf)
                .generatedAt(LocalDateTime.now())
                .totalOutstandingAmount(totalReceivable)
                .partnersWithDueCount(customersWithDue)
                .totalPartnersCount(customers.size())
                .lines(lines)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OutstandingSummaryReportDto generateSupplierOutstandingSummary(LocalDate asOfDate) {
        LocalDate asOf = asOfDate != null ? asOfDate : LocalDate.now();
        List<Supplier> suppliers = supplierRepository.findAll();
        List<OutstandingSummaryReportDto.OutstandingLine> lines = new ArrayList<>();
        BigDecimal totalPayable = BigDecimal.ZERO;
        int suppliersWithDue = 0;

        for (Supplier s : suppliers) {
            List<PurchaseOrder> pos = purchaseOrderRepository.findBySupplierIdOrderByOrderDateDesc(s.getId());
            BigDecimal totalPurchased = pos.stream()
                    .filter(po -> po.getStatus() == com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus.RECEIVED || 
                                  po.getStatus() == com.businessmanager.backend.purchasing.enums.PurchaseOrderStatus.PARTIALLY_RECEIVED)
                    .filter(po -> po.getOrderDate() != null && !po.getOrderDate().isAfter(asOf))
                    .map(po -> po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<FundTransaction> fundTxs = fundTransactionRepository
                    .findByReferenceDocumentTypeAndReferenceDocumentIdOrderByTransactionDateDesc("SUPPLIER", s.getId());

            BigDecimal totalPaid = fundTxs.stream()
                    .filter(tx -> tx.getTransactionDate() != null && !tx.getTransactionDate().isAfter(asOf))
                    .map(tx -> tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal opening = s.getOpeningBalance() != null ? s.getOpeningBalance() : BigDecimal.ZERO;
            BigDecimal balance = opening.add(totalPurchased).subtract(totalPaid);

            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                totalPayable = totalPayable.add(balance);
                suppliersWithDue++;
            }

            lines.add(OutstandingSummaryReportDto.OutstandingLine.builder()
                    .partnerId(s.getId())
                    .partnerCode(s.getSupplierCode())
                    .partnerName(s.getName())
                    .phone(s.getPhone())
                    .email(s.getEmail())
                    .totalBilledOrPurchased(totalPurchased)
                    .totalPaid(totalPaid)
                    .outstandingBalance(balance)
                    .status(s.getStatus() != null ? s.getStatus().name() : "ACTIVE")
                    .build());
        }

        return OutstandingSummaryReportDto.builder()
                .reportType("SUPPLIER_OUTSTANDING")
                .ourCompany(getOurCompanyInfoForSummary())
                .asOfDate(asOf)
                .generatedAt(LocalDateTime.now())
                .totalOutstandingAmount(totalPayable)
                .partnersWithDueCount(suppliersWithDue)
                .totalPartnersCount(suppliers.size())
                .lines(lines)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RunningSheetReportDto generateRunningSheetReport(LocalDate startDate, LocalDate endDate, Long employeeId) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(30);

        RunningSheetReportDto.EmployeeInfo empInfo = null;
        BigDecimal openingWorked = BigDecimal.ZERO;
        BigDecimal openingSettled = BigDecimal.ZERO;

        if (employeeId != null) {
            var emp = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));
            empInfo = RunningSheetReportDto.EmployeeInfo.builder()
                    .id(emp.getId())
                    .code(emp.getEmployeeCode())
                    .name(emp.getName())
                    .department(emp.getDepartment())
                    .roleTitle(emp.getRoleTitle())
                    .phone(emp.getContactDetails())
                    .build();

            openingWorked = employeeWorkEntryRepository.sumWorkedAmountByEmployeeIdBeforeDate(employeeId, start);
            openingSettled = employeeSettlementRepository.sumSettledAmountByEmployeeIdBeforeDate(employeeId, start);
        }

        BigDecimal openingBalance = openingWorked.subtract(openingSettled);

        // Fetch entries
        var workEntries = employeeWorkEntryRepository.findWorkEntriesForReport(employeeId, start, end);
        var settlements = employeeSettlementRepository.findSettlementsForReport(employeeId, start, end);

        // Combined item helper for sorting
        class SheetItem {
            LocalDate date;
            Long id;
            String type; // WORK or SETTLEMENT
            com.businessmanager.backend.employee.entity.EmployeeWorkEntry workEntry;
            com.businessmanager.backend.employee.entity.EmployeeSettlement settlement;

            SheetItem(LocalDate date, Long id, String type, com.businessmanager.backend.employee.entity.EmployeeWorkEntry w, com.businessmanager.backend.employee.entity.EmployeeSettlement s) {
                this.date = date;
                this.id = id;
                this.type = type;
                this.workEntry = w;
                this.settlement = s;
            }
        }

        List<SheetItem> items = new ArrayList<>();
        for (var w : workEntries) {
            items.add(new SheetItem(w.getDate(), w.getId(), "WORK", w, null));
        }
        for (var s : settlements) {
            items.add(new SheetItem(s.getSettlementDate(), s.getId(), "SETTLEMENT", null, s));
        }

        items.sort(Comparator.comparing((SheetItem i) -> i.date).thenComparing(i -> i.id));

        List<RunningSheetReportDto.RunningSheetLine> lines = new ArrayList<>();
        BigDecimal currentRunningBalance = openingBalance;
        BigDecimal totalWorked = BigDecimal.ZERO;
        BigDecimal totalSettled = BigDecimal.ZERO;

        for (var item : items) {
            if ("WORK".equals(item.type)) {
                var w = item.workEntry;
                BigDecimal amount = w.getWorkedAmount() != null ? w.getWorkedAmount() : BigDecimal.ZERO;
                totalWorked = totalWorked.add(amount);
                currentRunningBalance = currentRunningBalance.add(amount);

                lines.add(RunningSheetReportDto.RunningSheetLine.builder()
                        .id(w.getId())
                        .date(w.getDate())
                        .employeeId(w.getEmployee().getId())
                        .employeeCode(w.getEmployee().getEmployeeCode())
                        .employeeName(w.getEmployee().getName())
                        .entryType("WORK")
                        .description(w.getDescription() != null ? w.getDescription() : "Daily Worked Amount")
                        .workedAmount(amount)
                        .settledAmount(BigDecimal.ZERO)
                        .runningBalance(currentRunningBalance)
                        .build());
            } else {
                var s = item.settlement;
                BigDecimal amount = s.getAmount() != null ? s.getAmount() : BigDecimal.ZERO;
                totalSettled = totalSettled.add(amount);
                currentRunningBalance = currentRunningBalance.subtract(amount);

                lines.add(RunningSheetReportDto.RunningSheetLine.builder()
                        .id(s.getId())
                        .date(s.getSettlementDate())
                        .employeeId(s.getEmployee().getId())
                        .employeeCode(s.getEmployee().getEmployeeCode())
                        .employeeName(s.getEmployee().getName())
                        .entryType("SETTLEMENT")
                        .description(s.getNotes() != null ? s.getNotes() : "Money Settlement")
                        .paymentMode(s.getPaymentMode())
                        .referenceNo(s.getReferenceNo())
                        .workedAmount(BigDecimal.ZERO)
                        .settledAmount(amount)
                        .runningBalance(currentRunningBalance)
                        .build());
            }
        }

        StatementReportDto.CompanyInfo comp = getOurCompanyInfo();
        RunningSheetReportDto.CompanyInfo companyInfo = RunningSheetReportDto.CompanyInfo.builder()
                .name(comp.getName())
                .address(comp.getAddress())
                .phone(comp.getPhone())
                .email(comp.getEmail())
                .taxId(comp.getTaxId())
                .build();

        return RunningSheetReportDto.builder()
                .ourCompany(companyInfo)
                .employee(empInfo)
                .startDate(start)
                .endDate(end)
                .generatedAt(LocalDateTime.now())
                .openingBalance(openingBalance)
                .totalWorkedAmount(totalWorked)
                .totalSettledAmount(totalSettled)
                .closingBalance(currentRunningBalance)
                .lines(lines)
                .build();
    }
}
