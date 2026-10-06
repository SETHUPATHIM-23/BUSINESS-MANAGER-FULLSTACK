package com.businessmanager.backend.security.service;

public interface AuthService {
    void handleLoginSuccess(String username);
    void handleLoginFailure(String username);
    void validatePasswordPolicy(String password);
}
