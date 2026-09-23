package com.ebank.service;

import com.ebank.dto.LoginRequest;
import com.ebank.dto.LoginResponse;
import com.ebank.exception.DuplicateEntryException;
import com.ebank.exception.InvalidOperationException;
import com.ebank.model.Role;
import com.ebank.model.User;
import com.ebank.repository.RoleRepository;
import com.ebank.repository.UserRepository;
import com.ebank.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.auditService = auditService;
    }

    @Transactional
    public LoginResponse authenticateUser(LoginRequest loginRequest) {
        User user = userRepository.findByUserName(loginRequest.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (user.isLocked()) {
            throw new InvalidOperationException("Account is locked due to too many failed attempts. Please contact Administrator.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Reset failed attempts on success
            user.setFailedAttempts(0);
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            String token = tokenProvider.generateToken(authentication);
            String roleName = user.getRole() != null ? user.getRole().getRoleName() : "STAFF";

            auditService.log(user.getUserId(), "USER_LOGIN", "User", user.getUserId(), null, "Successful login");

            return LoginResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .role(roleName)
                    .expiresIn(tokenProvider.getExpirationMs())
                    .build();

        } catch (BadCredentialsException ex) {
            int attempts = user.getFailedAttempts() != null ? user.getFailedAttempts() + 1 : 1;
            user.setFailedAttempts(attempts);
            if (attempts >= 5) {
                user.setStatus("LOCKED");
                auditService.log(user.getUserId(), "ACCOUNT_LOCKED", "User", user.getUserId(), "ACTIVE", "LOCKED");
            }
            userRepository.save(user);
            throw ex;
        }
    }

    @Transactional
    public User registerUser(String username, String rawPassword, String roleName) {
        if (userRepository.existsByUserName(username)) {
            throw new DuplicateEntryException("Username is already taken: " + username);
        }

        Role role = roleRepository.findByRoleName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(roleName).build()));

        User user = User.builder()
                .userName(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .status("ACTIVE")
                .failedAttempts(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User saved = userRepository.save(user);
        auditService.log(saved.getUserId(), "USER_REGISTERED", "User", saved.getUserId(), null, "Created user with role: " + roleName);
        return saved;
    }

    @Transactional
    public void unlockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidOperationException("User not found: " + userId));
        user.setStatus("ACTIVE");
        user.setFailedAttempts(0);
        userRepository.save(user);
        auditService.log(userId, "ACCOUNT_UNLOCKED", "User", userId, "LOCKED", "ACTIVE");
    }
}
