package com.businessmanager.backend.security.service;

import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.security.entity.User;
import com.businessmanager.backend.security.enums.UserStatus;
import com.businessmanager.backend.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setFailedLoginCount(0);
    }

    @Test
    void handleLoginSuccess_WithFailedAttempts_ResetsCount() {
        testUser.setFailedLoginCount(3);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        authService.handleLoginSuccess("testuser");

        assertEquals(0, testUser.getFailedLoginCount());
        assertEquals(UserStatus.ACTIVE, testUser.getStatus());
        assertNull(testUser.getLockedUntil());
        verify(userRepository).save(testUser);
    }

    @Test
    void handleLoginSuccess_WithZeroFailedAttempts_DoesNothing() {
        testUser.setFailedLoginCount(0);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        authService.handleLoginSuccess("testuser");

        verify(userRepository, never()).save(any());
    }

    @Test
    void handleLoginFailure_IncrementsCount() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        authService.handleLoginFailure("testuser");

        assertEquals(1, testUser.getFailedLoginCount());
        verify(userRepository).save(testUser);
    }

    @Test
    void handleLoginFailure_WhenCountReachesMax_LocksAccount() {
        testUser.setFailedLoginCount(4);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        authService.handleLoginFailure("testuser");

        assertEquals(5, testUser.getFailedLoginCount());
        assertEquals(UserStatus.LOCKED, testUser.getStatus());
        assertNotNull(testUser.getLockedUntil());
        verify(userRepository).save(testUser);
    }

    @Test
    void handleLoginFailure_WhenInactive_DoesNothing() {
        testUser.setStatus(UserStatus.INACTIVE);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        authService.handleLoginFailure("testuser");

        assertEquals(0, testUser.getFailedLoginCount());
        verify(userRepository, never()).save(any());
    }

    @Test
    void validatePasswordPolicy_ValidPassword_DoesNotThrow() {
        assertDoesNotThrow(() -> authService.validatePasswordPolicy("Valid1@Pass"));
    }

    @Test
    void validatePasswordPolicy_TooShort_ThrowsException() {
        assertThrows(BusinessRuleException.class, () -> authService.validatePasswordPolicy("Short1!"));
    }

    @Test
    void validatePasswordPolicy_NoUppercase_ThrowsException() {
        assertThrows(BusinessRuleException.class, () -> authService.validatePasswordPolicy("valid1@pass"));
    }

    @Test
    void validatePasswordPolicy_NoLowercase_ThrowsException() {
        assertThrows(BusinessRuleException.class, () -> authService.validatePasswordPolicy("VALID1@PASS"));
    }

    @Test
    void validatePasswordPolicy_NoNumber_ThrowsException() {
        assertThrows(BusinessRuleException.class, () -> authService.validatePasswordPolicy("Valid@Pass"));
    }

    @Test
    void validatePasswordPolicy_NoSpecialCharacter_ThrowsException() {
        assertThrows(BusinessRuleException.class, () -> authService.validatePasswordPolicy("Valid1Pass"));
    }
}
