package com.patiperro.paseador.user.service;

import com.patiperro.paseador.client.AgendaDisponibilidadClient;
import com.patiperro.paseador.dto.user.PaseadorPerfilDTO;
import com.patiperro.paseador.model.Paseador;
import com.patiperro.paseador.repository.PaseadorRepository;
import com.patiperro.paseador.repository.TarifaPaseadorRepository;
import com.patiperro.paseador.user.dto.PaseadorCercanoResponseDTO;
import com.patiperro.paseador.user.dto.PaseadorCercanosConConteoResponseDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
 * Búsqueda geográfica de paseadores y perfiles públicos para tutores.
 * El campo {@code verificado} en los DTOs públicos indica identidad aprobada ({@code APROBADO});
 * no expone estado del flujo ni documentos de cédula.
 * <p>
 * Si se envían los cuatro parámetros de agenda,
 * el filtro de disponibilidad lo resuelve agenda-service (incluye exclusión de días
 * bloqueados por motivos personales cuando así esté implementado allí).
 * Opcionalmente ({@code patiperro.paseadores.cercanos.filtrar-disponible-desde-hoy})
 * se cruza con {@link AgendaDisponibilidadClient#idsConBloqueDisponibleDesdeHoy(int)}.
 * Con {@code soloVerificados=true} se amplía el prefetch geo (mismo criterio que agenda) y se
 * filtra en memoria usando {@code es_verificado}, reutilizando una sola carga de entidades.
 */
@Service
@RequiredArgsConstructor
public class PaseadorBusquedaService {

    private static final double RADIO_MIN_EXCLUSIVO_KM = 0.0;
    private static final double RADIO_MAX_KM = 500.0;
    private static final int LIMITE_MIN = 1;
    private static final int LIMITE_MAX = 100;
    private static final int OFFSET_MIN = 0;
    private static final int GEO_SQL_HARD_CAP = 5000;

    /**
     * Si es true y no hay filtro por franja completo, solo se devuelven paseadores con bloque
     * disponible desde hoy según agenda-service.
     *
     * Nota: esto aproxima el requisito "próximos 7 días" como "desde hoy en adelante".
     * Para cumplir estrictamente 7 días se requiere acotar la ventana en agenda-service
     * (o exponer un endpoint/param que filtre hasta hoy+7).
     */
    @Value("${patiperro.paseadores.cercanos.filtrar-disponible-desde-hoy:false}")
    private boolean filtrarDisponibleDesdeHoyCercanos;

    /** Id del estado "Disponible" en agenda (catálogo estado_bloque). */
    @Value("${patiperro.paseadores.cercanos.id-estado-bloque-disponible:1}")
    private int idEstadoBloqueDisponibleParaCercanos;

    /** Maximo de candidatos geo a traer cuando hay filtro de agenda (franja o desde-hoy). */
    @Value("${patiperro.paseadores.cercanos.max-geo-prefetch-con-agenda:2000}")
    private int maxGeoPrefetchConAgenda;

    /** Maximo de candidatos geo para el endpoint con conteo/paginacion. */
    @Value("${patiperro.paseadores.cercanos.max-geo-prefetch-con-conteo:2000}")
    private int maxGeoPrefetchConConteo;

    private static final String SQL_CERCANOS_TOP = """
            SELECT * FROM (
                SELECT p.id_paseador AS id_paseador,
                    (6371.0 * acos(GREATEST(-1.0, LEAST(1.0,
                        cos(radians(:latRef)) * cos(radians(d.latitud)) * cos(radians(d.longitud) - radians(:lonRef))
                        + sin(radians(:latRef)) * sin(radians(d.latitud))
                    )))) AS distancia_km,
                    c.radio_cobertura AS radio_cobertura,
                    d.latitud AS latitud,
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
            LIMIT :limiteMax
            """;

    @PersistenceContext
    private EntityManager entityManager;

    private final PaseadorRepository paseadorRepository;
    private final AgendaDisponibilidadClient agendaDisponibilidadClient;

    private final TarifaPaseadorRepository tarifaRepository;

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
            Integer idEstadoBloqueDisponible,
            boolean soloVerificados) {

        validarCoordenadas(latitudReferencia, longitudReferencia);
        validarRadioBusquedaMaxKm(radioBusquedaMaxKm);
        int limiteSeguro = normalizarLimite(limite);
        boolean agendaCompleto = validarFiltroAgenda(
                fechaDisponibilidad,
                horaInicioDisponibilidad,
                horaFinDisponibilidad,
                idEstadoBloqueDisponible);

        boolean filtroAgenda = agendaCompleto || filtrarDisponibleDesdeHoyCercanos;
        int limiteSql = calcularLimiteSqlGeo(limiteSeguro, filtroAgenda || soloVerificados);
        List<CandidatoGeo> candidatos = consultarCandidatosGeo(latitudReferencia, longitudReferencia, radioBusquedaMaxKm, limiteSql);

        // Intersección geo ∩ agenda; la resta "disponibilidad − bloqueo día" vive en agenda-service.
        if (agendaCompleto) {
            List<Integer> idsAgenda = agendaDisponibilidadClient.idsConBloqueDisponible(
                    fechaDisponibilidad, horaInicioDisponibilidad, horaFinDisponibilidad, idEstadoBloqueDisponible);
            candidatos = filtrarCandidatosPorIdsAgenda(candidatos, idsAgenda);
        } else if (filtrarDisponibleDesdeHoyCercanos) {
            // FUTURO: este modo filtra por "desde hoy en adelante" (agenda-service) y NO acota a "próximos 7 días".
            // Para cumplir estrictamente 7 días, agenda-service debería permitir limitar la ventana (p.ej. hastaFecha o dias=7).
            List<Integer> idsAgenda = agendaDisponibilidadClient.idsConBloqueDisponibleDesdeHoy(
                    idEstadoBloqueDisponibleParaCercanos);
            candidatos = filtrarCandidatosPorIdsAgenda(candidatos, idsAgenda);
        }

        CandidatosPreparados preparados = prepararCandidatos(candidatos, soloVerificados);
        if (preparados.candidatos().isEmpty()) {
            return List.of();
        }

        return mapearCercanosDesdeCandidatos(
                preparados.candidatos(), limiteSeguro, preparados.paseadoresPorId());
    }

    // Agrégalo dentro de tu clase PaseadorBusquedaService

    public PaseadorPerfilDTO obtenerPerfilPorId(Long idPaseador) {
        /* * 1. Buscamos en la base de datos. 
         * OJO: Cambia "usuarioRepository" y "Usuario" por el nombre 
         * real de tu repositorio y entidad (ej: Paseador, Usuario, etc.)
         */
        Paseador usuario = paseadorRepository.findById(idPaseador)
                .orElseThrow(() -> new IllegalArgumentException("Paseador no encontrado con ID: " + idPaseador));

        // 2. Metemos los datos de la base de datos en nuestro "paquete" DTO
        return PaseadorPerfilDTO.builder()
                .idUsuario(usuario.getId())
                .nombre(usuario.getPrimerNombre())
                .correo(usuario.getCorreo())
                .verificado(PaseadorVerificacionService.esVerificadoPublicamente(usuario))
                .build();
    }

    /**
     * Variante que entrega conteo real de paseadores que cumplen filtros, además de paginación.
     * No altera el contrato del endpoint existente; pensada para un endpoint nuevo.
     */
    @Transactional(readOnly = true)
    public PaseadorCercanosConConteoResponseDTO buscarCercanosConConteo(
            double latitudReferencia,
            double longitudReferencia,
            double radioBusquedaMaxKm,
            int offset,
            int limit,
            LocalDate fechaDisponibilidad,
            LocalDateTime horaInicioDisponibilidad,
            LocalDateTime horaFinDisponibilidad,
            Integer idEstadoBloqueDisponible,
            boolean soloVerificados) {

        validarCoordenadas(latitudReferencia, longitudReferencia);
        validarRadioBusquedaMaxKm(radioBusquedaMaxKm);
        int offsetSeguro = normalizarOffset(offset);
        int limitSeguro = normalizarLimite(limit);
        boolean agendaCompleto = validarFiltroAgenda(
                fechaDisponibilidad,
                horaInicioDisponibilidad,
                horaFinDisponibilidad,
                idEstadoBloqueDisponible);

        boolean filtroAgenda = agendaCompleto || filtrarDisponibleDesdeHoyCercanos;
        int limiteGeo = filtroAgenda || soloVerificados
                ? capGeoSql(Math.max(Math.max(1, maxGeoPrefetchConConteo), offsetSeguro + limitSeguro))
                : capGeoSql(Math.max(1, offsetSeguro + limitSeguro));
        List<CandidatoGeo> candidatos = consultarCandidatosGeo(latitudReferencia, longitudReferencia, radioBusquedaMaxKm, limiteGeo);

        // 2) Aplicar filtros de disponibilidad.
        if (agendaCompleto) {
            List<Integer> idsAgenda = agendaDisponibilidadClient.idsConBloqueDisponible(
                    fechaDisponibilidad, horaInicioDisponibilidad, horaFinDisponibilidad, idEstadoBloqueDisponible);
            candidatos = filtrarCandidatosPorIdsAgenda(candidatos, idsAgenda);
        } else if (filtrarDisponibleDesdeHoyCercanos) {
            List<Integer> idsAgenda = agendaDisponibilidadClient.idsConBloqueDisponibleDesdeHoy(
                    idEstadoBloqueDisponibleParaCercanos);
            candidatos = filtrarCandidatosPorIdsAgenda(candidatos, idsAgenda);
        }

        CandidatosPreparados preparados = prepararCandidatos(candidatos, soloVerificados);
        List<CandidatoGeo> candidatosFiltrados = preparados.candidatos();
        int total = candidatosFiltrados.size();
        if (total == 0) {
            return PaseadorCercanosConConteoResponseDTO.builder()
                    .totalDisponibles(0)
                    .offset(offsetSeguro)
                    .limit(limitSeguro)
                    .resultados(List.of())
                    .build();
        }

        // 3) Paginación en memoria sobre candidatos ordenados por distancia.
        int desde = Math.min(offsetSeguro, total);
        int hasta = Math.min(desde + limitSeguro, total);
        List<CandidatoGeo> pagina = candidatosFiltrados.subList(desde, hasta);

        List<PaseadorCercanoResponseDTO> resultados = mapearListaCercanos(pagina, preparados.paseadoresPorId());

        return PaseadorCercanosConConteoResponseDTO.builder()
                .totalDisponibles(total)
                .offset(offsetSeguro)
                .limit(limitSeguro)
                .resultados(resultados)
                .build();
    }

    private List<CandidatoGeo> consultarCandidatosGeo(
            double latitudReferencia,
            double longitudReferencia,
            double radioBusquedaMaxKm,
            int limiteMax) {
        int limiteSql = capGeoSql(limiteMax);
        Query query = entityManager.createNativeQuery(SQL_CERCANOS_TOP);
        query.setParameter("latRef", latitudReferencia);
        query.setParameter("lonRef", longitudReferencia);
        query.setParameter("radioMaxKm", radioBusquedaMaxKm);
        query.setParameter("limiteMax", limiteSql);

        @SuppressWarnings("unchecked")
        List<Object[]> filas = query.getResultList();

        List<CandidatoGeo> candidatos = new ArrayList<>();
        for (Object[] cols : filas) {
            long id = ((Number) cols[0]).longValue();
            double dist = ((Number) cols[1]).doubleValue();
            BigDecimal radio = cols[2] != null ? new BigDecimal(cols[2].toString()) : null;
            double lat = cols[3] != null ? ((Number) cols[3]).doubleValue() : Double.NaN;
            double lon = cols[4] != null ? ((Number) cols[4]).doubleValue() : Double.NaN;
            candidatos.add(new CandidatoGeo(id, dist, radio, lat, lon));
        }
        return candidatos;
    }

    private int calcularLimiteSqlGeo(int limiteSeguro, boolean prefetchAmplio) {
        if (!prefetchAmplio) {
            return capGeoSql(limiteSeguro);
        }
        return capGeoSql(Math.max(limiteSeguro, Math.max(1, maxGeoPrefetchConAgenda)));
    }

    private static int capGeoSql(int limite) {
        return Math.min(Math.max(limite, 1), GEO_SQL_HARD_CAP);
    }

    private List<PaseadorCercanoResponseDTO> mapearCercanosDesdeCandidatos(
            List<CandidatoGeo> candidatos,
            int limiteSeguro,
            Map<Long, Paseador> paseadoresPrecargados) {
        List<CandidatoGeo> pagina = candidatos.size() <= limiteSeguro ? candidatos : candidatos.subList(0, limiteSeguro);
        return mapearListaCercanos(pagina, paseadoresPrecargados);
    }

    private List<PaseadorCercanoResponseDTO> mapearListaCercanos(
            List<CandidatoGeo> candidatos,
            Map<Long, Paseador> paseadoresPrecargados) {
        if (candidatos.isEmpty()) {
            return List.of();
        }

        Map<Long, Paseador> entidades = paseadoresPrecargados != null ? paseadoresPrecargados : Map.of();
        List<PaseadorCercanoResponseDTO> salida = new ArrayList<>();
        for (CandidatoGeo cg : candidatos) {
            Paseador p = entidades.get(cg.idPaseador());
            if (p == null) {
                continue;
            }
            Integer tarifaMinima = tarifaRepository.findTarifaMinimaByPaseadorId(p.getId());

            salida.add(PaseadorCercanoResponseDTO.builder()
                    .idPaseador(p.getId())
                    .nombreCompleto(nombrePublico(p))
                    .fotoPerfil(p.getFotoPerfil())
                    .biografia(p.getBiografia())
                    .distanciaKm(cg.distanciaKm())
                    .radioCoberturaKm(cg.radioCoberturaKm())
                    .latitud(cg.latitud())
                    .longitud(cg.longitud())
                    .tarifaDesde(tarifaMinima != null ? tarifaMinima : 0)
                    .verificado(PaseadorVerificacionService.esVerificadoPublicamente(p))
                    .build());
        }
        return salida;
    }

    private CandidatosPreparados prepararCandidatos(List<CandidatoGeo> candidatos, boolean soloVerificados) {
        if (candidatos.isEmpty()) {
            return new CandidatosPreparados(List.of(), Map.of());
        }
        List<Long> ids = candidatos.stream().map(CandidatoGeo::idPaseador).toList();
        Map<Long, Paseador> porId = paseadorRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Paseador::getId, p -> p, (a, b) -> a, LinkedHashMap::new));
        if (!soloVerificados) {
            return new CandidatosPreparados(candidatos, porId);
        }
        List<CandidatoGeo> verificados = candidatos.stream()
                .filter(c -> {
                    Paseador p = porId.get(c.idPaseador());
                    return p != null && PaseadorVerificacionService.esVerificadoPublicamente(p);
                })
                .toList();
        return new CandidatosPreparados(verificados, porId);
    }

    private static void validarRadioBusquedaMaxKm(double radioBusquedaMaxKm) {
        if (Double.isNaN(radioBusquedaMaxKm) || Double.isInfinite(radioBusquedaMaxKm)) {
            throw new IllegalArgumentException(
                    "Parametro invalido: radioBusquedaMaxKm debe ser un numero finito (recibido=" + radioBusquedaMaxKm + ")");
        }
        if (radioBusquedaMaxKm <= RADIO_MIN_EXCLUSIVO_KM || radioBusquedaMaxKm > RADIO_MAX_KM) {
            throw new IllegalArgumentException(
                    "Parametro invalido: radioBusquedaMaxKm debe estar en (" + RADIO_MIN_EXCLUSIVO_KM + ", " + RADIO_MAX_KM + "] km"
                            + " (recibido=" + radioBusquedaMaxKm + ")");
        }
    }

    private static int normalizarLimite(int limite) {
        if (limite < LIMITE_MIN) {
            return LIMITE_MIN;
        }
        return Math.min(limite, LIMITE_MAX);
    }

    private static int normalizarOffset(int offset) {
        if (offset < OFFSET_MIN) {
            return OFFSET_MIN;
        }
        return offset;
    }

    /**
     * Valida el filtro por agenda como un "modo" independiente. Si se indica cualquier parámetro,
     * se exige el set completo para evitar combinaciones ambiguas.
     *
     * @return true si el filtro de agenda está completo y debe aplicarse.
     */
    private static boolean validarFiltroAgenda(
            LocalDate fechaDisponibilidad,
            LocalDateTime horaInicioDisponibilidad,
            LocalDateTime horaFinDisponibilidad,
            Integer idEstadoBloqueDisponible) {
        boolean hayAlguno = fechaDisponibilidad != null
                || horaInicioDisponibilidad != null
                || horaFinDisponibilidad != null
                || idEstadoBloqueDisponible != null;
        if (!hayAlguno) {
            return false;
        }
        boolean completo = fechaDisponibilidad != null
                && horaInicioDisponibilidad != null
                && horaFinDisponibilidad != null
                && idEstadoBloqueDisponible != null;
        if (!completo) {
            throw new IllegalArgumentException(
                    "Filtro agenda incompleto. Debe indicar: fechaDisponibilidad, horaInicioDisponibilidad, horaFinDisponibilidad, idEstadoBloqueDisponible"
                            + " (recibido: fechaDisponibilidad=" + fechaDisponibilidad
                            + ", horaInicioDisponibilidad=" + horaInicioDisponibilidad
                            + ", horaFinDisponibilidad=" + horaFinDisponibilidad
                            + ", idEstadoBloqueDisponible=" + idEstadoBloqueDisponible + ")");
        }
        return true;
    }

    private static List<CandidatoGeo> filtrarCandidatosPorIdsAgenda(
            List<CandidatoGeo> candidatos, List<Integer> idsAgenda) {
        Set<Long> permitidos = idsAgenda.stream().map(Integer::longValue).collect(Collectors.toSet());
        return candidatos.stream()
                .filter(c -> permitidos.contains(c.idPaseador()))
                .toList();
    }

    private static void validarCoordenadas(double lat, double lon) {
        if (lat < -90 || lat > 90) {
            throw new IllegalArgumentException(
                    "Parametro invalido: latitudReferencia debe estar entre -90 y 90 (recibido=" + lat + ")");
        }
        if (lon < -180 || lon > 180) {
            throw new IllegalArgumentException(
                    "Parametro invalido: longitudReferencia debe estar entre -180 y 180 (recibido=" + lon + ")");
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

    private record CandidatosPreparados(List<CandidatoGeo> candidatos, Map<Long, Paseador> paseadoresPorId) {}
}
