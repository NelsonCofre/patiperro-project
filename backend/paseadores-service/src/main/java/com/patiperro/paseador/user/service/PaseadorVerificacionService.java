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

/**
 * Reglas de negocio de verificación de identidad (cédula) del paseador
 * autenticado.
 * Flujo MVP: un único PDF → {@link EstadoVerificacionIdentidad#APROBADO}
 * automático.
 */
@Service
@RequiredArgsConstructor
public class PaseadorVerificacionService {

    private static final Set<String> LADOS = Set.of("documento", "frontal", "reverso");
    private static final int MOTIVO_RECHAZO_MAX_LENGTH = 500;

    private final PaseadorRepository paseadorRepository;
    private final PaseadorVerificacionDocumentoStorageService storageService;

    @Transactional(readOnly = true)
    public VerificacionIdentidadResponseDTO obtenerEstadoAutenticado() {
        return VerificacionIdentidadResponseDTO.from(findAuthenticatedPaseador());
    }

    /**
     * Sube un único PDF y aprueba la identidad automáticamente (sin revisión
     * manual).
     */
    @Transactional
    public VerificacionIdentidadResponseDTO subirDocumento(MultipartFile documento) {
        Paseador paseador = findAuthenticatedPaseador();
        validarPuedeSubir(paseador.getEstadoVerificacionIdentidad());

        Long idPaseador = paseador.getId();
        String anteriorFrontal = paseador.getArchivoCedulaFrontal();
        String anteriorReverso = paseador.getArchivoCedulaReverso();
        String nuevo = null;
        try {
            nuevo = storageService.savePdf(documento);
            LocalDateTime ahora = LocalDateTime.now();

            Paseador actual = paseadorRepository.findByIdForUpdate(idPaseador)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "Paseador autenticado no encontrado"));
            validarPuedeSubir(actual.getEstadoVerificacionIdentidad());

            actual.setArchivoCedulaFrontal(nuevo);
            actual.setArchivoCedulaReverso(null);
            aplicarEstadoVerificacion(actual, EstadoVerificacionIdentidad.APROBADO);
            actual.setVerificacionIdentidadEnviadaEn(ahora);
            actual.setVerificacionIdentidadRevisadaEn(ahora);
            actual.setMotivoRechazoVerificacionIdentidad(null);
            Paseador guardado = paseadorRepository.save(actual);
            storageService.deleteQuietly(anteriorFrontal);
            storageService.deleteQuietly(anteriorReverso);
            return VerificacionIdentidadResponseDTO.from(guardado);
        } catch (IOException ex) {
            storageService.deleteQuietly(nuevo);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el documento");
        } catch (RuntimeException ex) {
            storageService.deleteQuietly(nuevo);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Path resolverDocumentoAutenticado() {
        return resolverDocumento(findAuthenticatedPaseador(), "documento");
    }

    @Transactional(readOnly = true)
    public Path resolverDocumentoAutenticado(String lado) {
        return resolverDocumento(findAuthenticatedPaseador(), normalizarLado(lado));
    }

    @Transactional(readOnly = true)
    public Path resolverDocumentoPorPaseadorId(Long idPaseador, String lado) {
        Paseador paseador = paseadorRepository.findById(idPaseador)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paseador no encontrado"));
        return resolverDocumento(paseador, normalizarLado(lado));
    }

    private Path resolverDocumento(Paseador paseador, String ladoNorm) {
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
        if (!nuevoEstado.esDecisionRevisionAdmin()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estado de revisión inválido (use APROBADO o RECHAZADO)");
        }
        if (nuevoEstado == EstadoVerificacionIdentidad.RECHAZADO) {
            validarMotivoRechazo(motivo);
        }
        Paseador paseador = paseadorRepository.findByIdForUpdate(idPaseador)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paseador no encontrado"));
        if (paseador.getEstadoVerificacionIdentidad() != EstadoVerificacionIdentidad.EN_PROCESO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Solo se puede revisar un paseador en estado EN_PROCESO");
        }
        if (!tieneDocumentoRegistrado(paseador)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No hay documento de identidad registrado para este paseador");
        }
        aplicarEstadoVerificacion(paseador, nuevoEstado);
        paseador.setVerificacionIdentidadRevisadaEn(LocalDateTime.now());
        paseador.setMotivoRechazoVerificacionIdentidad(
                nuevoEstado == EstadoVerificacionIdentidad.RECHAZADO ? motivo.trim() : null);
        return VerificacionIdentidadResponseDTO.from(paseadorRepository.save(paseador));
    }

    /**
     * Badge público de confianza (búsqueda / perfil tutor).
     * Fuente de verdad: {@link EstadoVerificacionIdentidad#APROBADO}; {@code es_verificado} es derivado en BD.
     */
    public static boolean esVerificadoPublicamente(Paseador paseador) {
        if (paseador == null) {
            return false;
        }
        EstadoVerificacionIdentidad estado = paseador.getEstadoVerificacionIdentidad();
        return estado != null && estado.esAprobado();
    }

    /**
     * Único punto de escritura de estado + {@code es_verificado} en este servicio.
     * Usar en {@link #subirDocumento} y {@link #revisarVerificacionInterna}; {@link Paseador} refuerza en JPA.
     */
    static void aplicarEstadoVerificacion(Paseador paseador, EstadoVerificacionIdentidad estado) {
        paseador.setEstadoVerificacionIdentidad(estado);
        paseador.setEsVerificado(estado != null && estado.esAprobado());
    }

    private static void validarPuedeSubir(EstadoVerificacionIdentidad estado) {
        EstadoVerificacionIdentidad efectivo = estado != null
                ? estado
                : EstadoVerificacionIdentidad.SIN_ENVIAR;
        efectivo.mensajeBloqueoSubida().ifPresent(msg -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, msg);
        });
    }

    private static void validarMotivoRechazo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de rechazo es obligatorio");
        }
        if (motivo.trim().length() > MOTIVO_RECHAZO_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "El motivo de rechazo no puede superar " + MOTIVO_RECHAZO_MAX_LENGTH + " caracteres");
        }
    }

    private static boolean tieneDocumentoRegistrado(Paseador paseador) {
        return paseador.getArchivoCedulaFrontal() != null
                && !paseador.getArchivoCedulaFrontal().isBlank();
    }

    private static String filenameParaLado(Paseador paseador, String ladoNorm) {
        return switch (ladoNorm) {
            case "documento", "frontal" -> paseador.getArchivoCedulaFrontal();
            case "reverso" -> paseador.getArchivoCedulaReverso();
            default ->
                throw new IllegalArgumentException("Lado de documento inválido (use documento, frontal o reverso)");
        };
    }

    private static String normalizarLado(String lado) {
        if (lado == null || lado.isBlank()) {
            throw new IllegalArgumentException("Lado de documento inválido (use documento, frontal o reverso)");
        }
        String norm = lado.trim().toLowerCase(Locale.ROOT);
        if (!LADOS.contains(norm)) {
            throw new IllegalArgumentException("Lado de documento inválido (use documento, frontal o reverso)");
        }
        return norm;
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Paseador autenticado no encontrado"));
    }
}
