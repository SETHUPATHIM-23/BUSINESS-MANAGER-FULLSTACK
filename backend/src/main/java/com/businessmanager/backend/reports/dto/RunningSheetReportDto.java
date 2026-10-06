package com.businessmanager.backend.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunningSheetReportDto {
    private CompanyInfo ourCompany;
    private EmployeeInfo employee; // null if report is for all employees
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime generatedAt;

    private BigDecimal openingBalance;
    private BigDecimal totalWorkedAmount;
    private BigDecimal totalSettledAmount;
    private BigDecimal closingBalance;

    private List<RunningSheetLine> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanyInfo {
        private String name;
        private String address;
        private String phone;
        private String email;
        private String taxId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeInfo {
        private Long id;
        private String code;
        private String name;
        private String department;
        private String roleTitle;
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RunningSheetLine {
        private Long id;
        private LocalDate date;
        private Long employeeId;
        private String employeeCode;
        private String employeeName;
        private String entryType; // "WORK" or "SETTLEMENT"
        private String description;
        private String paymentMode;
        private String referenceNo;
        private BigDecimal workedAmount;
        private BigDecimal settledAmount;
        private BigDecimal runningBalance;
    }
}
