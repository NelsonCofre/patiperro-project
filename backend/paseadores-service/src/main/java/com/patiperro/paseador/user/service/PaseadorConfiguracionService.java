package com.patiperro.paseador.user.service;

import com.patiperro.paseador.model.Configuracion;
import com.patiperro.paseador.model.Paseador;
import com.patiperro.paseador.model.Tamano;
import com.patiperro.paseador.model.TarifaPaseador;
import com.patiperro.paseador.geo.NominatimGeocodingService;
import com.patiperro.paseador.model.Direccion;
import com.patiperro.paseador.repository.ConfiguracionRepository;
import com.patiperro.paseador.repository.DireccionRepository;
import com.patiperro.paseador.repository.PaseadorRepository;
import com.patiperro.paseador.repository.TamanoRepository;
import com.patiperro.paseador.user.dto.ConfiguracionPaseadorResponseDTO;
import com.patiperro.paseador.user.dto.PaseadorResumenResponseDTO;
import com.patiperro.paseador.user.dto.TarifaConfiguracionResponseDTO;
import com.patiperro.paseador.user.dto.TarifaInputDTO;
import com.patiperro.paseador.user.dto.UpsertConfiguracionRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaseadorConfiguracionService {

    private final PaseadorRepository paseadorRepository;
    private final ConfiguracionRepository configuracionRepository;
    private final TamanoRepository tamanoRepository;
    private final DireccionRepository direccionRepository;
    private final NominatimGeocodingService nominatimGeocodingService;

    @Transactional(readOnly = true)
    public ConfiguracionPaseadorResponseDTO getMyConfiguracion() {
        Paseador paseador = findAuthenticatedPaseador();
        Configuracion configuracion = configuracionRepository.findByPaseador_Id(paseador.getId())
                .orElseGet(() -> Configuracion.builder()
                        .paseador(paseador)
                        .radioCobertura(null)
                        .build());
        return toResponse(configuracion);
    }

    @Transactional(readOnly = true)
    public ConfiguracionPaseadorResponseDTO getConfiguracionPublicaByPaseadorId(Long paseadorId) {
        Configuracion configuracion = configuracionRepository.findByPaseador_Id(paseadorId)
                .orElseThrow(() -> new IllegalArgumentException("El paseador no tiene configuración publicada"));
        return toResponse(configuracion);
    }

    @Transactional(readOnly = true)
    public PaseadorResumenResponseDTO getResumenPublicoByPaseadorId(Long paseadorId) {
        Paseador paseador = paseadorRepository.findById(paseadorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paseador no encontrado"));
        return PaseadorResumenResponseDTO.builder()
                .idPaseador(paseador.getId())
                .nombreCompleto(nombrePublico(paseador))
                .fotoPerfil(paseador.getFotoPerfil())
                .correo(paseador.getCorreo())
                .esVerificado(PaseadorVerificacionService.esVerificadoPublicamente(paseador))
                .build();
    }

    @Transactional
    public ConfiguracionPaseadorResponseDTO upsertMyConfiguracion(UpsertConfiguracionRequestDTO request) {
        validateTarifas(request.getTarifas());

        Paseador paseador = findAuthenticatedPaseador();
        Configuracion configuracion = configuracionRepository.findByPaseador_Id(paseador.getId())
                .orElseGet(() -> Configuracion.builder()
                        .paseador(paseador)
                        .build());

        Map<Long, Tamano> tamanosById = loadTamanosById(request.getTarifas());

        configuracion.setRadioCobertura(request.getRadioCoberturaKm());

        if (configuracion.getTarifas() == null) {
            configuracion.setTarifas(new java.util.ArrayList<>());
        } else {
            configuracion.getTarifas().clear();
        }

        for (TarifaInputDTO t : request.getTarifas()) {
            TarifaPaseador tarifa = TarifaPaseador.builder()
                    .configuracion(configuracion)
                    .tamano(tamanosById.get(t.getTamanoId()))
                    .precioBase(t.getPrecioPorHora())
                    .build();
            configuracion.getTarifas().add(tarifa);
        }

        Configuracion saved = configuracionRepository.save(configuracion);

        Paseador conDir = paseadorRepository.findById(paseador.getId()).orElse(paseador);
        Direccion dir = conDir.getDireccion();
        if (dir != null && (dir.getLatitud() == null || dir.getLongitud() == null)) {
            nominatimGeocodingService.tryEnrich(dir);
            if (dir.getLatitud() != null && dir.getLongitud() != null) {
                direccionRepository.save(dir);
            }
        }

        return toResponse(saved);
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

    private void validateTarifas(List<TarifaInputDTO> tarifas) {
        Set<Long> seen = new HashSet<>();
        for (TarifaInputDTO t : tarifas) {
            if (!seen.add(t.getTamanoId())) {
                throw new IllegalArgumentException("No se puede repetir tamanoId en tarifas");
            }
        }
    }

    private Map<Long, Tamano> loadTamanosById(List<TarifaInputDTO> tarifas) {
        Set<Long> tamanoIds = tarifas.stream().map(TarifaInputDTO::getTamanoId).collect(java.util.stream.Collectors.toSet());
        List<Tamano> tamanos = tamanoRepository.findAllById(tamanoIds);
        if (tamanos.size() != tamanoIds.size()) {
            throw new IllegalArgumentException("Uno o más tamanoId no existen");
        }
        Map<Long, Tamano> map = new HashMap<>();
        for (Tamano t : tamanos) {
            map.put(t.getId(), t);
        }
        return map;
    }

    private static ConfiguracionPaseadorResponseDTO toResponse(Configuracion configuracion) {
        List<TarifaConfiguracionResponseDTO> tarifas = configuracion.getTarifas() == null
                ? List.of()
                : configuracion.getTarifas().stream()
                .map(t -> new TarifaConfiguracionResponseDTO(
                        t.getId(),
                        t.getTamano().getId(),
                        t.getTamano().getNombre(),
                        t.getPrecioBase()))
                .sorted(java.util.Comparator.comparing(TarifaConfiguracionResponseDTO::getTamanoId))
                .toList();

        return ConfiguracionPaseadorResponseDTO.builder()
                .configuracionId(configuracion.getId())
                .radioCoberturaKm(configuracion.getRadioCobertura())
                .tarifas(tarifas)
                .build();
    }

    private static String nombrePublico(Paseador p) {
        return java.util.stream.Stream.of(
                        p.getPrimerNombre(),
                        p.getSegundoNombre(),
                        p.getApellidoPaterno(),
                        p.getApellidoMaterno())
                .filter(s -> s != null && !s.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
    }
}
