package com.patiperro.usuarios.auth.service;

import com.patiperro.usuarios.auth.dto.LoginRequestDTO;
import com.patiperro.usuarios.auth.dto.LoginResponseDTO;
import com.patiperro.usuarios.user.model.Usuario;
import com.patiperro.usuarios.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;

    public LoginResponseDTO login(LoginRequestDTO request) {

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!usuario.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        return new LoginResponseDTO("Login exitoso", usuario.getEmail());
    }
}