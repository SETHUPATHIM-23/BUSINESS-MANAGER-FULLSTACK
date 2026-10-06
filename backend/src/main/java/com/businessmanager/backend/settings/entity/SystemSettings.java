package com.businessmanager.backend.settings.entity;

import com.businessmanager.backend.common.entity.BaseEntity;
import com.businessmanager.backend.settings.enums.InstallationState;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettings extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "installation_state", nullable = false)
    private InstallationState installationState;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "state_code")
    private String stateCode;

    @Column(name = "pincode")
    private String pincode;

    @Column(name = "country")
    private String country;

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "telephone")
    private String telephone;

    @Column(name = "email")
    private String email;

    @Column(name = "website")
    private String website;

    @Column(name = "gstin")
    private String gstin;

    @Column(name = "pan")
    private String pan;

    @Column(name = "invoice_prefix")
    private String invoicePrefix;

    @Column(name = "invoice_next_number")
    private Long invoiceNextNumber;

    @Column(name = "currency_symbol")
    private String currencySymbol;

    @Column(name = "currency_code")
    private String currencyCode;

    @Column(name = "invoice_terms", columnDefinition = "TEXT")
    private String invoiceTerms;

    @Column(name = "invoice_footer", columnDefinition = "TEXT")
    private String invoiceFooter;

    @Column(name = "vehicles_json", columnDefinition = "TEXT")
    private String vehiclesJson;

    @Column(name = "bank_details_json", columnDefinition = "TEXT")
    private String bankDetailsJson;
}
