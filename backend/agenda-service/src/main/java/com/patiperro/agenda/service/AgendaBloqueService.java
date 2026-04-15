package com.patiperro.agenda.service;

import com.patiperro.agenda.dto.AgendaBloqueRequestDTO;
import com.patiperro.agenda.dto.AgendaBloqueResponseDTO;
import com.patiperro.agenda.dto.AgendaBloqueSerieMensualRequestDTO;
import com.patiperro.agenda.dto.AgendaBloqueSerieMensualResponseDTO;
import com.patiperro.agenda.dto.AgendaDtoMapper;
import com.patiperro.agenda.model.AgendaBloque;
import com.patiperro.agenda.model.DiaSemana;
import com.patiperro.agenda.model.EstadoBloque;
import com.patiperro.agenda.repository.AgendaBloqueRepository;
import com.patiperro.agenda.repository.AgendaBloqueoDiaRepository;
import com.patiperro.agenda.repository.DiaSemanaRepository;
import com.patiperro.agenda.repository.EstadoBloqueRepository;
import com.patiperro.agenda.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgendaBloqueService {

    private final AgendaBloqueRepository agendaBloqueRepository;
    private final AgendaBloqueoDiaRepository agendaBloqueoDiaRepository;
    private final EstadoBloqueRepository estadoBloqueRepository;
    private final DiaSemanaRepository diaSemanaRepository;
    private final JwtService jwtService;

    @Value("${patiperro.agenda.estado-bloque.nombre-disponible:Disponible}")
    private String nombreEstadoDisponible;

    @Value("${patiperro.agenda.estado-bloque.nombre-reservado:Reservado}")
    private String nombreEstadoReservado;

    public List<AgendaBloqueResponseDTO> listar() {
        return agendaBloqueRepository.findAll().stream()
                .map(AgendaDtoMapper::toBloqueResponse)
                .toList();
    }

    public List<AgendaBloqueResponseDTO> listarPorUsuario(Integer idUsuario) {
        return agendaBloqueRepository.findByIdUsuario(idUsuario).stream()
                .map(AgendaDtoMapper::toBloqueResponse)
                .toList();
    }

    /**
     * Bloques horarios del paseador en el rango (oferta para tutores).
     * Excluye fechas con bloqueo personal de día completo ({@code agenda_bloqueo_dia}).
     */
    public List<AgendaBloqueResponseDTO> listarBloquesOfertables(Integer idUsuario, LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }

        Set<LocalDate> diasBloqueadosPersonal = agendaBloqueoDiaRepository
                .findByIdUsuarioAndFechaBetweenOrderByFechaAsc(idUsuario, desde, hasta)
                .stream()
                .map(bd -> bd.getFecha())
                .collect(Collectors.toSet());

        return agendaBloqueRepository
                .findByIdUsuarioAndFechaBetweenOrderByFechaAscHoraInicioAsc(idUsuario, desde, hasta)
                .stream()
                .filter(b -> !diasBloqueadosPersonal.contains(b.getFecha()))
                .map(AgendaDtoMapper::toBloqueResponse)
                .toList();
    }

    /**
     * Paseadores ({@code id_usuario}) con bloque disponible en la franja.
     */
    public List<Integer> buscarIdUsuariosDisponiblesEnFranja(
            LocalDate fecha,
            LocalDateTime inicioBuscado,
            LocalDateTime finBuscado,
            Integer idEstadoDisponible) {

        validarParametrosBusquedaFranja(fecha, inicioBuscado, finBuscado);

        if (idEstadoDisponible == null) {
            throw new IllegalArgumentException("idEstadoDisponible es obligatorio");
        }

        return agendaBloqueRepository.findIdUsuariosConBloqueDisponibleEnFranja(
                fecha, inicioBuscado, finBuscado, idEstadoDisponible);
    }

    /**
     * Paseadores con al menos un bloque disponible desde {@code desdeFecha} (típicamente hoy en el servidor).
     */
    public List<Integer> buscarIdUsuariosDisponiblesDesdeFecha(LocalDate desdeFecha, Integer idEstadoDisponible) {
        if (desdeFecha == null) {
            throw new IllegalArgumentException("desdeFecha es obligatoria");
        }
        if (idEstadoDisponible == null) {
            throw new IllegalArgumentException("idEstadoDisponible es obligatorio");
        }
        return agendaBloqueRepository.findIdUsuariosConBloqueDisponibleDesdeFecha(desdeFecha, idEstadoDisponible);
    }

    public AgendaBloqueResponseDTO obtener(Integer id) {
        return AgendaDtoMapper.toBloqueResponse(obtenerEntidad(id));
    }

    @Transactional
    public AgendaBloqueResponseDTO crear(AgendaBloqueRequestDTO dto) {
        validarRangoHorario(dto);
        AgendaBloque nuevo = new AgendaBloque();
        nuevo.setIdAgenda(null);
        nuevo.setIdUsuario(dto.getIdUsuario());
        nuevo.setHoraInicio(dto.getHoraInicio());
        nuevo.setHoraFinal(dto.getHoraFinal());
        nuevo.setFecha(dto.getFecha());
        nuevo.setEstadoBloque(resolverEstado(dto.getEstadoBloque().getIdEstado()));
        nuevo.setDiaSemana(resolverDia(dto.getDiaSemana().getIdDia()));
        return AgendaDtoMapper.toBloqueResponse(agendaBloqueRepository.save(nuevo));
    }

    @Transactional
    public AgendaBloqueResponseDTO actualizar(Integer id, AgendaBloqueRequestDTO dto) {
        AgendaBloque existente = obtenerEntidad(id);
        validarRangoHorario(dto);
        existente.setIdUsuario(dto.getIdUsuario());
        existente.setHoraInicio(dto.getHoraInicio());
        existente.setHoraFinal(dto.getHoraFinal());
        existente.setFecha(dto.getFecha());
        existente.setEstadoBloque(resolverEstado(dto.getEstadoBloque().getIdEstado()));
        existente.setDiaSemana(resolverDia(dto.getDiaSemana().getIdDia()));
        return AgendaDtoMapper.toBloqueResponse(agendaBloqueRepository.save(existente));
    }

    @Transactional
    public AgendaBloqueResponseDTO marcarReservado(Integer idAgenda, String rawJwt) {
        requireJwt(rawJwt);
        if (jwtService.extractTutorId(rawJwt) == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Se requiere JWT de tutor para reservar bloque");
        }
        AgendaBloque bloque = obtenerEntidad(idAgenda);
        EstadoBloque disponible = estadoPorNombre(nombreEstadoDisponible);
        EstadoBloque reservado = estadoPorNombre(nombreEstadoReservado);
        Integer actual = bloque.getEstadoBloque() != null ? bloque.getEstadoBloque().getIdEstado() : null;
        if (actual == null || !actual.equals(disponible.getIdEstado())) {
            throw new IllegalStateException("El bloque no está disponible");
        }
        bloque.setEstadoBloque(reservado);
        return AgendaDtoMapper.toBloqueResponse(agendaBloqueRepository.save(bloque));
    }

    @Transactional
    public AgendaBloqueResponseDTO marcarDisponible(Integer idAgenda, String rawJwt) {
        requireJwt(rawJwt);
        Long paseadorId = jwtService.extractPaseadorId(rawJwt);
        if (paseadorId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Se requiere JWT de paseador para liberar bloque");
        }
        AgendaBloque bloque = obtenerEntidad(idAgenda);
        if (!bloque.getIdUsuario().equals(paseadorId.intValue())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el paseador dueño del bloque puede liberarlo");
        }

        EstadoBloque disponible = estadoPorNombre(nombreEstadoDisponible);
        EstadoBloque reservado = estadoPorNombre(nombreEstadoReservado);
        Integer actual = bloque.getEstadoBloque() != null ? bloque.getEstadoBloque().getIdEstado() : null;

        if (actual != null && actual.equals(disponible.getIdEstado())) {
            return AgendaDtoMapper.toBloqueResponse(bloque);
        }
        if (actual == null || !actual.equals(reservado.getIdEstado())) {
            throw new IllegalStateException("El bloque no está en estado reservado");
        }
        bloque.setEstadoBloque(disponible);
        return AgendaDtoMapper.toBloqueResponse(agendaBloqueRepository.save(bloque));
    }

    @Transactional
    public void eliminar(Integer id) {
        AgendaBloque bloque = obtenerEntidad(id);
        if (esEstadoReservado(bloque.getEstadoBloque())) {
            throw new IllegalStateException("No se pueden eliminar bloques que están reservados");
        }
        agendaBloqueRepository.deleteById(id);
    }

    @Transactional
    public AgendaBloqueSerieMensualResponseDTO crearSerieMensualEnMes(AgendaBloqueSerieMensualRequestDTO dto) {
        LocalDate fechaSem = dto.getFechaSemilla();
        if (!fechaSem.equals(dto.getHoraInicio().toLocalDate())
                || !fechaSem.equals(dto.getHoraFinal().toLocalDate())) {
            throw new IllegalArgumentException("fechaSemilla debe coincidir con las fechas en horaInicio y horaFinal");
        }
        LocalTime tInicio = dto.getHoraInicio().toLocalTime();
        LocalTime tFin = dto.getHoraFinal().toLocalTime();
        if (!tFin.isAfter(tInicio)) {
            throw new IllegalArgumentException("hora_final debe ser posterior a hora_inicio");
        }
        EstadoBloque estado = resolverEstado(dto.getEstadoBloque().getIdEstado());
        if (esEstadoReservado(estado)) {
            throw new IllegalArgumentException("No se puede crear una serie en estado reservado");
        }

        LocalDate hoy = LocalDate.now();
        YearMonth ym = YearMonth.from(fechaSem);
        LocalDate primerDia = ym.atDay(1);
        LocalDate ultimoDia = ym.atEndOfMonth();
        DayOfWeek diaObjetivo = fechaSem.getDayOfWeek();

        List<DiaSemana> catalogoDias = diaSemanaRepository.findAll();
        DiaSemana diaSemana = resolverDiaJava(catalogoDias, diaObjetivo);

        List<AgendaBloque> relevantes = new ArrayList<>(
                agendaBloqueRepository.findByIdUsuarioAndFechaBetweenOrderByFechaAscHoraInicioAsc(
                        dto.getIdUsuario(), primerDia, ultimoDia));

        int omitidosPasado = 0;
        int omitidosSolape = 0;
        List<AgendaBloqueResponseDTO> creados = new ArrayList<>();

        for (LocalDate fecha = primerDia; !fecha.isAfter(ultimoDia); fecha = fecha.plusDays(1)) {
            if (fecha.getDayOfWeek() != diaObjetivo) continue;
            if (fecha.isBefore(hoy)) {
                omitidosPasado++;
                continue;
            }
            LocalDateTime hi = LocalDateTime.of(fecha, tInicio);
            LocalDateTime hf = LocalDateTime.of(fecha, tFin);
            if (haySolapeHorario(relevantes, fecha, hi, hf)) {
                omitidosSolape++;
                continue;
            }
            AgendaBloque nuevo = new AgendaBloque();
            nuevo.setIdAgenda(null);
            nuevo.setIdUsuario(dto.getIdUsuario());
            nuevo.setHoraInicio(hi);
            nuevo.setHoraFinal(hf);
            nuevo.setFecha(fecha);
            nuevo.setEstadoBloque(estado);
            nuevo.setDiaSemana(diaSemana);
            AgendaBloque guardado = agendaBloqueRepository.save(nuevo);
            relevantes.add(guardado);
            creados.add(AgendaDtoMapper.toBloqueResponse(guardado));
        }

        return AgendaBloqueSerieMensualResponseDTO.builder()
                .creados(creados.size())
                .omitidosPasado(omitidosPasado)
                .omitidosSolape(omitidosSolape)
                .bloques(creados)
                .build();
    }

    private void requireJwt(String rawJwt) {
        if (rawJwt == null || rawJwt.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Se requiere Authorization Bearer");
        }
        if (!jwtService.isTokenValid(rawJwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido o expirado");
        }
    }

    private EstadoBloque estadoPorNombre(String nombre) {
        return estadoBloqueRepository.findByNombreIgnoreCase(nombre)
                .orElseThrow(() -> new IllegalStateException("No existe estado_bloque con nombre: " + nombre));
    }

    private static boolean haySolapeHorario(List<AgendaBloque> bloques, LocalDate fecha, LocalDateTime inicio, LocalDateTime fin) {
        for (AgendaBloque b : bloques) {
            if (!b.getFecha().equals(fecha)) continue;
            if (inicio.isBefore(b.getHoraFinal()) && fin.isAfter(b.getHoraInicio())) return true;
        }
        return false;
    }

    private DiaSemana resolverDiaJava(List<DiaSemana> catalogo, DayOfWeek dia) {
        String esperado = nombreDiaSemanaEsperado(dia);
        String clave = normalizarNombreDia(esperado);
        return catalogo.stream()
                .filter(ds -> normalizarNombreDia(ds.getNombre()).equals(clave))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Catálogo dia_semana incompleto: falta " + esperado));
    }

    private static String nombreDiaSemanaEsperado(DayOfWeek d) {
        return switch (d) {
            case MONDAY -> "Lunes";
            case TUESDAY -> "Martes";
            case WEDNESDAY -> "Miercoles";
            case THURSDAY -> "Jueves";
            case FRIDAY -> "Viernes";
            case SATURDAY -> "Sabado";
            case SUNDAY -> "Domingo";
        };
    }

    private static String normalizarNombreDia(String nombre) {
        if (nombre == null) return "";
        String n = Normalizer.normalize(nombre.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}+", "");
    }

    private boolean esEstadoReservado(EstadoBloque estadoBloque) {
        if (estadoBloque == null || estadoBloque.getNombre() == null) return false;
        return "reservado".equalsIgnoreCase(estadoBloque.getNombre().trim());
    }

    private AgendaBloque obtenerEntidad(Integer id) {
        return agendaBloqueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bloque de agenda no encontrado"));
    }

    private void validarParametrosBusquedaFranja(LocalDate fecha, LocalDateTime inicioBuscado, LocalDateTime finBuscado) {
        if (finBuscado.isBefore(inicioBuscado) || finBuscado.isEqual(inicioBuscado)) {
            throw new IllegalArgumentException("La hora de término debe ser posterior a la hora de inicio");
        }
        if (!fecha.isEqual(inicioBuscado.toLocalDate()) || !fecha.isEqual(finBuscado.toLocalDate())) {
            throw new IllegalArgumentException("La fecha debe coincidir con el día de inicio y fin");
        }
    }

    private void validarRangoHorario(AgendaBloqueRequestDTO dto) {
        if (dto.getHoraFinal().isBefore(dto.getHoraInicio())
                || dto.getHoraFinal().isEqual(dto.getHoraInicio())) {
            throw new IllegalArgumentException("hora_final debe ser posterior a hora_inicio");
        }
    }

    private EstadoBloque resolverEstado(Integer idEstado) {
        if (idEstado == null) throw new IllegalArgumentException("estado_bloque es obligatorio");
        return estadoBloqueRepository.findById(idEstado)
                .orElseThrow(() -> new IllegalArgumentException("Estado de bloque no encontrado: " + idEstado));
    }

    private DiaSemana resolverDia(Integer idDia) {
        if (idDia == null) throw new IllegalArgumentException("dia_semana es obligatorio");
        return diaSemanaRepository.findById(idDia)
                .orElseThrow(() -> new IllegalArgumentException("Día de la semana no encontrado: " + idDia));
    }
}