package com.patiperro.tutores.auth.controller;

import com.patiperro.tutores.auth.dto.LoginRequestDTO;
import com.patiperro.tutores.auth.dto.LoginResponseDTO;
import com.patiperro.tutores.auth.dto.RegisterRequestDTO;
import com.patiperro.tutores.auth.service.AuthService;
import com.patiperro.tutores.auth.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Objects;
import java.util.Map;

/** Auth del dueño de mascota (entidad Tutor): login, registro, logout. */
@RestController
@RequestMapping("/api/auth/tutores")
@RequiredArgsConstructor
public class AuthController {

    // Login / registro de tutor (dueño).
    private final AuthService authService;
    private final JwtService jwtService;

    // Endpoint publico para iniciar sesion.
    // Recibe correo + contrasena y retorna mensaje + correo del tutor autenticado.
    // El JWT se envia como cookie HttpOnly.
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO request) {
        LoginResponseDTO response = authService.login(request);
        String token = jwtService.generateToken(
                Objects.requireNonNull(response.getCorreo()),
                Objects.requireNonNull(response.getIdTutor()));
        response.setAccessToken(token);

        ResponseCookie cookie = ResponseCookie.from("access_token", Objects.requireNonNull(token))
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(jwtService.getExpirationMs() / 1000)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    // Registra un nuevo dueño de mascota (tutor): datos personales, direccion, fotoPerfil, biografia.
    @GetMapping("/correo-disponible")
    public ResponseEntity<Map<String, Object>> correoDisponible(@RequestParam("correo") String correo) {
        try {
            if (authService.correoDisponible(correo)) {
                return ResponseEntity.ok(Map.of("disponible", true));
            }
            return ResponseEntity.ok(Map.of(
                    "disponible", false,
                    "mensaje", "El correo ya está registrado"));
        } catch (IllegalArgumentException ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "Correo inválido";
            return ResponseEntity.badRequest().body(Map.of("disponible", false, "mensaje", msg));
        } catch (IllegalStateException ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "No se pudo validar el correo";
            return ResponseEntity.status(503).body(Map.of("disponible", false, "mensaje", msg));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        LoginResponseDTO response = authService.register(request);
        String token = jwtService.generateToken(
                Objects.requireNonNull(response.getCorreo()),
                Objects.requireNonNull(response.getIdTutor()));
        response.setAccessToken(token);

        ResponseCookie cookie = ResponseCookie.from("access_token", Objects.requireNonNull(token))
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(jwtService.getExpirationMs() / 1000)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    // Limpia la cookie de autenticacion en cliente.
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }
}