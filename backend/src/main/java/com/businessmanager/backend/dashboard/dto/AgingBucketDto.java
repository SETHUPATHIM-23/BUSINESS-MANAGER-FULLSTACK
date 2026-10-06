package com.businessmanager.backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgingBucketDto {
    private BigDecimal current;
    private BigDecimal days30;
    private BigDecimal days60;
    private BigDecimal days90Plus;
    private BigDecimal total;
    private List<CustomerAgingDetailDto> details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerAgingDetailDto {
        private Long customerId;
        private String customerName;
        private String invoiceNumber;
        private String invoiceDate;
        private String dueDate;
        private BigDecimal outstandingAmount;
        private long ageInDays;
        private String statusCategory;
        private BigDecimal currentAmount;
        private BigDecimal days30Amount;
        private BigDecimal days60Amount;
        private BigDecimal days90PlusAmount;
    }
}
