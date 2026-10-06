package com.businessmanager.backend.security.service;

import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.security.entity.User;
import com.businessmanager.backend.security.enums.UserStatus;
import com.businessmanager.backend.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_TIME_DURATION_MINUTES = 15;

    @Override
    @Transactional
    public void handleLoginSuccess(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            if (user.getFailedLoginCount() > 0 || user.getStatus() == UserStatus.LOCKED) {
                user.setFailedLoginCount(0);
                user.setStatus(UserStatus.ACTIVE);
                user.setLockedUntil(null);
                userRepository.save(user);
            }
        });
    }

    @Override
    @Transactional
    public void handleLoginFailure(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            if (user.getStatus() != UserStatus.ACTIVE && user.getStatus() != UserStatus.LOCKED) {
                return; // don't increment for INACTIVE users
            }
            
            int newCount = user.getFailedLoginCount() + 1;
            user.setFailedLoginCount(newCount);
            
            if (newCount >= MAX_FAILED_ATTEMPTS) {
                user.setStatus(UserStatus.LOCKED);
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_TIME_DURATION_MINUTES));
            }
            userRepository.save(user);
        });
    }

    @Override
    public void validatePasswordPolicy(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessRuleException("Password must be at least 8 characters long");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new BusinessRuleException("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new BusinessRuleException("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            throw new BusinessRuleException("Password must contain at least one number");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new BusinessRuleException("Password must contain at least one special character");
        }
    }
}
