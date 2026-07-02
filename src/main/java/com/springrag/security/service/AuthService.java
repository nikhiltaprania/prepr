package com.springrag.security.service;

import com.springrag.security.dto.AuthResponse;
import com.springrag.security.dto.LoginRequest;
import com.springrag.security.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
