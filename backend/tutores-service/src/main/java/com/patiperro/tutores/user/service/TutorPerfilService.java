package com.patiperro.tutores.user.service;

import com.patiperro.tutores.user.dto.CambiarContrasenaRequestDTO;
import com.patiperro.tutores.user.dto.MiPerfilResponseDTO;
import com.patiperro.tutores.user.model.Tutor;
import com.patiperro.tutores.user.repository.TutorRepository;
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
public class TutorPerfilService {

    private static final Pattern PASSWORD_UPPER = Pattern.compile("[A-Z]");
    private static final Pattern PASSWORD_NUMBER = Pattern.compile("\\d");
    private static final Pattern PASSWORD_SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    private final TutorRepository tutorRepository;
    private final TutorPerfilFotoStorageService storageService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public MiPerfilResponseDTO getMyProfile() {
        return toResponse(findAuthenticatedTutor());
    }

    @Transactional
    public MiPerfilResponseDTO updateFotoPerfil(MultipartFile file) throws IOException {
        Tutor tutor = findAuthenticatedTutor();
        String urlAnterior = tutor.getFotoPerfil();
        String filenameNuevo = storageService.save(file);
        try {
            String urlNueva = storageService.buildPublicUrl(filenameNuevo);
            tutor.setFotoPerfil(urlNueva);
            tutorRepository.save(tutor);
            registrarLimpiezaFotoTrasCommit(urlAnterior, filenameNuevo);
            return toResponse(tutor);
        } catch (RuntimeException ex) {
            storageService.deleteQuietly(filenameNuevo);
            throw ex;
        }
    }

    @Transactional
    public void changePassword(CambiarContrasenaRequestDTO request) {
        validatePasswordStrength(request.getContrasenaNueva());
        Tutor tutor = findAuthenticatedTutor();
        if (!passwordEncoder.matches(request.getContrasenaActual(), tutor.getContrasena())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta");
        }
        if (passwordEncoder.matches(request.getContrasenaNueva(), tutor.getContrasena())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser distinta a la actual");
        }
        tutor.setContrasena(passwordEncoder.encode(request.getContrasenaNueva()));
        tutorRepository.save(tutor);
    }

    private Tutor findAuthenticatedTutor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("No hay sesión autenticada");
        }
        String correo = String.valueOf(authentication.getPrincipal()).trim();
        if (correo.isBlank() || "anonymousUser".equalsIgnoreCase(correo)) {
            throw new IllegalArgumentException("No hay sesión autenticada");
        }
        return tutorRepository.findByCorreo(correo)
                .orElseThrow(() -> new IllegalArgumentException("Tutor autenticado no encontrado"));
    }

    private MiPerfilResponseDTO toResponse(Tutor tutor) {
        return MiPerfilResponseDTO.builder()
                .id(tutor.getId())
                .nombreCompleto(nombreCompleto(tutor))
                .correo(tutor.getCorreo())
                .telefono(tutor.getTelefono())
                .fotoPerfil(tutor.getFotoPerfil())
                .biografia(tutor.getBiografia())
                .build();
    }

    private static String nombreCompleto(Tutor tutor) {
        return Stream.of(
                        tutor.getPrimerNombre(),
                        tutor.getSegundoNombre(),
                        tutor.getApellidoPaterno(),
                        tutor.getApellidoMaterno())
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
