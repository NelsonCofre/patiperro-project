package com.patiperro.pagos.controller;

import com.patiperro.pagos.dto.comprobante.ComprobantePagoResponse;
import com.patiperro.pagos.service.ComprobantePagoService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/pagos/comprobante")
public class ComprobantePagoController {

    private final ComprobantePagoService comprobantePagoService;

    public ComprobantePagoController(ComprobantePagoService comprobantePagoService) {
        this.comprobantePagoService = comprobantePagoService;
    }

    /**
     * Resumen estructurado (JSON). Sirve snapshot persistido si existe y el pago sigue aprobado.
     */
    @GetMapping("/{idReserva}")
    public ResponseEntity<ComprobantePagoResponse> obtener(
            @PathVariable("idReserva") Long idReserva,
            Authentication authentication) {
        return ResponseEntity.ok(comprobantePagoService.obtenerParaTutor(idReserva, authentication));
    }

    /**
     * Descarga legible del mismo comprobante (HTML). Misma autenticación anti-IDOR que el JSON.
     */
    @GetMapping(value = "/{idReserva}/html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> obtenerHtml(
            @PathVariable("idReserva") Long idReserva,
            Authentication authentication) {
        String html = comprobantePagoService.obtenerHtmlParaTutor(idReserva, authentication);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("comprobante-reserva-" + idReserva + ".html", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}

