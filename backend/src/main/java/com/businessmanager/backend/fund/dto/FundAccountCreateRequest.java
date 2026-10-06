package com.businessmanager.backend.fund.dto;

import com.businessmanager.backend.fund.entity.FundAccountType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundAccountCreateRequest {

    @NotBlank(message = "Fund account name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Account type is required (CASH or BANK)")
    private FundAccountType type;

    @Size(max = 50, message = "Account number must not exceed 50 characters")
    private String accountNumber;

    private Long glAccountId;

    private Long locationId;

    @AssertTrue(message = "Bank accounts must have an account number")
    public boolean isBankRequiresAccountNumber() {
        if (type == FundAccountType.BANK) {
            return accountNumber != null && !accountNumber.trim().isEmpty();
        }
        return true;
    }
}
