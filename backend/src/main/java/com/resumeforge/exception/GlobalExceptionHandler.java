package com.resumeforge.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @org.springframework.web.bind.annotation.ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        List<Map<String, String>> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> {
                    Map<String, String> err = new HashMap<>();
                    err.put("field", error.getField());
                    err.put("message", error.getDefaultMessage());
                    return err;
                })
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse("validation_error", "Campos invalidos.", fieldErrors));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse("not_found", ex.getMessage(), null));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildErrorResponse("forbidden", ex.getMessage(), null));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildErrorResponse("unauthorized", ex.getMessage(), null));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(ValidationException ex) {
        List<Map<String, String>> details = new ArrayList<>();
        if (ex.getDetails() != null) {
            ex.getDetails().forEach((field, message) -> {
                Map<String, String> err = new HashMap<>();
                err.put("field", field);
                err.put("message", message);
                details.add(err);
            });
        }
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(buildErrorResponse("unprocessable_entity", ex.getMessage(), details));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildErrorResponse("conflict", ex.getMessage(), null));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(AiGenerationException.class)
    public ResponseEntity<Map<String, Object>> handleAiGeneration(AiGenerationException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildErrorResponse("service_unavailable", ex.getMessage(), null));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("internal_error", "Erro inesperado.", null));
    }

    private Map<String, Object> buildErrorResponse(String error, String message, List<Map<String, String>> details) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", error);
        body.put("message", message);
        body.put("timestamp", OffsetDateTime.now().toString());
        if (details != null) {
            body.put("details", details);
        }
        return body;
    }
}
