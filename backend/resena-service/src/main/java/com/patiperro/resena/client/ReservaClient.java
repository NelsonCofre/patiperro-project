package com.patiperro.resena.client;

import com.patiperro.resena.dto.ReservaDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// El nombre debe coincidir con el del microservicio y la URL donde corre
@FeignClient(name = "reserva-service", url = "http://localhost:8090/api/reserva") 
public interface ReservaClient {
    @GetMapping("/{id}") 
    ReservaDTO obtenerReservaPorId(@PathVariable("id") Integer id); // Cambiado a Integer
}