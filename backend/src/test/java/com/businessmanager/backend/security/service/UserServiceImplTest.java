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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthService authService;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPasswordHash("Password123!");
        user.setStatus(UserStatus.ACTIVE);
    }

    @Test
    void createUser_Success() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed_pass");
        when(userRepository.save(any(User.class))).thenReturn(user);

        User created = userService.createUser(user);

        assertNotNull(created);
        verify(authService).validatePasswordPolicy("Password123!");
        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(user);
    }

    @Test
    void createUser_DuplicateUsername_ThrowsException() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> userService.createUser(user));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_Success() {
        User updated = new User();
        updated.setUsername("testuser_updated");
        updated.setPasswordHash("NewPass123!");
        updated.setStatus(UserStatus.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("testuser_updated")).thenReturn(false);
        when(passwordEncoder.encode("NewPass123!")).thenReturn("hashed_new_pass");
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.updateUser(1L, updated);

        assertNotNull(result);
        assertEquals("testuser_updated", user.getUsername());
        verify(authService).validatePasswordPolicy("NewPass123!");
        verify(passwordEncoder).encode("NewPass123!");
    }

    @Test
    void updateUser_DuplicateUsername_ThrowsException() {
        User updated = new User();
        updated.setUsername("existing_user");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("existing_user")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> userService.updateUser(1L, updated));
        verify(userRepository, never()).save(any());
    }

    @Test
    void deactivateUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        userService.deactivateUser(1L);
        
        assertEquals(UserStatus.INACTIVE, user.getStatus());
        verify(userRepository).save(user);
    }

    @Test
    void deactivateUser_Admin_ThrowsException() {
        user.setUsername("admin");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        assertThrows(BusinessRuleException.class, () -> userService.deactivateUser(1L));
        verify(userRepository, never()).save(any());
    }
}
