package com.patiperro.pagos.controller;

import com.patiperro.pagos.dto.billetera.BilleteraBucketResponse;
import com.patiperro.pagos.dto.billetera.BilleteraResumenPaseadorResponse;
import com.patiperro.pagos.dto.billetera.CatalogoRegistroCuentaResponse;
import com.patiperro.pagos.dto.billetera.CuentaBancariaPaseadorResponse;
import com.patiperro.pagos.dto.billetera.RegistrarCuentaBancariaPaseadorRequest;
import com.patiperro.pagos.dto.billetera.RetiroHistorialItemResponse;
import com.patiperro.pagos.dto.billetera.RetiroPaseadorResponse;
import com.patiperro.pagos.dto.billetera.SolicitarRetiroPaseadorRequest;
import com.patiperro.pagos.service.BilleteraService;
import com.patiperro.pagos.service.RetiroPaseadorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de billetera para el paseador autenticado. El resumen completo va en {@link #obtenerMiBilletera}; los GET por
 * bucket ({@link #obtenerMiBilleteraBucket}) devuelven solo un {@link BilleteraBucketResponse} sin historial ni
 * proyección N+2.
 */
@RestController
@RequestMapping("/api/pagos/paseador/billetera")
@PreAuthorize("hasRole('PASEADOR')")
public class BilleteraPaseadorController {

    private final BilleteraService billeteraService;
    private final RetiroPaseadorService retiroPaseadorService;

    public BilleteraPaseadorController(BilleteraService billeteraService, RetiroPaseadorService retiroPaseadorService) {
        this.billeteraService = billeteraService;
        this.retiroPaseadorService = retiroPaseadorService;
    }

    /**
     * Resumen completo: buckets, historial de liberaciones, proyección N+2 por día y {@code updatedAt}.
     */
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

    @GetMapping("/cuentas-bancarias")
    public ResponseEntity<List<CuentaBancariaPaseadorResponse>> listarCuentasBancarias(Authentication authentication) {
        Long idUsuario = resolvePaseadorIdOrNull(authentication);
        if (idUsuario == null) {
            return ResponseEntity.status(authentication == null || !authentication.isAuthenticated()
                    ? HttpStatus.UNAUTHORIZED
                    : HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(billeteraService.listarCuentasBancariasParaPaseador(idUsuario));
    }

    /**
     * Listas ordenadas de {@code banco} y {@code tipo_cuenta} para el formulario de registro de cuenta.
     */
    @GetMapping("/catalogo/registro-cuenta")
    public ResponseEntity<CatalogoRegistroCuentaResponse> catalogoRegistroCuenta(Authentication authentication) {
        if (resolvePaseadorIdOrNull(authentication) == null) {
            return ResponseEntity.status(authentication == null || !authentication.isAuthenticated()
                    ? HttpStatus.UNAUTHORIZED
                    : HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(billeteraService.catalogoParaRegistroCuentaBancaria());
    }

    /**
     * Registra o actualiza la cuenta bancaria de destino para retiros (una por billetera).
     */
    @PostMapping("/cuentas-bancarias")
    public ResponseEntity<CuentaBancariaPaseadorResponse> registrarCuentaBancaria(
            @Valid @RequestBody RegistrarCuentaBancariaPaseadorRequest body,
            Authentication authentication) {
        Long idUsuario = resolvePaseadorIdOrNull(authentication);
        if (idUsuario == null) {
            return ResponseEntity.status(authentication == null || !authentication.isAuthenticated()
                    ? HttpStatus.UNAUTHORIZED
                    : HttpStatus.FORBIDDEN).build();
        }
        CuentaBancariaPaseadorResponse saved = billeteraService.registrarOActualizarCuentaBancaria(
                idUsuario, body.bancoId(), body.tipoCuentaId(), body.numeroCuenta());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Conveniencia para UI: mismo cálculo que {@link #obtenerMiBilletera}, pero solo un bucket.
     * No incluye historial ni proyección; para el resumen completo usar GET sin sufijo.
     * No acepta id de paseador por URL.
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

    /**
     * Historial de solicitudes de retiro del paseador (más recientes primero).
     */
    @GetMapping("/retiros")
    public ResponseEntity<List<RetiroHistorialItemResponse>> listarHistorialRetiros(Authentication authentication) {
        Long idUsuario = resolvePaseadorIdOrNull(authentication);
        if (idUsuario == null) {
            return ResponseEntity.status(authentication == null || !authentication.isAuthenticated()
                    ? HttpStatus.UNAUTHORIZED
                    : HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(retiroPaseadorService.listarHistorialRetiros(idUsuario));
    }

    /**
     * Descuenta {@code saldo_actual} en la misma transacción que registra {@link com.patiperro.pagos.model.TipoTransaccion#RETIRO_PASEADOR}.
     */
    @PostMapping("/retiros")
    public ResponseEntity<RetiroPaseadorResponse> solicitarRetiro(
            @Valid @RequestBody SolicitarRetiroPaseadorRequest body,
            Authentication authentication) {
        Long idUsuario = resolvePaseadorIdOrNull(authentication);
        if (idUsuario == null) {
            return ResponseEntity.status(authentication == null || !authentication.isAuthenticated()
                    ? HttpStatus.UNAUTHORIZED
                    : HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(retiroPaseadorService.solicitarRetiro(idUsuario, body.monto()));
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
