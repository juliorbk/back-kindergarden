package com.ueclca.backend.exceptions;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, "Conflicto", ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    ProblemDetail handleBadRequest(BadRequestException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Solicitud inválida", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Acceso denegado", "No tienes permisos para realizar esta acción");
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleAuthentication(AuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "No autorizado", "Credenciales inválidas");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new TreeMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Datos inválidos",
                "La solicitud contiene campos inválidos");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Error inesperado", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "Ocurrió un error inesperado");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
