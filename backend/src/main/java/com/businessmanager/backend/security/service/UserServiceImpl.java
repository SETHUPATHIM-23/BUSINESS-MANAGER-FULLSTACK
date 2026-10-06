package com.businessmanager.backend.security.service;

import com.businessmanager.backend.common.audit.annotation.AuditAction;
import com.businessmanager.backend.common.exception.BusinessRuleException;
import com.businessmanager.backend.common.exception.ResourceNotFoundException;
import com.businessmanager.backend.security.entity.User;
import com.businessmanager.backend.security.enums.UserStatus;
import com.businessmanager.backend.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Override
    @Transactional
    @AuditAction(action = "CREATE_USER", module = "ADMIN")
    public User createUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new BusinessRuleException("Username '" + user.getUsername() + "' is already taken");
        }
        
        authService.validatePasswordPolicy(user.getPasswordHash());
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        return userRepository.save(user);
    }

    @Override
    @Transactional
    @AuditAction(action = "UPDATE_USER", module = "ADMIN")
    public User updateUser(Long id, User updatedUser) {
        User existingUser = getUserById(id);
        
        if (!existingUser.getUsername().equals(updatedUser.getUsername()) && 
            userRepository.existsByUsername(updatedUser.getUsername())) {
            throw new BusinessRuleException("Username '" + updatedUser.getUsername() + "' is already taken");
        }

        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setStatus(updatedUser.getStatus());
        
        if (updatedUser.getPasswordHash() != null && !updatedUser.getPasswordHash().isEmpty()) {
            authService.validatePasswordPolicy(updatedUser.getPasswordHash());
            existingUser.setPasswordHash(passwordEncoder.encode(updatedUser.getPasswordHash()));
        }
        
        existingUser.setRoles(updatedUser.getRoles());
        
        return userRepository.save(existingUser);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> searchUsers(String search, UserStatus status, Pageable pageable) {
        return userRepository.searchUsers(search, status, pageable);
    }

    @Override
    @Transactional
    @AuditAction(action = "DEACTIVATE_USER", module = "ADMIN")
    public void deactivateUser(Long id) {
        User existingUser = getUserById(id);
        if ("admin".equals(existingUser.getUsername()) || "sethu".equals(existingUser.getUsername())) {
            throw new BusinessRuleException("Cannot deactivate the primary admin account");
        }
        existingUser.setStatus(UserStatus.INACTIVE);
        userRepository.save(existingUser);
    }
}
