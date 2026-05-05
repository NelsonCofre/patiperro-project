package com.patiperro.pagos.controller;

import com.patiperro.pagos.dto.billetera.BilleteraResumenPaseadorResponse;
import com.patiperro.pagos.service.BilleteraService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagos/paseador/billetera")
public class BilleteraPaseadorController {

    private final BilleteraService billeteraService;

    public BilleteraPaseadorController(BilleteraService billeteraService) {
        this.billeteraService = billeteraService;
    }

    @GetMapping
    public ResponseEntity<BilleteraResumenPaseadorResponse> obtenerMiBilletera(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        boolean esPaseador = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_PASEADOR"::equals);
        if (!esPaseador) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Long idUsuario)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(billeteraService.resumenParaPaseador(idUsuario));
    }
}
