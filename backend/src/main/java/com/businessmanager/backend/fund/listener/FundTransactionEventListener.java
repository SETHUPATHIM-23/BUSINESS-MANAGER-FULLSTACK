package com.businessmanager.backend.fund.listener;

import com.businessmanager.backend.common.event.CustomerPaymentPostedEvent;
import com.businessmanager.backend.common.event.SalaryDisbursementPostedEvent;
import com.businessmanager.backend.common.event.SupplierPaymentPostedEvent;
import com.businessmanager.backend.fund.service.FundTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FundTransactionEventListener {

    private final FundTransactionService fundTransactionService;

    @EventListener
    public void handleCustomerPaymentPosted(CustomerPaymentPostedEvent event) {
        fundTransactionService.recordCustomerPayment(
                event.getFundAccountId(),
                event.getInvoiceId(),
                event.getAmount(),
                event.getDate()
        );
    }

    @EventListener
    public void handleSupplierPaymentPosted(SupplierPaymentPostedEvent event) {
        fundTransactionService.recordSupplierPayment(
                event.getFundAccountId(),
                event.getPurchaseOrderId(),
                event.getAmount(),
                event.getDate()
        );
    }

    @EventListener
    public void handleSalaryDisbursementPosted(SalaryDisbursementPostedEvent event) {
        fundTransactionService.recordPayrollDisbursement(
                event.getFundAccountId(),
                event.getEmployeeId(),
                event.getAmount(),
                event.getDate()
        );
    }
}
