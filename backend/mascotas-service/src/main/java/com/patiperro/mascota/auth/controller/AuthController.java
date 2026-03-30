package com.patiperro.mascota.auth.controller;

import com.patiperro.mascota.auth.dto.AuthResponseDTO;
import com.patiperro.mascota.auth.dto.LoginRequestDTO;
import com.patiperro.mascota.auth.dto.RegisterRequestDTO;
import com.patiperro.mascota.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mascotas/auth") // Ruta pública para autenticación
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    /**
     * Endpoint para registrar un nuevo Tutor.
     * URL: POST http://localhost:8083/api/mascotas/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @RequestBody RegisterRequestDTO request
    ) {
        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * Endpoint para iniciar sesión y obtener el token JWT.
     * URL: POST http://localhost:8083/api/mascotas/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @RequestBody LoginRequestDTO request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }
}