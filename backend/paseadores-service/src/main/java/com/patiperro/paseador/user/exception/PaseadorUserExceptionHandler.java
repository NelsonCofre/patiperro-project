package com.patiperro.paseador.user.exception;

import com.patiperro.paseador.user.controller.PaseadorConfiguracionController;
import com.patiperro.paseador.user.controller.PaseadorVerificacionController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {
        PaseadorVerificacionController.class,
        PaseadorConfiguracionController.class
})
public class PaseadorUserExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return errorBody(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return errorBody(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Payload Too Large",
                "El archivo supera el tamaño máximo permitido (5 MB por documento)");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingMultipartParam(MissingServletRequestParameterException ex) {
        return errorBody(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                "Parámetro multipart requerido: " + ex.getParameterName());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return errorBody(status, status.getReasonPhrase(), message);
    }

    private static ResponseEntity<Map<String, Object>> errorBody(
            HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(
                Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", status.value(),
                        "error", error,
                        "message", message
                )
        );
    }
}
