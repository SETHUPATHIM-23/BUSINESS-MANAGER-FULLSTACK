package com.businessmanager.backend.accounting.dto;

import com.businessmanager.backend.accounting.enums.AccountType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountSummaryDto {
    private Long id;
    private String code;
    private String name;
    private AccountType type;
    private boolean active;
}
