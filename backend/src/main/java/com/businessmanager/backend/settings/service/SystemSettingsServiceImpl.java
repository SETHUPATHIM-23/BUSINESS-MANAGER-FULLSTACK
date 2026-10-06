package com.businessmanager.backend.settings.service;

import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.security.entity.Role;
import com.businessmanager.backend.security.entity.User;
import com.businessmanager.backend.security.enums.UserStatus;
import com.businessmanager.backend.security.repository.RoleRepository;
import com.businessmanager.backend.security.repository.UserRepository;
import com.businessmanager.backend.settings.dto.SystemSetupRequest;
import com.businessmanager.backend.settings.dto.SystemStatusResponse;
import com.businessmanager.backend.settings.entity.SystemSettings;
import com.businessmanager.backend.settings.enums.InstallationState;
import com.businessmanager.backend.settings.repository.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemSettingsServiceImpl implements SystemSettingsService {

    private final SystemSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public SystemStatusResponse getSystemStatus() {
        SystemSettings settings = getOrCreateSettings();
        return mapToStatusResponse(settings);
    }

    @Override
    @Transactional
    public SystemStatusResponse performFirstTimeSetup(SystemSetupRequest request) {
        SystemSettings settings = getOrCreateSettings();

        if (settings.getInstallationState() == InstallationState.READY) {
            throw new BusinessRuleException("System is already initialized. First-time setup is closed.");
        }

        log.info("Executing production first-time initialization for company: {}", request.getCompanyName());

        // 1. Create or update Administrator user securely
        Role adminRole = roleRepository.findByName("ROLE_ADMINISTRATOR")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_ADMINISTRATOR");
                    role.setCreatedBy("system");
                    role.setUpdatedBy("system");
                    return roleRepository.save(role);
                });

        User adminUser = userRepository.findByUsername(request.getAdminUsername())
                .orElseGet(() -> {
                    User u = new User();
                    u.setUsername(request.getAdminUsername());
                    return u;
                });

        adminUser.setPasswordHash(passwordEncoder.encode(request.getAdminPassword()));
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser.setRoles(Set.of(adminRole));
        adminUser.setCreatedBy("first-run-setup");
        adminUser.setUpdatedBy("first-run-setup");
        userRepository.save(adminUser);

        // 2. Configure company details & business settings
        settings.setInstallationState(InstallationState.READY);
        settings.setCompanyName(request.getCompanyName());
        settings.setLegalName(request.getLegalName() != null ? request.getLegalName() : request.getCompanyName());
        settings.setAddress(request.getAddress());
        settings.setCity(request.getCity());
        settings.setState(request.getState());
        settings.setStateCode(request.getStateCode());
        settings.setPincode(request.getPincode());
        settings.setCountry(request.getCountry() != null ? request.getCountry() : "India");
        settings.setMobile(request.getMobile());
        settings.setTelephone(request.getTelephone());
        settings.setEmail(request.getEmail());
        settings.setWebsite(request.getWebsite());
        settings.setGstin(request.getGstin());
        settings.setPan(request.getPan());

        settings.setInvoicePrefix(request.getInvoicePrefix() != null ? request.getInvoicePrefix() : "INV-");
        settings.setInvoiceNextNumber(request.getInvoiceNextNumber() != null ? request.getInvoiceNextNumber() : 1L);
        settings.setCurrencySymbol(request.getCurrencySymbol() != null ? request.getCurrencySymbol() : "₹");
        settings.setCurrencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : "INR");
        settings.setInvoiceTerms(request.getInvoiceTerms());
        settings.setInvoiceFooter(request.getInvoiceFooter());

        SystemSettings saved = settingsRepository.save(settings);
        log.info("Production first-time initialization completed successfully. System status: READY.");

        return mapToStatusResponse(saved);
    }

    @Override
    @Transactional
    public SystemStatusResponse updateCompanySettings(SystemSetupRequest request) {
        SystemSettings settings = getOrCreateSettings();

        if (request.getCompanyName() != null) settings.setCompanyName(request.getCompanyName());
        if (request.getLegalName() != null) settings.setLegalName(request.getLegalName());
        if (request.getAddress() != null) settings.setAddress(request.getAddress());
        if (request.getCity() != null) settings.setCity(request.getCity());
        if (request.getState() != null) settings.setState(request.getState());
        if (request.getStateCode() != null) settings.setStateCode(request.getStateCode());
        if (request.getPincode() != null) settings.setPincode(request.getPincode());
        if (request.getCountry() != null) settings.setCountry(request.getCountry());
        if (request.getMobile() != null) settings.setMobile(request.getMobile());
        if (request.getTelephone() != null) settings.setTelephone(request.getTelephone());
        if (request.getEmail() != null) settings.setEmail(request.getEmail());
        if (request.getWebsite() != null) settings.setWebsite(request.getWebsite());
        if (request.getGstin() != null) settings.setGstin(request.getGstin());
        if (request.getPan() != null) settings.setPan(request.getPan());

        if (request.getInvoicePrefix() != null) settings.setInvoicePrefix(request.getInvoicePrefix());
        if (request.getInvoiceNextNumber() != null) settings.setInvoiceNextNumber(request.getInvoiceNextNumber());
        if (request.getCurrencySymbol() != null) settings.setCurrencySymbol(request.getCurrencySymbol());
        if (request.getCurrencyCode() != null) settings.setCurrencyCode(request.getCurrencyCode());
        if (request.getInvoiceTerms() != null) settings.setInvoiceTerms(request.getInvoiceTerms());
        if (request.getInvoiceFooter() != null) settings.setInvoiceFooter(request.getInvoiceFooter());

        if (request.getVehiclesJson() != null) settings.setVehiclesJson(request.getVehiclesJson());
        if (request.getBankDetailsJson() != null) settings.setBankDetailsJson(request.getBankDetailsJson());

        SystemSettings updated = settingsRepository.save(settings);
        return mapToStatusResponse(updated);
    }

    private SystemSettings getOrCreateSettings() {
        return settingsRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    SystemSettings s = new SystemSettings();
                    s.setInstallationState(InstallationState.INITIALIZED);
                    s.setInvoicePrefix("INV-");
                    s.setInvoiceNextNumber(1L);
                    s.setCurrencySymbol("₹");
                    s.setCurrencyCode("INR");
                    s.setCreatedBy("system");
                    s.setUpdatedBy("system");
                    return settingsRepository.save(s);
                });
    }

    private SystemStatusResponse mapToStatusResponse(SystemSettings settings) {
        return SystemStatusResponse.builder()
                .installationState(settings.getInstallationState())
                .companyName(settings.getCompanyName())
                .legalName(settings.getLegalName())
                .address(settings.getAddress())
                .city(settings.getCity())
                .state(settings.getState())
                .stateCode(settings.getStateCode())
                .pincode(settings.getPincode())
                .country(settings.getCountry())
                .mobile(settings.getMobile())
                .telephone(settings.getTelephone())
                .email(settings.getEmail())
                .website(settings.getWebsite())
                .gstin(settings.getGstin())
                .pan(settings.getPan())
                .invoicePrefix(settings.getInvoicePrefix())
                .invoiceNextNumber(settings.getInvoiceNextNumber())
                .currencySymbol(settings.getCurrencySymbol())
                .currencyCode(settings.getCurrencyCode())
                .invoiceTerms(settings.getInvoiceTerms())
                .invoiceFooter(settings.getInvoiceFooter())
                .vehiclesJson(settings.getVehiclesJson())
                .bankDetailsJson(settings.getBankDetailsJson())
                .build();
    }
}
