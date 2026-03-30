package com.patiperro.mascota.controller; // Asegúrate que esta sea la carpeta exacta

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mascotas")
public class MascotaHealthController {

    /**
     * Este endpoint es público y sirve para verificar 
     * si el microservicio está encendido.
     * URL: http://localhost:8083/api/mascotas/health
     */
    @GetMapping("/health")
    public String checkHealth() {
        return "Microservicio de Mascotas funcionando correctamente";
    }
}