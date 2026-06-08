package com.patiperro.paseador.auth.controller;

import com.patiperro.paseador.auth.dto.LoginRequestDTO;
import com.patiperro.paseador.auth.dto.LoginResponseDTO;
import com.patiperro.paseador.auth.dto.RegisterRequestDTO;
import com.patiperro.paseador.auth.service.AuthService;
import com.patiperro.paseador.auth.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.Map;

@RestController
@RequestMapping("/api/paseadores/auth")
@RequiredArgsConstructor
public class AuthController {

        private final AuthService authService;
        private final JwtService jwtService;

        @PostMapping("/login")
        public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO request) {
                LoginResponseDTO response = authService.login(request);
        String token = jwtService.generateToken(
                Objects.requireNonNull(response.getCorreo()),
                Objects.requireNonNull(response.getIdPaseador()));
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
                                Objects.requireNonNull(response.getIdPaseador()));
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
