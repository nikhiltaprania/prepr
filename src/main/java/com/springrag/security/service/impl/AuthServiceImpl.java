package com.springrag.security.service.impl;

import com.springrag.common.exception.DuplicateResourceException;
import com.springrag.security.dto.AuthResponse;
import com.springrag.security.dto.LoginRequest;
import com.springrag.security.dto.RegisterRequest;
import com.springrag.security.service.AuthService;
import com.springrag.security.service.TokenService;
import com.springrag.user.domain.User;
import com.springrag.user.domain.UserRole;
import com.springrag.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           TokenService tokenService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER);
        user.setTargetRole(request.getTargetRole());
        user.setExperienceYears(request.getExperienceYears());
        User saved = userRepository.save(user);
        log.info("Registered new user. id={}", saved.getId());
        return toAuthResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Throws on bad credentials -> handled by the global exception handler as 401/403.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + request.getEmail()));
        log.info("User logged in. id={}", user.getId());
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = tokenService.issueToken(user);
        return new AuthResponse(token, tokenService.accessTokenTtlSeconds(), user.getId(),
                user.getUsername(), user.getRole().authority());
    }
}
