package com.patiperro.pagos.controller;

import com.patiperro.pagos.dto.recaudacion.RecaudacionPeriodoResponse;
import com.patiperro.pagos.service.RecaudacionPlataformaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/pagos/admin/recaudacion")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminRecaudacionController {

    private final RecaudacionPlataformaService recaudacionPlataformaService;

    @GetMapping("/acumulado")
    public List<RecaudacionPeriodoResponse> acumulado(
            @RequestParam(defaultValue = "day") String periodo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return recaudacionPlataformaService.acumulado(periodo, desde, hasta);
    }
}
