package com.patiperro.paseador.user.service;

import com.patiperro.paseador.model.EstadoVerificacionIdentidad;
import com.patiperro.paseador.model.Paseador;
import com.patiperro.paseador.repository.PaseadorRepository;
import com.patiperro.paseador.user.dto.VerificacionIdentidadResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaseadorVerificacionService {

    private static final Set<String> LADOS = Set.of("frontal", "reverso");

    private final PaseadorRepository paseadorRepository;
    private final PaseadorVerificacionDocumentoStorageService storageService;

    @Transactional(readOnly = true)
    public VerificacionIdentidadResponseDTO obtenerEstadoAutenticado() {
        return toResponse(findAuthenticatedPaseador());
    }

    @Transactional
    public VerificacionIdentidadResponseDTO subirDocumentos(MultipartFile cedulaFrontal, MultipartFile cedulaReverso) {
        Paseador paseador = findAuthenticatedPaseador();
        validarPuedeSubir(paseador.getEstadoVerificacionIdentidad());

        String anteriorFrontal = paseador.getArchivoCedulaFrontal();
        String anteriorReverso = paseador.getArchivoCedulaReverso();
        String nuevoFrontal = null;
        String nuevoReverso = null;
        try {
            nuevoFrontal = storageService.save(cedulaFrontal);
            nuevoReverso = storageService.save(cedulaReverso);
            paseador.setArchivoCedulaFrontal(nuevoFrontal);
            paseador.setArchivoCedulaReverso(nuevoReverso);
            paseador.setEstadoVerificacionIdentidad(EstadoVerificacionIdentidad.EN_PROCESO);
            paseador.setVerificacionIdentidadEnviadaEn(LocalDateTime.now());
            paseador.setVerificacionIdentidadRevisadaEn(null);
            paseador.setMotivoRechazoVerificacionIdentidad(null);
            Paseador guardado = paseadorRepository.save(paseador);
            storageService.deleteQuietly(anteriorFrontal);
            storageService.deleteQuietly(anteriorReverso);
            return toResponse(guardado);
        } catch (IOException ex) {
            storageService.deleteQuietly(nuevoFrontal);
            storageService.deleteQuietly(nuevoReverso);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudieron guardar los documentos");
        } catch (RuntimeException ex) {
            storageService.deleteQuietly(nuevoFrontal);
            storageService.deleteQuietly(nuevoReverso);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Path resolverDocumentoAutenticado(String lado) {
        String ladoNorm = normalizarLado(lado);
        Paseador paseador = findAuthenticatedPaseador();
        String filename = filenameParaLado(paseador, ladoNorm);
        if (filename == null || filename.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no disponible");
        }
        Path path = storageService.resolveExisting(filename);
        if (path == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado");
        }
        return path;
    }

    @Transactional
    public VerificacionIdentidadResponseDTO revisarVerificacionInterna(
            Long idPaseador,
            EstadoVerificacionIdentidad nuevoEstado,
            String motivo) {
        if (nuevoEstado != EstadoVerificacionIdentidad.APROBADO
                && nuevoEstado != EstadoVerificacionIdentidad.RECHAZADO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estado de revisión inválido (use APROBADO o RECHAZADO)");
        }
        if (nuevoEstado == EstadoVerificacionIdentidad.RECHAZADO
                && (motivo == null || motivo.isBlank())) {
            throw new IllegalArgumentException("El motivo de rechazo es obligatorio");
        }
        Paseador paseador = paseadorRepository.findById(idPaseador)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paseador no encontrado"));
        if (paseador.getEstadoVerificacionIdentidad() != EstadoVerificacionIdentidad.EN_PROCESO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Solo se puede revisar un paseador en estado EN_PROCESO");
        }
        if (paseador.getArchivoCedulaFrontal() == null
                || paseador.getArchivoCedulaFrontal().isBlank()
                || paseador.getArchivoCedulaReverso() == null
                || paseador.getArchivoCedulaReverso().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No hay documentos de identidad registrados para este paseador");
        }
        paseador.setEstadoVerificacionIdentidad(nuevoEstado);
        paseador.setVerificacionIdentidadRevisadaEn(LocalDateTime.now());
        paseador.setMotivoRechazoVerificacionIdentidad(
                nuevoEstado == EstadoVerificacionIdentidad.RECHAZADO ? motivo.trim() : null);
        return toResponse(paseadorRepository.save(paseador));
    }

    public static boolean esVerificadoPublicamente(Paseador paseador) {
        return paseador != null
                && paseador.getEstadoVerificacionIdentidad() == EstadoVerificacionIdentidad.APROBADO;
    }

    private static void validarPuedeSubir(EstadoVerificacionIdentidad estado) {
        if (estado == EstadoVerificacionIdentidad.EN_PROCESO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Verificación en proceso: no puedes subir nuevos documentos hasta que un administrador responda");
        }
        if (estado == EstadoVerificacionIdentidad.APROBADO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tu identidad ya está verificada");
        }
    }

    private static String normalizarLado(String lado) {
        if (lado == null || lado.isBlank()) {
            throw new IllegalArgumentException("Lado de documento inválido (use frontal o reverso)");
        }
        String norm = lado.trim().toLowerCase(Locale.ROOT);
        if (!LADOS.contains(norm)) {
            throw new IllegalArgumentException("Lado de documento inválido (use frontal o reverso)");
        }
        return norm;
    }

    private static String filenameParaLado(Paseador paseador, String lado) {
        return "frontal".equals(lado)
                ? paseador.getArchivoCedulaFrontal()
                : paseador.getArchivoCedulaReverso();
    }

    private Paseador findAuthenticatedPaseador() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No hay sesión autenticada");
        }
        String correo = String.valueOf(authentication.getPrincipal()).trim();
        if (correo.isBlank() || "anonymousUser".equalsIgnoreCase(correo)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No hay sesión autenticada");
        }
        return paseadorRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Paseador autenticado no encontrado"));
    }

    static VerificacionIdentidadResponseDTO toResponse(Paseador paseador) {
        EstadoVerificacionIdentidad estado = paseador.getEstadoVerificacionIdentidad() != null
                ? paseador.getEstadoVerificacionIdentidad()
                : EstadoVerificacionIdentidad.SIN_ENVIAR;
        return VerificacionIdentidadResponseDTO.builder()
                .estado(estado)
                .estadoEtiqueta(etiquetaEstado(estado))
                .puedeSubir(estado == EstadoVerificacionIdentidad.SIN_ENVIAR
                        || estado == EstadoVerificacionIdentidad.RECHAZADO)
                .enviadoEn(paseador.getVerificacionIdentidadEnviadaEn())
                .revisadoEn(paseador.getVerificacionIdentidadRevisadaEn())
                .motivoRechazo(paseador.getMotivoRechazoVerificacionIdentidad())
                .tieneFrontal(paseador.getArchivoCedulaFrontal() != null && !paseador.getArchivoCedulaFrontal().isBlank())
                .tieneReverso(paseador.getArchivoCedulaReverso() != null && !paseador.getArchivoCedulaReverso().isBlank())
                .build();
    }

    private static String etiquetaEstado(EstadoVerificacionIdentidad estado) {
        return switch (estado) {
            case SIN_ENVIAR -> "Sin enviar";
            case EN_PROCESO -> "Verificación en proceso";
            case APROBADO -> "Verificado";
            case RECHAZADO -> "Rechazado";
        };
    }
}
