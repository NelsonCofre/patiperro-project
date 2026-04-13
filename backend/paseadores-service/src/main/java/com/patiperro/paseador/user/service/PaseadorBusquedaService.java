package com.patiperro.paseador.user.service;

import com.patiperro.paseador.client.AgendaDisponibilidadClient;
import com.patiperro.paseador.model.Paseador;
import com.patiperro.paseador.repository.PaseadorRepository;
import com.patiperro.paseador.user.dto.PaseadorCercanoResponseDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Búsqueda geográfica de paseadores. Si se envían los cuatro parámetros de agenda,
 * el filtro de disponibilidad lo resuelve agenda-service (incluye exclusión de días
 * bloqueados por motivos personales cuando así esté implementado allí).
 */
@Service
@RequiredArgsConstructor
public class PaseadorBusquedaService {

    private static final String SQL_CERCANOS = """
            SELECT * FROM (
                SELECT p.id_paseador AS id_paseador,
                    (6371.0 * acos(GREATEST(-1.0, LEAST(1.0,
                        cos(radians(:latRef)) * cos(radians(d.latitud)) * cos(radians(d.longitud) - radians(:lonRef))
                        + sin(radians(:latRef)) * sin(radians(d.latitud))
                    )))) AS distancia_km,
                    c.radio_cobertura AS radio_cobertura,
                    d.latitud AS latitud,     -- AGREGA ESTA LÍNEA
                    d.longitud AS longitud
                FROM paseador p
                INNER JOIN direccion d ON p.direccion_id_direccion = d.id_direccion
                INNER JOIN configuracion c ON c.paseador_id_paseador = p.id_paseador
                WHERE d.latitud IS NOT NULL
                  AND d.longitud IS NOT NULL
                  AND c.radio_cobertura IS NOT NULL
            ) t
            WHERE t.distancia_km <= LEAST(t.radio_cobertura::double precision, :radioMaxKm)
            ORDER BY t.distancia_km ASC
            LIMIT :limite
            """;

    @PersistenceContext
    private EntityManager entityManager;

    private final PaseadorRepository paseadorRepository;
    private final AgendaDisponibilidadClient agendaDisponibilidadClient;

