package com.ueclca.backend.services;

import com.ueclca.backend.dtos.LoginRequest;
import com.ueclca.backend.dtos.LoginResponse;
import com.ueclca.backend.dtos.UserResponse;
import com.ueclca.backend.models.User;
import com.ueclca.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    public AuthService(
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        UserService userService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (AuthenticationException ex) {
            log.warn("Inicio de sesión rechazado para {}: {}", request.email(),
                ex.getClass().getSimpleName());
            throw ex;
        }

        User user = userService.getByEmail(request.email());
        String token = jwtService.issueToken(user);

        log.info("Inicio de sesión correcto para {} ({})", user.getEmail(), user.getRole());
        return new LoginResponse(
            token,
            "Bearer",
            jwtService.expirationSeconds(),
            UserResponse.from(user)
        );
    }
}
