package com.patiperro.usuarios.auth.controller;

import com.patiperro.usuarios.auth.dto.LoginRequestDTO;
import com.patiperro.usuarios.auth.dto.LoginResponseDTO;
import com.patiperro.usuarios.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }
}