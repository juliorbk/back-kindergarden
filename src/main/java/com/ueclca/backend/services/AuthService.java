package com.ueclca.backend.services;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.ueclca.backend.dtos.LoginRequest;
import com.ueclca.backend.dtos.LoginResponse;
import com.ueclca.backend.dtos.UserResponse;
import com.ueclca.backend.models.User;
import com.ueclca.backend.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userService.getByEmail(request.email());
        String token = jwtService.issueToken(user);

        return new LoginResponse(token, "Bearer", jwtService.expirationSeconds(), UserResponse.from(user));
    }
}
