package com.patiperro.mascota.user.controller; // CORREGIDO: se agrega .user

import com.patiperro.mascota.model.Raza;
import com.patiperro.mascota.user.service.RazaService; // CORREGIDO: se agrega .user
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/razas") 
@CrossOrigin(origins = "*")
public class RazaController {

    @Autowired
    private RazaService razaService;

    @GetMapping
    public List<Raza> obtenerTodas() {
        return razaService.listarTodas();
    }
}