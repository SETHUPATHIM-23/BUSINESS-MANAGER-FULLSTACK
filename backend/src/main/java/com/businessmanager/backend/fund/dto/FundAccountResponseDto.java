package com.businessmanager.backend.fund.dto;

import com.businessmanager.backend.fund.entity.FundAccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundAccountResponseDto {
    private Long id;
    private String name;
    private FundAccountType type;
    private String accountNumber;
    private BigDecimal currentBalance;
    private Long glAccountId;
    private String glAccountCode;
    private String glAccountName;
    private Boolean active;
    private Long locationId;
    private String locationName;
    private LocalDateTime createdAt;
}
