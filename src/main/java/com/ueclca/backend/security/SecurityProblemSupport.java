package com.ueclca.backend.security;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityProblemSupport implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        log.warn("Acceso no autorizado (401) en {} {}: {}",
                request.getMethod(), request.getRequestURI(), authException.getMessage());
        log.debug("Detalle del fallo de autenticación", authException);
        write(response, request, HttpStatus.UNAUTHORIZED, "No autorizado",
                "Se requiere un token de acceso válido");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        String principal = request.getUserPrincipal() == null
                ? "anónimo"
                : request.getUserPrincipal().getName();
        log.warn("Acceso denegado (403) en {} {} para {}",
                request.getMethod(), request.getRequestURI(), principal);
        log.debug("Detalle del acceso denegado", accessDeniedException);
        write(response, request, HttpStatus.FORBIDDEN, "Acceso denegado",
                "No tienes permisos para realizar esta acción");
    }

    private void write(HttpServletResponse response, HttpServletRequest request, HttpStatus status,
            String title, String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
