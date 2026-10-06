package com.businessmanager.backend.accounting.dto;

import com.businessmanager.backend.accounting.enums.AccountType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AccountResponseDto {
    private Long id;
    private String code;
    private String name;
    private AccountType type;
    private Long parentId;
    private String parentCode;
    private String parentName;
    private boolean active;
    private BigDecimal debitTotal;
    private BigDecimal creditTotal;
    private BigDecimal netBalance;
}
