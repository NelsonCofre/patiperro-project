package com.patiperro.paseador.user.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PaseadorUserExceptionHandlerTest {

    private final PaseadorUserExceptionHandler handler = new PaseadorUserExceptionHandler();

    @Test
    void handleIllegalArgument_retorna400ConMensaje() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleIllegalArgument(new IllegalArgumentException("Archivo requerido: cedulaFrontal"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Bad Request", response.getBody().get("error"));
        assertEquals("Archivo requerido: cedulaFrontal", response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void handleMaxUploadSize_retorna413() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleMaxUploadSize(new MaxUploadSizeExceededException(6L * 1024 * 1024));

        assertEquals(413, response.getStatusCode().value());
        assertEquals("Payload Too Large", response.getBody().get("error"));
        assertEquals(
                "El archivo supera el tamaño máximo permitido (5 MB por documento)",
                response.getBody().get("message"));
    }

    @Test
    void handleMissingServletRequestParameter_retorna400() {
        ResponseEntity<Map<String, Object>> response = handler.handleMissingMultipartParam(
                new MissingServletRequestParameterException("cedulaReverso", "MultipartFile"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Parámetro multipart requerido: cedulaReverso", response.getBody().get("message"));
    }

    @Test
    void handleMissingServletRequestPart_retorna400() {
        ResponseEntity<Map<String, Object>> response = handler.handleMissingMultipartPart(
                new MissingServletRequestPartException("cedulaReverso"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Parámetro multipart requerido: cedulaReverso", response.getBody().get("message"));
    }

    @Test
    void handleResponseStatus_conflict409_propagaMensajeNegocio() {
        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(
                new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Verificación en proceso: no puedes subir nuevos documentos"));

        assertEquals(409, response.getStatusCode().value());
        assertEquals("Conflict", response.getBody().get("error"));
        assertEquals(
                "Verificación en proceso: no puedes subir nuevos documentos",
                response.getBody().get("message"));
    }

    @Test
    void handleResponseStatus_unauthorized401() {
        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No hay sesión autenticada"));

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Unauthorized", response.getBody().get("error"));
    }
}
