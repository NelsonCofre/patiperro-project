package com.patiperro.resena.client;

import com.patiperro.resena.dto.TutorDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "tutor-service", url = "http://localhost:8081") // Donde corra tu micro de tutores
public interface TutorClient {
    @GetMapping("/api/tutores/{id}") // URL plural según tu controlador
    TutorDTO obtenerTutorPorId(@PathVariable("id") Long id);
}