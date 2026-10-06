package com.businessmanager.backend.accounting.entity;

import com.businessmanager.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "account_mappings")
@Getter
@Setter
public class AccountMapping extends BaseEntity {

    @Column(name = "mapping_key", nullable = false, unique = true, length = 50)
    private String mappingKey;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
}
