package com.patiperro.paseador.controller;

import com.patiperro.paseador.user.dto.ConfiguracionPaseadorResponseDTO;
import com.patiperro.paseador.user.service.PaseadorConfiguracionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/paseadores/public")
@RequiredArgsConstructor
public class PaseadorConfiguracionPublicController {

    private final PaseadorConfiguracionService configuracionService;

    @GetMapping("/{idPaseador}/configuracion")
    public ConfiguracionPaseadorResponseDTO obtenerConfiguracionPublica(@PathVariable Long idPaseador) {
        return configuracionService.getConfiguracionPublicaByPaseadorId(idPaseador);
    }
}
