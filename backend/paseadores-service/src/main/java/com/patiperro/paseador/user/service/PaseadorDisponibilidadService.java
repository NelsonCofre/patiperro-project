package com.patiperro.paseador.user.service;

import com.patiperro.paseador.client.AgendaDisponibilidadClient;
import com.patiperro.paseador.dto.agenda.AgendaBloqueOfertaJsonDTO;
import com.patiperro.paseador.dto.disponibilidad.BloqueDisponibleResponseDTO;
import com.patiperro.paseador.dto.disponibilidad.DisponibilidadPorFechaResponseDTO;
import com.patiperro.paseador.dto.disponibilidad.PaseadorDisponibilidadResponseDTO;
import com.patiperro.paseador.repository.PaseadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Disponibilidad pública del paseador consultando agenda-service.
 * <p><b>Identificadores:</b> se asume que {@code id_paseador} (paseadores-service) es el mismo valor
 * que {@code id_usuario} en agenda-service. Si en el futuro el modelo de negocio separa la entidad
 * usuario de la entidad paseador, esta consulta podría apuntar al usuario equivocado o devolver
 * resultados vacíos sin que el fallo sea evidente; habría que introducir un mapeo explícito
 * (p. ej. tabla o API que traduzca id paseador → id usuario de agenda).
 */
@Service
@RequiredArgsConstructor
public class PaseadorDisponibilidadService {

    private final PaseadorRepository paseadorRepository;
    private final AgendaDisponibilidadClient agendaDisponibilidadClient;

    @Value("${patiperro.paseadores.cercanos.id-estado-bloque-disponible:1}")
    private int idEstadoBloqueDisponible;

    public PaseadorDisponibilidadResponseDTO disponibilidadProximosDias(long idPaseador, int dias) {
        if (!paseadorRepository.existsById(idPaseador)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paseador no encontrado");
        }
        if (dias < 1 || dias > 31) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dias debe estar entre 1 y 31");
        }
        if (idPaseador > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id de paseador fuera de rango para agenda");
        }
        int idUsuarioAgenda = Math.toIntExact(idPaseador);

        LocalDate desde = LocalDate.now();
        LocalDate hasta = desde.plusDays(dias - 1L);

        List<AgendaBloqueOfertaJsonDTO> bloques = agendaDisponibilidadClient.listarBloquesOfertables(
                idUsuarioAgenda, desde, hasta);

        List<AgendaBloqueOfertaJsonDTO> soloDisponibles = bloques.stream()
                .filter(b -> b.getEstadoBloque() != null
                        && Objects.equals(b.getEstadoBloque().getIdEstado(), idEstadoBloqueDisponible))
                .sorted(Comparator.comparing(AgendaBloqueOfertaJsonDTO::getFecha)
                        .thenComparing(AgendaBloqueOfertaJsonDTO::getHoraInicio))
                .toList();

        Map<LocalDate, List<BloqueDisponibleResponseDTO>> porFecha = new LinkedHashMap<>();
        for (AgendaBloqueOfertaJsonDTO b : soloDisponibles) {
            BloqueDisponibleResponseDTO item = BloqueDisponibleResponseDTO.builder()
                    .idAgenda(b.getIdAgenda())
                    .horaInicio(b.getHoraInicio())
                    .horaFinal(b.getHoraFinal())
                    .build();
            porFecha.computeIfAbsent(b.getFecha(), k -> new ArrayList<>()).add(item);
        }

        List<DisponibilidadPorFechaResponseDTO> diasOrdenados = porFecha.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> DisponibilidadPorFechaResponseDTO.builder()
                        .fecha(e.getKey())
                        .bloques(e.getValue())
                        .build())
                .collect(Collectors.toList());

        return PaseadorDisponibilidadResponseDTO.builder()
                .idPaseador(idPaseador)
                .dias(dias)
                .porFecha(diasOrdenados)
                .build();
    }
}
