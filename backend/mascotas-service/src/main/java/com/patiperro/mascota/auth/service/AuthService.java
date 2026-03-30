package com.patiperro.mascota.auth.service;

import com.patiperro.mascota.auth.dto.AuthResponseDTO;
import com.patiperro.mascota.auth.dto.LoginRequestDTO;
import com.patiperro.mascota.auth.dto.RegisterRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponseDTO register(RegisterRequestDTO request) {
        // En un caso real, aquí guardarías el usuario en la DB
        String token = jwtService.generateToken(request.getEmail());
        return AuthResponseDTO.builder()
                .token(token)
                .build();
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        // En un caso real, aquí validarías la contraseña
        String token = jwtService.generateToken(request.getEmail());
        return AuthResponseDTO.builder()
                .token(token)
                .build();
    }
}