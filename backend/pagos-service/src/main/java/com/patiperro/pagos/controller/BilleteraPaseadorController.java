package com.patiperro.pagos.controller;

import com.patiperro.pagos.dto.billetera.BilleteraBucketResponse;
import com.patiperro.pagos.dto.billetera.BilleteraResumenPaseadorResponse;
import com.patiperro.pagos.service.BilleteraService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
        Long idUsuario = resolvePaseadorIdOrNull(authentication);
        if (idUsuario == null) {
            return ResponseEntity.status(authentication == null || !authentication.isAuthenticated()
                    ? HttpStatus.UNAUTHORIZED
                    : HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(billeteraService.resumenParaPaseador(idUsuario));
    }

    /**
     * Endpoints de conveniencia para UI: no recalculan, solo filtran el resumen.
     * No aceptan id de paseador por URL para evitar exposición de terceros.
     */
    @GetMapping("/{bucketKey}")
    public ResponseEntity<BilleteraBucketResponse> obtenerMiBilleteraBucket(
            @PathVariable("bucketKey") String bucketKey,
            Authentication authentication) {
        Long idUsuario = resolvePaseadorIdOrNull(authentication);
        if (idUsuario == null) {
            return ResponseEntity.status(authentication == null || !authentication.isAuthenticated()
                    ? HttpStatus.UNAUTHORIZED
                    : HttpStatus.FORBIDDEN).build();
        }

        BilleteraResumenPaseadorResponse resumen = billeteraService.resumenParaPaseador(idUsuario);
        if (bucketKey == null) {
            return ResponseEntity.badRequest().build();
        }
        String key = bucketKey.trim().toLowerCase();
        return switch (key) {
            case "retenido" -> ResponseEntity.ok(resumen.retenido());
            case "verificacion" -> ResponseEntity.ok(resumen.verificacion());
            case "disponible" -> ResponseEntity.ok(resumen.disponible());
            default -> ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        };
    }

    private static Long resolvePaseadorIdOrNull(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        boolean esPaseador = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_PASEADOR"::equals);
        if (!esPaseador) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long l) {
            return l;
        }
        if (principal instanceof Integer i) {
            return i.longValue();
        }
        if (principal instanceof String s) {
            String t = s.trim();
            if (t.isEmpty()) {
                return null;
            }
            try {
                return Long.parseLong(t);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
