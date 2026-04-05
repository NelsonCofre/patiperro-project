package com.patiperro.mascota.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice // Esta es la "Vigilancia de Controladores" solicitada //
public class GlobalExceptionHandler {

    // 1. VIGILANCIA DE ARGUMENTOS (Se mantiene tu lógica de mensaje nulo) //
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Solicitud inválida";
        return buildErrorResponse(HttpStatus.BAD_REQUEST, msg, null);
    }

    // 2. VIGILANCIA DE OPERACIONES PROHIBIDAS (Se mantiene tu lógica de "No autorizado") //
    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<Object> handleForbidden(ForbiddenOperationException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "No autorizado";
        return buildErrorResponse(HttpStatus.FORBIDDEN, msg, null);
    }

    // 3. VIGILANCIA DE VALIDACIONES (Mantiene tu captura de errores de @Valid en Mascota) //
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        // Se envía un mensaje general y los detalles específicos del peso/fecha en el campo 'details' //
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Error de validación en los datos de la mascota", errors);
    }

    // 4. VIGILANCIA GLOBAL (Nueva mejora para atrapar cualquier otro error) //
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAll(Exception ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado", null);
    }

    /**
     * MÉTODO DE FACTORIZACIÓN DE RESPUESTAS 
     * Crea el formato estándar que el Frontend de React espera siempre.
     */
    private ResponseEntity<Object> buildErrorResponse(HttpStatus status, String message, Map<String, String> details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message); // Aquí va tu mensaje de "Peso inválido", "Fecha futura", etc. //
        
        if (details != null) {
            body.put("details", details); // Aquí se listan todos los errores del formulario //
        }
        
        return new ResponseEntity<>(body, status);
    }
}