package com.ueclca.backend.exceptions;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        logHandled(HttpStatus.NOT_FOUND, ex);
        return problem(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException ex) {
        logHandled(HttpStatus.CONFLICT, ex);
        return problem(HttpStatus.CONFLICT, "Conflicto", ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    ProblemDetail handleBadRequest(BadRequestException ex) {
        logHandled(HttpStatus.BAD_REQUEST, ex);
        return problem(HttpStatus.BAD_REQUEST, "Solicitud inválida", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        logHandled(HttpStatus.FORBIDDEN, ex);
        return problem(HttpStatus.FORBIDDEN, "Acceso denegado", "No tienes permisos para realizar esta acción");
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleAuthentication(AuthenticationException ex) {
        logHandled(HttpStatus.UNAUTHORIZED, ex);
        return problem(HttpStatus.UNAUTHORIZED, "No autorizado", "Credenciales inválidas");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail handleUnreadableBody(Exception ex) {
        logHandled(HttpStatus.BAD_REQUEST, ex);
        return problem(HttpStatus.BAD_REQUEST, "Solicitud inválida", "La solicitud contiene valores inválidos");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new TreeMap<>();
        ex.getConstraintViolations().forEach(violation ->
                errors.put(violation.getPropertyPath().toString(), violation.getMessage()));

        log.warn("Solicitud rechazada (400): restricciones incumplidas {}", errors);
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Datos inválidos",
                "La solicitud contiene valores inválidos");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    ProblemDetail handleRouteNotFound(Exception ex) {
        logHandled(HttpStatus.NOT_FOUND, ex);
        return problem(HttpStatus.NOT_FOUND, "Ruta no encontrada", "El recurso solicitado no existe");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ProblemDetail handleMissingParameter(MissingServletRequestParameterException ex) {
        logHandled(HttpStatus.BAD_REQUEST, ex);
        return problem(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                "Falta el parámetro obligatorio: " + ex.getParameterName());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        logHandled(HttpStatus.METHOD_NOT_ALLOWED, ex);
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido",
                "El método HTTP no está permitido para esta ruta");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ProblemDetail handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        logHandled(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex);
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de contenido no soportado",
                "El tipo de contenido de la solicitud no es compatible");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        logHandled(HttpStatus.PAYLOAD_TOO_LARGE, ex);
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "Archivo demasiado grande",
                "El archivo supera el tamaño máximo permitido");
    }

    @ExceptionHandler(ErrorResponseException.class)
    ProblemDetail handleErrorResponse(ErrorResponseException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        logHandled(status, ex);
        return problem(status, titleFor(status), "La solicitud no pudo procesarse");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        logHandled(HttpStatus.CONFLICT, ex.getMostSpecificCause());
        return problem(HttpStatus.CONFLICT, "Conflicto de datos",
                "La operación entra en conflicto con datos existentes");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new TreeMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        log.warn("Solicitud rechazada (400): campos inválidos {}", errors);
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Datos inválidos",
                "La solicitud contiene campos inválidos");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        logHandled(HttpStatus.INTERNAL_SERVER_ERROR, ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "Ocurrió un error inesperado");
    }

    private void logHandled(HttpStatus status, Throwable ex) {
        if (status.is5xxServerError()) {
            log.error("Error {} al procesar la solicitud: {}", status.value(), ex.getMessage(), ex);
        } else if (status == HttpStatus.NOT_FOUND) {
            log.debug("Solicitud no encontrada: {}", ex.getMessage());
        } else {
            log.warn("Solicitud rechazada ({}): {}: {}", status.value(),
                    ex.getClass().getSimpleName(), ex.getMessage());
        }
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }

    private String titleFor(HttpStatusCode statusCode) {
        return switch (statusCode.value()) {
            case 400 -> "Solicitud inválida";
            case 401 -> "No autorizado";
            case 403 -> "Acceso denegado";
            case 404 -> "Recurso no encontrado";
            case 405 -> "Método no permitido";
            case 409 -> "Conflicto";
            case 415 -> "Tipo de contenido no soportado";
            default -> statusCode.is4xxClientError() ? "Solicitud inválida" : "Error interno";
        };
    }
}
