package com.patiperro.pagos.controller;

import com.patiperro.pagos.dto.comprobante.ComprobantePagoResponse;
import com.patiperro.pagos.service.ComprobantePagoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagos/comprobante")
public class ComprobantePagoController {

    private final ComprobantePagoService comprobantePagoService;

    public ComprobantePagoController(ComprobantePagoService comprobantePagoService) {
        this.comprobantePagoService = comprobantePagoService;
    }

    @GetMapping("/{idReserva}")
    public ResponseEntity<ComprobantePagoResponse> obtener(
            @PathVariable("idReserva") Long idReserva,
            Authentication authentication) {
        return ResponseEntity.ok(comprobantePagoService.obtenerParaTutor(idReserva, authentication));
    }
}

