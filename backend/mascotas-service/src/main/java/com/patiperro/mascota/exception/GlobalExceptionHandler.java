package com.patiperro.mascota.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Solicitud inválida";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", msg));
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(ForbiddenOperationException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "No autorizado";
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", msg));
    }

    // Este método captura los errores de @Valid (como el peso < 0 o fecha futura)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        // Devuelve el mensaje exacto que pusiste en el modelo (ej: "El peso debe ser...")
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }
}