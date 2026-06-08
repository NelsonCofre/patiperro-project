package com.patiperro.tutores.auth.service;

import com.patiperro.tutores.geo.NominatimGeocodingService;
import com.patiperro.tutores.auth.dto.LoginRequestDTO;
import com.patiperro.tutores.auth.dto.LoginResponseDTO;
import com.patiperro.tutores.auth.dto.RegisterRequestDTO;
import com.patiperro.tutores.auth.exception.InvalidCredentialsException;
import com.patiperro.tutores.auth.support.CorreoRegistroSupport;
import com.patiperro.tutores.auth.support.PaseadorCorreoRegistroClient;
import com.patiperro.tutores.user.model.Direccion;
import com.patiperro.tutores.user.model.Foto;
import com.patiperro.tutores.user.model.Tutor;
import com.patiperro.tutores.user.repository.DireccionRepository;
import com.patiperro.tutores.user.repository.FotoRepository;
import com.patiperro.tutores.user.repository.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/** Autenticacion de dueños de mascota (tabla {@code tutor}). */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final TutorRepository tutorRepository;
    private final DireccionRepository direccionRepository;
    private final FotoRepository fotoRepository;
    private final PasswordEncoder passwordEncoder;
    private final NominatimGeocodingService nominatimGeocodingService;
    private final PaseadorCorreoRegistroClient paseadorCorreoRegistroClient;

    // Flujo login:
    // 1) Buscar tutor por correo.
    // 2) Validar contrasena.
    // 3) Retornar respuesta de login.
    public LoginResponseDTO login(LoginRequestDTO request) {
        String correo = CorreoRegistroSupport.normalizar(request.getCorreo());
        Tutor tutor = tutorRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getContrasena(), tutor.getContrasena())) {
            throw new InvalidCredentialsException();
        }

        // Armamos el nombre para el storage del front
        String nombreCompleto = tutor.getPrimerNombre() + " " + tutor.getApellidoPaterno();

        return new LoginResponseDTO("Login exitoso", tutor.getCorreo(), tutor.getId(), null, nombreCompleto);
    }

    // Registro en una sola transaccion: direccion y fotos se persisten de forma explicita
    // (evita problemas de cascade/equals-hashCode de @Data en entidades).
    @Transactional
    @SuppressWarnings("null")
    public LoginResponseDTO register(RegisterRequestDTO request) {
        String correo = CorreoRegistroSupport.normalizar(request.getCorreo());
        assertCorreoDisponibleParaRegistro(correo);

        Direccion direccion = null;
        if (hasDireccionData(request)) {
            Direccion nueva = Direccion.builder()
                    .pais(request.getPais())
                    .region(request.getRegion())
                    .ciudad(request.getCiudad())
                    .calle(request.getCalle())
                    .comuna(request.getComuna())
                    .numeracion(request.getNumeracion())
                    .casaDepartamento(request.getCasaDepartamento())
                    .build();
            nominatimGeocodingService.tryEnrich(nueva);
            direccion = direccionRepository.save(nueva);
        }

        Tutor tutor = Tutor.builder()
                .rut(request.getRut())
                .primerNombre(request.getPrimerNombre())
                .segundoNombre(request.getSegundoNombre())
                .apellidoPaterno(request.getApellidoPaterno())
                .apellidoMaterno(request.getApellidoMaterno())
                .fechaNacimiento(request.getFechaNacimiento())
                .telefono(request.getTelefono())
                .correo(correo)
                .contrasena(passwordEncoder.encode(request.getContrasena()))
                .fotoPerfil(request.getFotoPerfil())
                .biografia(request.getBiografia())
                .direccion(direccion)
                .build();
        if (tutor.getFotos() == null) {
            tutor.setFotos(new ArrayList<>());
        }

        tutor = tutorRepository.save(tutor);

        // Galeria (tabla foto): solo URLs de `fotos`; fotoPerfil va en tutor.foto_perfil, no se duplica aqui.
        for (String url : collectGaleriaUrls(request)) {
            fotoRepository.save(Foto.builder().url(url).tutor(tutor).build());
        }

        // También enviamos el nombre en el registro
        String nombreCompleto = tutor.getPrimerNombre() + " " + tutor.getApellidoPaterno();

        return new LoginResponseDTO("Registro exitoso", tutor.getCorreo(), tutor.getId(), null, nombreCompleto);
    }

    /** Comprueba tutor + paseador (integración interna). */
    public boolean correoDisponible(String correoRaw) {
        String correo = CorreoRegistroSupport.normalizar(correoRaw);
        return !tutorRepository.existsByCorreoIgnoreCase(correo)
                && !paseadorCorreoRegistroClient.existeCorreo(correo);
    }

    private void assertCorreoDisponibleParaRegistro(String correoNormalizado) {
        if (!correoDisponible(correoNormalizado)) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }
    }

    private Set<String> collectGaleriaUrls(RegisterRequestDTO request) {
        Set<String> urls = new LinkedHashSet<>();
        if (request.getFotosUrls() != null) {
            for (String u : request.getFotosUrls()) {
                if (u != null && !u.isBlank()) {
                    urls.add(u.trim());
                }
            }
        }
        return urls;
    }

    // Determina si el request trae al menos un dato de direccion.
    // Si no trae datos, no se crea registro de direccion.
    private boolean hasDireccionData(RegisterRequestDTO request) {
        return request.getPais() != null || request.getRegion() != null || request.getCiudad() != null
                || request.getCalle() != null || request.getComuna() != null || request.getNumeracion() != null
                || (request.getCasaDepartamento() != null && !request.getCasaDepartamento().isBlank());
    }
}
