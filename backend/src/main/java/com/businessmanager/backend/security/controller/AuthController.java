package com.businessmanager.backend.security.controller;

import com.businessmanager.backend.security.dto.*;
import com.businessmanager.backend.security.entity.Role;
import com.businessmanager.backend.security.entity.User;
import com.businessmanager.backend.security.jwt.JwtTokenProvider;
import com.businessmanager.backend.security.service.CustomUserDetailsService;
import com.businessmanager.backend.security.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final com.businessmanager.backend.security.service.AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
            authService.handleLoginSuccess(loginRequest.getUsername());
        } catch (AuthenticationException e) {
            authService.handleLoginFailure(loginRequest.getUsername());
            throw e;
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String accessToken = tokenProvider.generateAccessToken(userDetails);
        String refreshToken = tokenProvider.generateRefreshToken(userDetails);

        User user = userService.getUserByUsername(userDetails.getUsername());
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
        List<String> permissions = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .distinct()
                .collect(Collectors.toList());

        return ResponseEntity.ok(LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(userDetails.getUsername())
                .roles(roles)
                .permissions(permissions)
                .build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        if (tokenProvider.validateToken(refreshToken)) {
            String username = tokenProvider.getUsernameFromJwt(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            String accessToken = tokenProvider.generateAccessToken(userDetails);
            
            return ResponseEntity.ok(TokenRefreshResponse.builder()
                    .accessToken(accessToken)
                    .build());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
        List<String> permissions = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .distinct()
                .collect(Collectors.toList());

        return ResponseEntity.ok(LoginResponse.builder()
                .username(username)
                .roles(roles)
                .permissions(permissions)
                .build());
    }

    @PutMapping("/profile")
    public ResponseEntity<LoginResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileUpdateRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String currentUsername = authentication.getName();
        User user = userService.getUserByUsername(currentUsername);

        User updatePayload = new User();
        updatePayload.setUsername(request.getUsername());
        updatePayload.setPasswordHash(request.getNewPassword());
        updatePayload.setStatus(user.getStatus());
        updatePayload.setRoles(user.getRoles());

        User updatedUser = userService.updateUser(user.getId(), updatePayload);
        UserDetails userDetails = userDetailsService.loadUserByUsername(updatedUser.getUsername());

        String accessToken = tokenProvider.generateAccessToken(userDetails);
        String refreshToken = tokenProvider.generateRefreshToken(userDetails);

        List<String> roles = updatedUser.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
        List<String> permissions = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .distinct()
                .collect(Collectors.toList());

        return ResponseEntity.ok(LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(updatedUser.getUsername())
                .roles(roles)
                .permissions(permissions)
                .build());
    }
}
