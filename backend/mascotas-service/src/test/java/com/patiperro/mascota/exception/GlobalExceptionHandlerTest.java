package com.patiperro.mascota.exception;

import com.patiperro.mascota.service.MascotaFotoStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void maxUploadSizeUsaMensajeDe5Mb() {
        ResponseEntity<Object> response = handler.handleMaxUpload(
                new MaxUploadSizeExceededException(6L * 1024 * 1024));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(MascotaFotoStorageService.MSG_PESO, messageOf(response));
    }

    @Test
    void illegalArgumentFormatoFotoDevuelve400() {
        ResponseEntity<Object> response = handler.handleIllegalArgument(
                new IllegalArgumentException(MascotaFotoStorageService.MSG_FORMATO));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(MascotaFotoStorageService.MSG_FORMATO, messageOf(response));
    }

    @Test
    void illegalArgumentMascotaNoEncontradaDevuelve404() {
        ResponseEntity<Object> response = handler.handleIllegalArgument(
                new IllegalArgumentException("Mascota no encontrada"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void missingFileMultipartUsaMensajeFormato() {
        ResponseEntity<Object> response = handler.handleMissingMultipartPart(
                new MissingServletRequestPartException("file"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(MascotaFotoStorageService.MSG_FORMATO, messageOf(response));
    }

    @SuppressWarnings("unchecked")
    private static String messageOf(ResponseEntity<Object> response) {
        Object body = response.getBody();
        assertInstanceOf(Map.class, body);
        return (String) ((Map<String, Object>) body).get("message");
    }
}
