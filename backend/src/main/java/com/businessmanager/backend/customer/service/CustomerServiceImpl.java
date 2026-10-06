package com.businessmanager.backend.customer.service;

import com.businessmanager.backend.billing.entity.Invoice;
import com.businessmanager.backend.billing.enums.InvoiceStatus;
import com.businessmanager.backend.billing.repository.InvoiceRepository;
import com.businessmanager.backend.common.audit.annotation.AuditAction;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.customer.dto.CustomerCreateDto;
import com.businessmanager.backend.customer.dto.CustomerResponseDto;
import com.businessmanager.backend.customer.dto.CustomerStatementResponse;
import com.businessmanager.backend.customer.dto.CustomerSummaryDto;
import com.businessmanager.backend.customer.dto.CustomerUpdateDto;
import com.businessmanager.backend.customer.entity.Customer;
import com.businessmanager.backend.customer.enums.CustomerStatus;
import com.businessmanager.backend.customer.mapper.CustomerMapper;
import com.businessmanager.backend.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;

import com.businessmanager.backend.fund.entity.FundTransaction;
import com.businessmanager.backend.fund.repository.FundTransactionRepository;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final InvoiceRepository invoiceRepository;
    private final FundTransactionRepository fundTransactionRepository;

    @Override
    @Transactional
    @AuditAction(action = "CREATE", module = "CUSTOMER")
    public CustomerResponseDto createCustomer(CustomerCreateDto dto) {
        // Enforce unique customer_code
        if (customerRepository.findByCustomerCode(dto.getCustomerCode()).isPresent()) {
            throw new BusinessRuleException("Customer code already exists: " + dto.getCustomerCode());
        }

        Customer customer = customerMapper.toEntity(dto);
        customer.setStatus(CustomerStatus.ACTIVE);
        
        Customer savedCustomer = customerRepository.save(customer);
        return customerMapper.toDto(savedCustomer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));
        return customerMapper.toDto(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomerByCode(String code) {
        Customer customer = customerRepository.findByCustomerCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with code: " + code));
        return customerMapper.toDto(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerSummaryDto> searchCustomers(String name, String code, String phone, CustomerStatus status, Pageable pageable) {
        Page<Customer> customers = customerRepository.searchCustomers(name, code, phone, status, pageable);
        return customers.map(customerMapper::toSummaryDto);
    }

    @Override
    @Transactional
    @AuditAction(action = "UPDATE", module = "CUSTOMER")
    public CustomerResponseDto updateCustomer(Long id, CustomerUpdateDto dto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));

        customerMapper.updateEntityFromDto(dto, customer);
        Customer savedCustomer = customerRepository.save(customer);
        return customerMapper.toDto(savedCustomer);
    }

    @Override
    @Transactional
    @AuditAction(action = "DEACTIVATE", module = "CUSTOMER")
    public void deactivateCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));
        
        customer.setStatus(CustomerStatus.INACTIVE);
        customerRepository.save(customer);
    }

    @Override
    @Transactional
    @AuditAction(action = "DELETE", module = "CUSTOMER")
    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));

        // Enforce CUST-080: prevent deletion of a customer with existing transaction history
        if (hasTransactionHistory(customer)) {
            throw new BusinessRuleException("Customer has transaction history and cannot be deleted. Deactivate instead.");
        }

        customerRepository.delete(customer);
    }

    /**
     * Helper to verify if the customer has existing transactions (invoices).
     */
    private boolean hasTransactionHistory(Customer customer) {
        long invoiceCount = invoiceRepository.countByCustomerIdAndStatusIn(
                customer.getId(),
                List.of(InvoiceStatus.DRAFT, InvoiceStatus.POSTED, InvoiceStatus.PARTIALLY_PAID, InvoiceStatus.PAID, InvoiceStatus.CANCELLED, InvoiceStatus.VOIDED)
        );
        return invoiceCount > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public void validateCreditLimit(Long customerId, BigDecimal invoiceAmount) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        // Rule CUST-060: Block any on-credit invoice if creditHold is true
        if (customer.isCreditHold()) {
            throw new BusinessRuleException("Cannot post on-credit invoice: Customer '" + customer.getName() + "' is on credit hold.");
        }

        // Rule CUST-030: Block on-credit invoice if it exceeds creditLimit
        if (customer.getCreditLimit() != null) {
            BigDecimal runningBalance = getRunningBalance(customer);
            BigDecimal projectedBalance = runningBalance.add(invoiceAmount);
            if (projectedBalance.compareTo(customer.getCreditLimit()) > 0) {
                throw new BusinessRuleException("Cannot post on-credit invoice: Amount " + invoiceAmount + 
                        " would push running balance (" + runningBalance + ") to " + projectedBalance + 
                        ", exceeding the credit limit of " + customer.getCreditLimit() + ".");
            }
        }
    }

    private BigDecimal getRunningBalance(Customer customer) {
        BigDecimal outstanding = invoiceRepository.calculateOutstandingBalanceByCustomerId(customer.getId());
        return customer.getOpeningBalance().add(outstanding);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerStatementResponse getStatement(
            Long customerId, 
            LocalDate startDate, 
            LocalDate endDate
    ) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(30);

        List<Invoice> invoices = invoiceRepository.findByCustomerIdOrderByInvoiceDateDesc(customerId);
        List<FundTransaction> fundTxs = fundTransactionRepository
                .findByReferenceDocumentTypeAndReferenceDocumentIdOrderByTransactionDateDesc("CUSTOMER", customerId);

        BigDecimal initialOpening = customer.getOpeningBalance() != null ? customer.getOpeningBalance() : BigDecimal.ZERO;

        // Calculate opening balance as of 'start' (initial opening + pre-period invoices - pre-period payments)
        BigDecimal preBilled = invoices.stream()
                .filter(i -> i.getStatus() != InvoiceStatus.CANCELLED && i.getStatus() != InvoiceStatus.VOIDED)
                .filter(i -> i.getInvoiceDate() != null && i.getInvoiceDate().isBefore(start))
                .map(i -> i.getGrandTotal() != null ? i.getGrandTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal prePaid = fundTxs.stream()
                .filter(t -> t.getTransactionDate() != null && t.getTransactionDate().isBefore(start))
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal periodOpeningBalance = initialOpening.add(preBilled).subtract(prePaid);

        List<CustomerStatementResponse.StatementEntry> entries = new ArrayList<>();
        BigDecimal totalBilledInPeriod = BigDecimal.ZERO;
        BigDecimal totalPaidInPeriod = BigDecimal.ZERO;

        for (Invoice invoice : invoices) {
            if (invoice.getInvoiceDate() != null && 
                !invoice.getInvoiceDate().isBefore(start) && 
                !invoice.getInvoiceDate().isAfter(end)) {

                if (invoice.getStatus() != InvoiceStatus.CANCELLED && invoice.getStatus() != InvoiceStatus.VOIDED) {
                    BigDecimal grandTotal = invoice.getGrandTotal() != null ? invoice.getGrandTotal() : BigDecimal.ZERO;
                    totalBilledInPeriod = totalBilledInPeriod.add(grandTotal);

                    entries.add(CustomerStatementResponse.StatementEntry.builder()
                            .date(invoice.getInvoiceDate())
                            .documentCode(invoice.getInvoiceNumber() != null && !invoice.getInvoiceNumber().isBlank() ? invoice.getInvoiceNumber() : String.valueOf(invoice.getId()))
                            .description("Invoiced - Invoice No: " + (invoice.getInvoiceNumber() != null && !invoice.getInvoiceNumber().isBlank() ? invoice.getInvoiceNumber() : invoice.getId()))
                            .type("INVOICE")
                            .amount(grandTotal)
                            .build());
                }
            }
        }

        for (FundTransaction tx : fundTxs) {
            if (tx.getTransactionDate() != null && 
                !tx.getTransactionDate().isBefore(start) && 
                !tx.getTransactionDate().isAfter(end)) {

                BigDecimal amount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
                totalPaidInPeriod = totalPaidInPeriod.add(amount);

                String payDesc = "Payment Received";
                if (tx.getDescription() != null && !tx.getDescription().isBlank()) {
                    String desc = tx.getDescription().replace("#", "");
                    if (desc.toLowerCase().contains("invoice")) {
                        payDesc = "Payment Received - Against " + desc;
                    } else {
                        payDesc = "Payment Received - " + desc;
                    }
                }

                entries.add(CustomerStatementResponse.StatementEntry.builder()
                        .date(tx.getTransactionDate())
                        .documentCode("PAY-" + tx.getId())
                        .description(payDesc)
                        .type("PAYMENT")
                        .amount(amount.negate())
                        .build());
            }
        }

        entries.sort((a, b) -> {
            int comp = a.getDate().compareTo(b.getDate());
            return comp != 0 ? comp : a.getDocumentCode().compareTo(b.getDocumentCode());
        });

        BigDecimal closingBalance = periodOpeningBalance.add(totalBilledInPeriod).subtract(totalPaidInPeriod);

        return CustomerStatementResponse.builder()
                .customerCode(customer.getCustomerCode())
                .customerName(customer.getName())
                .openingBalance(periodOpeningBalance)
                .closingBalance(closingBalance)
                .startDate(start)
                .endDate(end)
                .entries(entries)
                .totalUnpaidInvoices(totalBilledInPeriod)
                .totalUnappliedCredits(totalPaidInPeriod)
                .reconciledBalance(closingBalance)
                .build();
    }
}
