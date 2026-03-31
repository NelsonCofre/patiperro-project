package com.patiperro.paseador.user.controller;

import com.patiperro.paseador.user.dto.ConfiguracionPaseadorResponseDTO;
import com.patiperro.paseador.user.dto.UpsertConfiguracionRequestDTO;
import com.patiperro.paseador.user.service.PaseadorConfiguracionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/paseadores/me")
@RequiredArgsConstructor
public class PaseadorConfiguracionController {

    private final PaseadorConfiguracionService configuracionService;

    @GetMapping("/configuracion")
    public ResponseEntity<ConfiguracionPaseadorResponseDTO> getMyConfiguracion() {
        return ResponseEntity.ok(configuracionService.getMyConfiguracion());
    }

    @PutMapping("/configuracion")
    public ResponseEntity<ConfiguracionPaseadorResponseDTO> upsertMyConfiguracion(
            @Valid @RequestBody UpsertConfiguracionRequestDTO request) {
        return ResponseEntity.ok(configuracionService.upsertMyConfiguracion(request));
    }
}
