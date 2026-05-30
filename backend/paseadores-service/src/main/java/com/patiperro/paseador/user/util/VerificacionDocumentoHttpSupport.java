package com.patiperro.paseador.user.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Cabeceras comunes para respuestas de documentos de identidad (PII).
 * No expone el nombre interno del archivo en disco.
 */
public final class VerificacionDocumentoHttpSupport {

    private VerificacionDocumentoHttpSupport() {
    }

    public static ResponseEntity.BodyBuilder okDocumento(String contentType) {
        String resolved = contentType != null && !contentType.isBlank()
                ? contentType
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String disposition = resolved.toLowerCase().startsWith("image/")
                ? "inline"
                : "attachment";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, resolved)
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header("X-Content-Type-Options", "nosniff")
                .header("X-Frame-Options", "DENY")
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition);
    }
}
