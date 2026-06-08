package com.patiperro.paseador.auth.service;

import com.patiperro.paseador.auth.dto.LoginRequestDTO;
import com.patiperro.paseador.auth.dto.LoginResponseDTO;
import com.patiperro.paseador.auth.dto.RegisterRequestDTO;
import com.patiperro.paseador.auth.exception.InvalidCredentialsException;
import com.patiperro.paseador.auth.support.CorreoRegistroSupport;
import com.patiperro.paseador.auth.support.TutorCorreoRegistroClient;
import com.patiperro.paseador.geo.NominatimGeocodingService;
import com.patiperro.paseador.model.Direccion;
import com.patiperro.paseador.model.EstadoVerificacionIdentidad;
import com.patiperro.paseador.model.Foto;
import com.patiperro.paseador.model.Paseador;
import com.patiperro.paseador.repository.DireccionRepository;
import com.patiperro.paseador.repository.FotoRepository;
import com.patiperro.paseador.repository.PaseadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PaseadorRepository paseadorRepository;
    private final DireccionRepository direccionRepository;
    private final FotoRepository fotoRepository;
    private final PasswordEncoder passwordEncoder;
    private final NominatimGeocodingService nominatimGeocodingService;
    private final TutorCorreoRegistroClient tutorCorreoRegistroClient;

    public LoginResponseDTO login(LoginRequestDTO request) {
        String correo = CorreoRegistroSupport.normalizar(request.getCorreo());
        Paseador paseador = paseadorRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getContrasena(), paseador.getContrasena())) {
            throw new InvalidCredentialsException();
        }

        // Construimos el nombre completo para el DTO
        String nombreCompleto = paseador.getPrimerNombre() + " " + paseador.getApellidoPaterno();

        return new LoginResponseDTO(
            "Login exitoso", 
            paseador.getCorreo(), 
            paseador.getId(), 
            null, 
            nombreCompleto // Nuevo parámetro
        );
    }

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

        Paseador paseador = Paseador.builder()
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
                .estadoVerificacionIdentidad(EstadoVerificacionIdentidad.SIN_ENVIAR)
                .build();
        
        if (paseador.getFotos() == null) {
            paseador.setFotos(new ArrayList<>());
        }

        paseador = paseadorRepository.save(paseador);

        for (String url : collectGaleriaUrls(request)) {
            fotoRepository.save(Foto.builder().url(url).paseador(paseador).build());
        }

        // También enviamos el nombre en el registro exitoso
        String nombreCompleto = paseador.getPrimerNombre() + " " + paseador.getApellidoPaterno();

        return new LoginResponseDTO(
            "Registro exitoso", 
            paseador.getCorreo(), 
            paseador.getId(), 
            null, 
            nombreCompleto // Nuevo parámetro
        );
    }

    /** Comprueba paseador + tutor (integración interna). */
    public boolean correoDisponible(String correoRaw) {
        String correo = CorreoRegistroSupport.normalizar(correoRaw);
        return !paseadorRepository.existsByCorreoIgnoreCase(correo)
                && !tutorCorreoRegistroClient.existeCorreo(correo);
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

    private boolean hasDireccionData(RegisterRequestDTO request) {
        return request.getPais() != null || request.getRegion() != null || request.getCiudad() != null
                || request.getCalle() != null || request.getComuna() != null || request.getNumeracion() != null
                || (request.getCasaDepartamento() != null && !request.getCasaDepartamento().isBlank());
    }
}