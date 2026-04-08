package com.patiperro.paseador.auth.service;

import com.patiperro.paseador.auth.dto.LoginRequestDTO;
import com.patiperro.paseador.auth.dto.LoginResponseDTO;
import com.patiperro.paseador.auth.dto.RegisterRequestDTO;
import com.patiperro.paseador.auth.exception.InvalidCredentialsException;
import com.patiperro.paseador.model.Direccion;
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

    public LoginResponseDTO login(LoginRequestDTO request) {
        Paseador paseador = paseadorRepository.findByCorreo(request.getCorreo())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getContrasena(), paseador.getContrasena())) {
            throw new InvalidCredentialsException();
        }

        return new LoginResponseDTO("Login exitoso", paseador.getCorreo(), paseador.getId());
    }

    @Transactional
    @SuppressWarnings("null")
    public LoginResponseDTO register(RegisterRequestDTO request) {
        if (paseadorRepository.existsByCorreo(request.getCorreo())) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }

        Direccion direccion = null;
        if (hasDireccionData(request)) {
            direccion = direccionRepository.save(Direccion.builder()
                    .pais(request.getPais())
                    .region(request.getRegion())
                    .ciudad(request.getCiudad())
                    .calle(request.getCalle())
                    .comuna(request.getComuna())
                    .numeracion(request.getNumeracion())
                    .casaDepartamento(request.getCasaDepartamento())
                    .build());
        }

        Paseador paseador = Paseador.builder()
                .rut(request.getRut())
                .primerNombre(request.getPrimerNombre())
                .segundoNombre(request.getSegundoNombre())
                .apellidoPaterno(request.getApellidoPaterno())
                .apellidoMaterno(request.getApellidoMaterno())
                .fechaNacimiento(request.getFechaNacimiento())
                .telefono(request.getTelefono())
                .correo(request.getCorreo())
                .contrasena(passwordEncoder.encode(request.getContrasena()))
                .fotoPerfil(request.getFotoPerfil())
                .biografia(request.getBiografia())
                .direccion(direccion)
                .build();
        if (paseador.getFotos() == null) {
            paseador.setFotos(new ArrayList<>());
        }

        paseador = paseadorRepository.save(paseador);

        for (String url : collectGaleriaUrls(request)) {
            fotoRepository.save(Foto.builder().url(url).paseador(paseador).build());
        }

        return new LoginResponseDTO("Registro exitoso", paseador.getCorreo(), paseador.getId());
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