    /**
     * Candidatos por distancia; opcionalmente restringidos a quienes tienen bloque
     * disponible en la franja indicada (mismo día en {@code fechaDisponibilidad}).
     * Los IDs devueltos por agenda deben corresponder a {@code paseador.id_paseador} / {@code id_usuario} en agenda.
     */
    @Transactional(readOnly = true)
    public List<PaseadorCercanoResponseDTO> buscarCercanos(
            double latitudReferencia,
            double longitudReferencia,
            double radioBusquedaMaxKm,
            int limite,
            LocalDate fechaDisponibilidad,
            LocalDateTime horaInicioDisponibilidad,
            LocalDateTime horaFinDisponibilidad,
            Integer idEstadoBloqueDisponible) {

        validarCoordenadas(latitudReferencia, longitudReferencia);
        if (radioBusquedaMaxKm <= 0 || radioBusquedaMaxKm > 500) {
            throw new IllegalArgumentException("radioBusquedaMaxKm debe estar entre 0 exclusivo y 500");
        }
        int limiteSeguro = Math.min(Math.max(limite, 1), 100);

        boolean filtroAgenda = fechaDisponibilidad != null
                || horaInicioDisponibilidad != null
                || horaFinDisponibilidad != null
                || idEstadoBloqueDisponible != null;
        boolean agendaCompleto = fechaDisponibilidad != null
                && horaInicioDisponibilidad != null
                && horaFinDisponibilidad != null
                && idEstadoBloqueDisponible != null;
        if (filtroAgenda && !agendaCompleto) {
            throw new IllegalArgumentException(
                    "Para filtrar por agenda indique fechaDisponibilidad, horaInicioDisponibilidad, horaFinDisponibilidad e idEstadoBloqueDisponible");
        }

        Query query = entityManager.createNativeQuery(SQL_CERCANOS);
        query.setParameter("latRef", latitudReferencia);
        query.setParameter("lonRef", longitudReferencia);
        query.setParameter("radioMaxKm", radioBusquedaMaxKm);
        query.setParameter("limite", limiteSeguro);

        @SuppressWarnings("unchecked")
        List<Object[]> filas = query.getResultList();

        List<CandidatoGeo> candidatos = new ArrayList<>();
        for (Object[] cols : filas) {
            long id = ((Number) cols[0]).longValue();
            double dist = ((Number) cols[1]).doubleValue();
            BigDecimal radio = cols[2] != null ? new BigDecimal(cols[2].toString()) : null;
            // 1. Extraemos las nuevas columnas del Object[]
            double lat = ((Number) cols[3]).doubleValue(); 
            double lon = ((Number) cols[4]).doubleValue();

            // 2. Pasamos los 5 parámetros al constructor (ahora sí estará definido)
            candidatos.add(new CandidatoGeo(id, dist, radio, lat, lon));
        }

        // Intersección geo ∩ agenda; la resta "disponibilidad − bloqueo día" vive en agenda-service.
        if (agendaCompleto) {
            List<Integer> idsAgenda = agendaDisponibilidadClient.idsConBloqueDisponible(
                    fechaDisponibilidad, horaInicioDisponibilidad, horaFinDisponibilidad, idEstadoBloqueDisponible);
            Set<Long> permitidos = idsAgenda.stream().map(Integer::longValue).collect(Collectors.toSet());
            candidatos = candidatos.stream()
                    .filter(c -> permitidos.contains(c.idPaseador()))
                    .toList();
        }

        if (candidatos.isEmpty()) {
            return List.of();
        }

        List<Long> idsOrdenados = candidatos.stream().map(c -> c.idPaseador).toList();
        Map<Long, CandidatoGeo> porId = candidatos.stream()
                .collect(Collectors.toMap(CandidatoGeo::idPaseador, c -> c, (a, b) -> a, LinkedHashMap::new));

        Map<Long, Paseador> entidades = paseadorRepository.findAllById(idsOrdenados).stream()
                .collect(Collectors.toMap(Paseador::getId, p -> p));

        List<PaseadorCercanoResponseDTO> salida = new ArrayList<>();
        for (Long id : idsOrdenados) {
            Paseador p = entidades.get(id);
            if (p == null) {
                continue;
            }
            CandidatoGeo cg = porId.get(id);
            
            // AGREGAMOS .latitud() y .longitud() al builder
            salida.add(PaseadorCercanoResponseDTO.builder()
                    .idPaseador(p.getId())
                    .nombreCompleto(nombrePublico(p))
                    .fotoPerfil(p.getFotoPerfil())
                    .biografia(p.getBiografia())
                    .distanciaKm(cg.distanciaKm())
                    .radioCoberturaKm(cg.radioCoberturaKm())
                    .latitud(cg.latitud())   // <--- ESTA LÍNEA FALTA
                    .longitud(cg.longitud()) // <--- ESTA LÍNEA FALTA
                    .build());
        }
        return salida;
    }

    private static void validarCoordenadas(double lat, double lon) {
        if (lat < -90 || lat > 90) {
            throw new IllegalArgumentException("latitudReferencia debe estar entre -90 y 90");
        }
        if (lon < -180 || lon > 180) {
            throw new IllegalArgumentException("longitudReferencia debe estar entre -180 y 180");
        }
    }

    private static String nombrePublico(Paseador p) {
        StringBuilder sb = new StringBuilder();
        if (p.getPrimerNombre() != null && !p.getPrimerNombre().isBlank()) {
            sb.append(p.getPrimerNombre().trim());
        }
        if (p.getSegundoNombre() != null && !p.getSegundoNombre().isBlank()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(p.getSegundoNombre().trim());
        }
        if (p.getApellidoPaterno() != null && !p.getApellidoPaterno().isBlank()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(p.getApellidoPaterno().trim());
        }
        return sb.toString();
    }

    private record CandidatoGeo(long idPaseador, double distanciaKm, BigDecimal radioCoberturaKm, double latitud, double longitud) {}
}
