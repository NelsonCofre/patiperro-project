package com.patiperro.paseador.user.service;

import com.patiperro.paseador.model.Paseador;
import com.patiperro.paseador.repository.PaseadorRepository;
import com.patiperro.paseador.user.dto.CambiarContrasenaRequestDTO;
import com.patiperro.paseador.user.dto.MiPerfilResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PaseadorPerfilService {

    private static final Pattern PASSWORD_UPPER = Pattern.compile("[A-Z]");
    private static final Pattern PASSWORD_NUMBER = Pattern.compile("\\d");
    private static final Pattern PASSWORD_SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    private final PaseadorRepository paseadorRepository;
    private final PaseadorPerfilFotoStorageService storageService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public MiPerfilResponseDTO getMyProfile() {
        return toResponse(findAuthenticatedPaseador());
    }

    @Transactional
    public MiPerfilResponseDTO updateFotoPerfil(MultipartFile file) throws IOException {
        Paseador paseador = findAuthenticatedPaseador();
        String urlAnterior = paseador.getFotoPerfil();
        String filenameNuevo = storageService.save(file);
        try {
            String urlNueva = storageService.buildPublicUrl(filenameNuevo);
            paseador.setFotoPerfil(urlNueva);
            paseadorRepository.save(paseador);
            registrarLimpiezaFotoTrasCommit(urlAnterior, filenameNuevo);
            return toResponse(paseador);
        } catch (RuntimeException ex) {
            storageService.deleteQuietly(filenameNuevo);
            throw ex;
        }
    }

    @Transactional
    public void changePassword(CambiarContrasenaRequestDTO request) {
        validatePasswordStrength(request.getContrasenaNueva());
        Paseador paseador = findAuthenticatedPaseador();
        if (!passwordEncoder.matches(request.getContrasenaActual(), paseador.getContrasena())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta");
        }
        if (passwordEncoder.matches(request.getContrasenaNueva(), paseador.getContrasena())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser distinta a la actual");
        }
        paseador.setContrasena(passwordEncoder.encode(request.getContrasenaNueva()));
        paseadorRepository.save(paseador);
    }

    private Paseador findAuthenticatedPaseador() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("No hay sesión autenticada");
        }
        String correo = String.valueOf(authentication.getPrincipal()).trim();
        if (correo.isBlank() || "anonymousUser".equalsIgnoreCase(correo)) {
            throw new IllegalArgumentException("No hay sesión autenticada");
        }
        return paseadorRepository.findByCorreo(correo)
                .orElseThrow(() -> new IllegalArgumentException("Paseador autenticado no encontrado"));
    }

    private MiPerfilResponseDTO toResponse(Paseador paseador) {
        return MiPerfilResponseDTO.builder()
                .id(paseador.getId())
                .nombreCompleto(nombreCompleto(paseador))
                .correo(paseador.getCorreo())
                .telefono(paseador.getTelefono() != null ? String.valueOf(paseador.getTelefono()) : null)
                .fotoPerfil(paseador.getFotoPerfil())
                .biografia(paseador.getBiografia())
                .build();
    }

    private static String nombreCompleto(Paseador paseador) {
        return Stream.of(
                        paseador.getPrimerNombre(),
                        paseador.getSegundoNombre(),
                        paseador.getApellidoPaterno(),
                        paseador.getApellidoMaterno())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" "));
    }

    private void validatePasswordStrength(String password) {
        if (password == null
                || password.length() < 8
                || !PASSWORD_UPPER.matcher(password).find()
                || !PASSWORD_NUMBER.matcher(password).find()
                || !PASSWORD_SPECIAL.matcher(password).find()) {
            throw new IllegalArgumentException(
                    "Tu contraseña debe contener al menos 8 caracteres, incluyendo una mayúscula, un número y un carácter especial.");
        }
    }

    private void registrarLimpiezaFotoTrasCommit(String urlAnterior, String filenameNuevo) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            eliminarArchivoLocalSiNuestro(urlAnterior);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eliminarArchivoLocalSiNuestro(urlAnterior);
            }

            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    storageService.deleteQuietly(filenameNuevo);
                }
            }
        });
    }

    private void eliminarArchivoLocalSiNuestro(String url) {
        String filename = storageService.extractFilenameFromPublicUrl(url);
        if (filename != null) {
            storageService.deleteQuietly(filename);
        }
    }
}
