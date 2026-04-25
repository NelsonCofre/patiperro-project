package com.patiperro.reserva.service;

import com.patiperro.reserva.dto.BookingStatusPatchRequestDTO;
import com.patiperro.reserva.dto.BookingTimelineResponseDTO;
import com.patiperro.reserva.dto.CodigoReservaValidarRequestDTO;
import com.patiperro.reserva.dto.CodigoReservaValidarResponseDTO;
import com.patiperro.reserva.dto.EncuentroConfirmadoEventDTO;
import com.patiperro.reserva.dto.EstadoEncuentroResponseDTO;
import com.patiperro.reserva.dto.integracion.AgendaBloqueReservaClientDTO;
import com.patiperro.reserva.dto.AgendaBloqueResumenDTO;
import com.patiperro.reserva.dto.MascotaResumenDTO;
import com.patiperro.reserva.dto.PaseadorDecision;
import com.patiperro.reserva.dto.PaseadorResumenDTO;
import com.patiperro.reserva.dto.ReservaDtoMapper;
import com.patiperro.reserva.dto.ReservaRequestDTO;
import com.patiperro.reserva.dto.ReservaResponseDTO;
import com.patiperro.reserva.dto.MascotaPortadaUrlResponse;
import com.patiperro.reserva.dto.MascotaInternoDetalleResponseDTO;
import com.patiperro.reserva.dto.ReservaPaseadorSolicitudResponseDTO;
import com.patiperro.reserva.dto.ReservaTutorDetalleResponseDTO;
import com.patiperro.reserva.dto.BookingStatusPatchRequestDTO.TutorDecision;
import com.patiperro.reserva.dto.integracion.TutorReservaClientDTO;
import com.patiperro.reserva.model.EstadoReserva;
import com.patiperro.reserva.model.EstadoReservaCatalogo;
import com.patiperro.reserva.model.Reserva;
import com.patiperro.reserva.repository.ReservaRepository;
import com.patiperro.reserva.security.JwtService;
import com.patiperro.reserva.support.AgendaIntegracionClient;
import com.patiperro.reserva.support.MascotaIntegracionClient;
import com.patiperro.reserva.support.PaseadorIntegracionClient;
import com.patiperro.reserva.support.TutorIntegracionClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final EstadoReservaService estadoReservaService;
    private final AgendaIntegracionClient agendaIntegracionClient;
    private final MascotaIntegracionClient mascotaIntegracionClient;
    private final PaseadorIntegracionClient paseadorIntegracionClient;
    private final TutorIntegracionClient tutorIntegracionClient;
    private final JwtService jwtService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${patiperro.reserva.codigo.validacion-expira-minutos:30}")
    private int codigoExpiraMinutos;
    @Value("${patiperro.reserva.codigo.max-intentos:3}")
    private int codigoMaxIntentos;
    @Value("${patiperro.reserva.codigo.bloqueo-minutos:5}")
    private int codigoBloqueoMinutos;

    public List<ReservaResponseDTO> listarTodas() {
        return reservaRepository.findAll().stream()
                .map(ReservaDtoMapper::toReservaResponse)
                .toList();
    }

    public ReservaResponseDTO obtenerPorId(Integer id) {
        return ReservaDtoMapper.toReservaResponse(obtenerEntidad(id));
    }

    @Transactional
    public ReservaResponseDTO crear(ReservaRequestDTO dto, String rawJwt) {
        EstadoReserva estado = estadoReservaService.obtenerPorNombreIgnoreCase(EstadoReservaCatalogo.NOMBRE_SOLICITADA);
        validarTutorJwt(dto.getIdTutorUsuario(), rawJwt);

        Reserva r = new Reserva();
        r.setIdReserva(null);
        r.setIdTutorUsuario(dto.getIdTutorUsuario());
        r.setIdMascota(dto.getIdMascota());
        r.setIdAgendaBloque(dto.getIdAgendaBloque());
        r.setIdTarifa(dto.getIdTarifa());
        // Marca de servidor para trazabilidad consistente (no depender del reloj del cliente).
        r.setFechaSolicitud(LocalDateTime.now());
        r.setFechaAceptacion(dto.getFechaAceptacion());
        r.setMontoTotal(dto.getMontoTotal());
        r.setIdPago(dto.getIdPago());
        r.setEstadoReserva(estado);
        r.setFechaInicioReal(dto.getFechaInicioReal());
        r.setFechaFin(dto.getFechaFin());
        r.setCodigoEncuentro(dto.getCodigoEncuentro());
        r.setMotivoRechazo(dto.getMotivoRechazo());
        r.setDetalleRechazo(dto.getDetalleRechazo());

        Reserva saved = reservaRepository.save(r);
        agendaIntegracionClient.marcarBloqueReservado(saved.getIdAgendaBloque(), rawJwt);
        return ReservaDtoMapper.toReservaResponse(saved);
    }

    /**
     * Endpoint legado-friendly: tutorId tomado desde claim {@code tutorId} del JWT.
     */
    public List<ReservaTutorDetalleResponseDTO> listarBookingsTutorDesdeJwt(String rawJwt) {
        if (rawJwt == null || rawJwt.isBlank()) {
            throw new IllegalArgumentException("Se requiere JWT de tutor");
        }
        if (!jwtService.isTokenValid(rawJwt)) {
            throw new IllegalArgumentException("Token inválido o expirado");
        }
        Long tutorId = jwtService.extractTutorId(rawJwt);
        if (tutorId == null) {
            throw new IllegalArgumentException("JWT sin claim tutorId");
        }
        return listarDetallePorTutor(tutorId.intValue(), rawJwt);
    }

    /**
     * Timeline de estados para reserva (solicitada -> aceptada -> en curso -> finalizada).
     */
    public BookingTimelineResponseDTO obtenerTimelineReserva(Integer idReserva, String rawJwt) {
        Reserva r = obtenerEntidad(idReserva);
        validarAccesoTimeline(r, rawJwt);
        EstadoReserva estadoActual = r.getEstadoReserva();

        List<BookingTimelineResponseDTO.TimelineStepDTO> steps = List.of(
                new BookingTimelineResponseDTO.TimelineStepDTO(
                        "solicitada", "Solicitada", r.getFechaSolicitud() != null, r.getFechaSolicitud()),
                new BookingTimelineResponseDTO.TimelineStepDTO(
                        "aceptada", "Aceptada", r.getFechaAceptacion() != null, r.getFechaAceptacion()),
                new BookingTimelineResponseDTO.TimelineStepDTO(
                        "en_curso", "En Curso", r.getFechaInicioReal() != null, r.getFechaInicioReal()),
                new BookingTimelineResponseDTO.TimelineStepDTO(
                        "finalizada", "Finalizada", r.getFechaFin() != null, r.getFechaFin())
        );

        return new BookingTimelineResponseDTO(
                r.getIdReserva(),
                r.getIdTutorUsuario(),
                r.getIdMascota(),
                r.getIdAgendaBloque(),
                estadoActual != null ? estadoActual.getIdEstadoReserva() : null,
                estadoActual != null ? estadoActual.getNombreEstado() : null,
                steps
        );
    }

    @Transactional
    public ReservaResponseDTO actualizar(Integer id, ReservaRequestDTO dto, String rawJwt) {
        Reserva r = obtenerEntidad(id);
        EstadoReserva estado = estadoReservaService.obtenerEntidad(dto.getIdEstadoReserva());
        r.setIdTutorUsuario(dto.getIdTutorUsuario());
        r.setIdMascota(dto.getIdMascota());
        r.setIdAgendaBloque(dto.getIdAgendaBloque());
        r.setIdTarifa(dto.getIdTarifa());
        r.setFechaSolicitud(dto.getFechaSolicitud());
        r.setFechaAceptacion(dto.getFechaAceptacion());
        r.setMontoTotal(dto.getMontoTotal());
        r.setIdPago(dto.getIdPago());
        r.setEstadoReserva(estado);
        r.setFechaInicioReal(dto.getFechaInicioReal());
        r.setFechaFin(dto.getFechaFin());
        r.setCodigoEncuentro(dto.getCodigoEncuentro());
        r.setMotivoRechazo(dto.getMotivoRechazo());
        r.setDetalleRechazo(dto.getDetalleRechazo());
        return ReservaDtoMapper.toReservaResponse(reservaRepository.save(r));
    }

    @Transactional
    public ReservaResponseDTO aplicarDecisionPaseador(Integer idReserva, BookingStatusPatchRequestDTO dto, String rawJwt) {
        Reserva r = obtenerEntidad(idReserva);
        validarPaseadorPropietarioReserva(r, rawJwt);
        EstadoReserva actual = r.getEstadoReserva();
        EstadoReserva nuevo = resolverEstadoDestino(dto);
        validarTransicionPaseador(actual, nuevo);

        r.setEstadoReserva(nuevo);
        aplicarMarcaTiempoTransicion(r, nuevo);
        aplicarDatosDecisionPaseador(r, dto, nuevo);
        Reserva saved = reservaRepository.save(r);

        if (esEstadoRechazada(nuevo)) {
            agendaIntegracionClient.marcarBloqueDisponible(saved.getIdAgendaBloque(), rawJwt);
        }
        return ReservaDtoMapper.toReservaResponse(saved);
    }

    @Transactional
    public CodigoReservaValidarResponseDTO validarCodigoEncuentro(CodigoReservaValidarRequestDTO dto, String rawJwt) {
        Reserva r = obtenerEntidad(dto.getIdReserva());
        validarPaseadorPropietarioReserva(r, rawJwt);

        if (!esAceptada(r.getEstadoReserva()) && !esEnCurso(r.getEstadoReserva())) {
            throw new IllegalArgumentException("La reserva no está en estado válido para validar código");
        }

        if (r.getCodigoEncuentro() == null) {
            throw new IllegalArgumentException("La reserva aún no tiene código de encuentro");
        }

        if (isCodigoBloqueado(r)) {
            long segundos = ChronoUnit.SECONDS.between(LocalDateTime.now(), r.getCodigoBloqueadoHasta());
            throw new IllegalArgumentException("Intentos agotados. Reintenta en " + Math.max(1, segundos) + " segundos");
        }

        String esperado = String.format("%04d", Math.abs(r.getCodigoEncuentro() % 10000));
        if (!esperado.equals(dto.getCodigoIngresado())) {
            registrarIntentoFallidoCodigo(r);
            throw new IllegalArgumentException("Código incorrecto");
        }

        LocalDateTime base = r.getFechaAceptacion() != null ? r.getFechaAceptacion() : r.getFechaSolicitud();
        if (base == null) {
            throw new IllegalArgumentException("No se pudo validar vigencia del código");
        }
        if (LocalDateTime.now().isAfter(base.plusMinutes(Math.max(1, codigoExpiraMinutos)))) {
            throw new IllegalArgumentException("El código de encuentro expiró");
        }

        EstadoReserva enCurso = estadoReservaService.obtenerPorNombreIgnoreCase(EstadoReservaCatalogo.NOMBRE_EN_CURSO);
        r.setEstadoReserva(enCurso);
        if (r.getFechaInicioReal() == null) {
            r.setFechaInicioReal(LocalDateTime.now());
        }
        r.setCodigoIntentosFallidos(0);
        r.setCodigoBloqueadoHasta(null);
        Reserva saved = reservaRepository.save(r);

        notificarEncuentroConfirmado(saved, rawJwt);

        EstadoReserva estado = saved.getEstadoReserva();
        return new CodigoReservaValidarResponseDTO(
                saved.getIdReserva(),
                true,
                estado != null ? estado.getIdEstadoReserva() : null,
                estado != null ? estado.getNombreEstado() : null,
                saved.getFechaInicioReal()
        );
    }

    public EstadoEncuentroResponseDTO obtenerEstadoEncuentro(Integer idReserva, String rawJwt) {
        Reserva r = obtenerEntidad(idReserva);
        validarAccesoTimeline(r, rawJwt);
        EstadoReserva estado = r.getEstadoReserva();

        String estadoEncuentro = "PENDIENTE";
        String mensaje = "Pendiente de validación del código de encuentro";
        Long segundos = null;

        if (esEnCurso(estado) || esFinalizada(estado)) {
            estadoEncuentro = "CONFIRMADO";
            mensaje = "Código validado y paseo iniciado";
        } else if (isCodigoBloqueado(r)) {
            estadoEncuentro = "FALLIDO";
            segundos = ChronoUnit.SECONDS.between(LocalDateTime.now(), r.getCodigoBloqueadoHasta());
            mensaje = "Intentos agotados. Reintenta en " + Math.max(1, segundos) + " segundos";
        } else if (r.getCodigoIntentosFallidos() != null && r.getCodigoIntentosFallidos() > 0) {
            estadoEncuentro = "FALLIDO";
            mensaje = "Hay intentos fallidos recientes";
        }

        return new EstadoEncuentroResponseDTO(
                r.getIdReserva(),
                estadoEncuentro,
                estado != null ? estado.getIdEstadoReserva() : null,
                estado != null ? estado.getNombreEstado() : null,
                r.getCodigoIntentosFallidos() != null ? r.getCodigoIntentosFallidos() : 0,
                r.getCodigoBloqueadoHasta(),
                segundos != null ? Math.max(1, segundos) : null,
                mensaje
        );
    }

    /**
     * Scheduler: completa {@code codigoEncuentroExpiraEn} para reservas ACEPTADAS con PIN.
     * No requiere JWT (job interno).
     */
    @Transactional
    public void rellenarCodigoEncuentroExpiraJobItem(Integer idReserva) {
        Reserva r = obtenerEntidad(idReserva);
        if (!esAceptada(r.getEstadoReserva())) {
            return;
        }
        if (r.getCodigoEncuentro() == null) {
            return;
        }
        if (r.getCodigoEncuentroExpiraEn() != null) {
            return;
        }
        LocalDateTime base = r.getFechaAceptacion() != null ? r.getFechaAceptacion() : r.getFechaSolicitud();
        if (base == null) {
            base = LocalDateTime.now();
        }
        r.setCodigoEncuentroExpiraEn(base.plusMinutes(Math.max(1, codigoExpiraMinutos)));
        reservaRepository.save(r);
    }

    /**
     * Scheduler: cancela ACEPTADAS sin inicio real cuyo PIN venció, y libera bloque en agenda.
     */
    @Transactional
    public void cancelarAceptadaPorEncuentroVencidoJobItem(Integer idReserva) {
        Reserva r = obtenerEntidad(idReserva);
        if (!esAceptada(r.getEstadoReserva())) {
            return;
        }
        if (r.getFechaInicioReal() != null) {
            return;
        }
        if (r.getCodigoEncuentro() == null || r.getCodigoEncuentroExpiraEn() == null) {
            return;
        }
        if (!r.getCodigoEncuentroExpiraEn().isBefore(LocalDateTime.now())) {
            return;
        }

        EstadoReserva cancelada = estadoReservaService.obtenerPorNombreIgnoreCase(EstadoReservaCatalogo.NOMBRE_CANCELADA);
        r.setEstadoReserva(cancelada);
        if (r.getFechaFin() == null) {
            r.setFechaFin(LocalDateTime.now());
        }
        if (r.getMotivoRechazo() == null || r.getMotivoRechazo().isBlank()) {
            r.setMotivoRechazo("Código de encuentro expirado");
        }
        if (r.getDetalleRechazo() == null || r.getDetalleRechazo().isBlank()) {
            r.setDetalleRechazo("La reserva fue cancelada automáticamente por expiración del código de encuentro.");
        }
        reservaRepository.save(r);
        agendaIntegracionClient.marcarBloqueDisponibleInterno(r.getIdAgendaBloque());
    }

    /**
     * {@code PATCH /status}: decisión del paseador ({@link #aplicarDecisionPaseador}) o del tutor
     * ({@link #aplicarCancelacionTutor}).
     */
    @Transactional
    public ReservaResponseDTO aplicarCambioEstado(Integer idReserva, BookingStatusPatchRequestDTO dto, String rawJwt) {
        if (dto.getTutorDecision() != null) {
            return aplicarCancelacionTutor(idReserva, dto.getTutorDecision(), rawJwt);
        }
        return aplicarDecisionPaseador(idReserva, dto, rawJwt);
    }

    /**
     * El tutor anula su solicitud pendiente; libera el bloque en agenda vía endpoint interno.
     */
    @Transactional
    public ReservaResponseDTO aplicarCancelacionTutor(Integer idReserva, TutorDecision decision, String rawJwt) {
        if (decision != TutorDecision.CANCELAR_SOLICITUD) {
            throw new IllegalArgumentException("Decisión de tutor no soportada");
        }
        Reserva r = obtenerEntidad(idReserva);
        validarTutorJwt(r.getIdTutorUsuario(), rawJwt);
        EstadoReserva actual = r.getEstadoReserva();
        if (!esSolicitada(actual)) {
            throw new IllegalArgumentException("Solo puede cancelar solicitudes en estado SOLICITADA");
        }
        EstadoReserva cancelada =
                estadoReservaService.obtenerPorNombreIgnoreCase(EstadoReservaCatalogo.NOMBRE_CANCELADA);
        r.setEstadoReserva(cancelada);
        Reserva saved = reservaRepository.save(r);
        agendaIntegracionClient.marcarBloqueDisponibleInterno(saved.getIdAgendaBloque());
        return ReservaDtoMapper.toReservaResponse(saved);
    }

    @Transactional
    public void eliminar(Integer id) {
        if (!reservaRepository.existsById(id)) {
            throw new IllegalArgumentException("Reserva no encontrada");
        }
        reservaRepository.deleteById(id);
    }

    // =========================================================================
    // NUEVO MÉTODO DE VIGILANCIA: Protección de servicios comprometidos
    // =========================================================================
    /**
     * Verifica si existen reservas activas/comprometidas para una lista de bloques.
     * Esto impide que el paseador borre horarios que ya tienen un compromiso real.
     */
    public boolean tieneReservasComprometidasEnBloques(List<Integer> idsAgendaBloque) {
        if (idsAgendaBloque == null || idsAgendaBloque.isEmpty()) {
            return false; // No hay bloques para validar //
        }
        // Reserva que aún retiene el bloque: solicitada, aceptada o en curso (no rechazada ni finalizada).
        List<Integer> estadosComprometidos = List.of(
                EstadoReservaCatalogo.ID_SOLICITADA,
                EstadoReservaCatalogo.ID_ACEPTADA,
                EstadoReservaCatalogo.ID_EN_CURSO);
        
        // Ejecuta la consulta de existencia en el repositorio //
        return reservaRepository.existsByIdAgendaBloqueInAndEstadoReserva_IdEstadoReservaIn(
            idsAgendaBloque, 
            estadosComprometidos
        );
    }

    /**
     * Listado del tutor: con integración a agenda activa, ordena por inicio programado del bloque
     * (futuras primero ascendente, luego pasadas descendente) y exige JWT del tutor.
     * Sin integración, devuelve el listado sin ordenar por bloque.
     */
    public List<ReservaResponseDTO> listarPorTutor(Integer idTutorUsuario, String rawJwt) {
        List<Reserva> reservas = reservaRepository.findByIdTutorUsuario(idTutorUsuario);
        if (!agendaIntegracionClient.isEnabled()) {
            return reservas.stream()
                    .map(ReservaDtoMapper::toReservaResponse)
                    .toList();
        }
        validarTutorJwt(idTutorUsuario, rawJwt);
        LocalDateTime ahora = LocalDateTime.now();
        List<ReservaConInicio> conInicio = new ArrayList<>(reservas.size());
        for (Reserva r : reservas) {
            AgendaBloqueReservaClientDTO bloque =
                    agendaIntegracionClient.obtenerBloquePorId(r.getIdAgendaBloque(), rawJwt);
            conInicio.add(new ReservaConInicio(r, resolverInicioPaseo(r, bloque), bloque));
        }
        List<ReservaConInicio> futuras = conInicio.stream()
                .filter(x -> !x.inicio().isBefore(ahora))
                .sorted(Comparator.comparing(ReservaConInicio::inicio))
                .toList();
        List<ReservaConInicio> pasadas = conInicio.stream()
                .filter(x -> x.inicio().isBefore(ahora))
                .sorted(Comparator.comparing(ReservaConInicio::inicio).reversed())
                .toList();
        return Stream.concat(futuras.stream(), pasadas.stream())
                .map(x -> ReservaDtoMapper.toReservaResponse(x.reserva(), x.bloque()))
                .toList();
    }

    private static LocalDateTime resolverInicioPaseo(Reserva reserva, AgendaBloqueReservaClientDTO bloque) {
        if (bloque.getHoraInicio() != null) {
            return bloque.getHoraInicio();
        }
        if (bloque.getFecha() != null) {
            return bloque.getFecha().atStartOfDay();
        }
        return reserva.getFechaSolicitud();
    }

    private record ReservaConInicio(Reserva reserva, LocalDateTime inicio, AgendaBloqueReservaClientDTO bloque) {}

    public List<ReservaTutorDetalleResponseDTO> listarDetallePorTutor(Integer idTutorUsuario, String rawJwt) {
        validarTutorJwt(idTutorUsuario, rawJwt);
        return reservaRepository.findByIdTutorUsuario(idTutorUsuario).stream()
                .map(r -> toTutorDetalle(r, rawJwt))
                .sorted((a, b) -> {
                    LocalDateTime fechaA = a.getHoraInicio() != null ? a.getHoraInicio() : a.getFechaSolicitud();
                    LocalDateTime fechaB = b.getHoraInicio() != null ? b.getHoraInicio() : b.getFechaSolicitud();
                    if (fechaA == null && fechaB == null) {
                        return 0;
                    }
                    if (fechaA == null) {
                        return 1;
                    }
                    if (fechaB == null) {
                        return -1;
                    }
                    return fechaA.compareTo(fechaB);
                })
                .toList();
    }

    /**
     * Reservas en estado SOLICITADA cuyo bloque de agenda pertenece al paseador (JWT {@code paseadorId}).
     */
    public List<ReservaPaseadorSolicitudResponseDTO> listarSolicitudesPendientesPaseador(
            Integer idPaseador, String rawJwt) {
        validarPaseadorJwt(idPaseador, rawJwt);
        if (!agendaIntegracionClient.isEnabled()) {
            return List.of();
        }
        List<AgendaBloqueReservaClientDTO> bloques =
                agendaIntegracionClient.listarBloquesPorUsuario(idPaseador, rawJwt);
        if (bloques.isEmpty()) {
            return List.of();
        }
        Map<Integer, AgendaBloqueReservaClientDTO> bloquePorId = bloques.stream()
                .filter(b -> b.getIdAgenda() != null)
                .collect(Collectors.toMap(AgendaBloqueReservaClientDTO::getIdAgenda, b -> b, (a, b) -> a));

        List<Integer> idsBloque = new ArrayList<>(bloquePorId.keySet());
        List<Reserva> reservas = reservaRepository.findByIdAgendaBloqueInAndEstadoReserva_IdEstadoReserva(
                idsBloque, EstadoReservaCatalogo.ID_SOLICITADA);

        List<ReservaPaseadorSolicitudResponseDTO> salida = new ArrayList<>();
        for (Reserva r : reservas) {
            AgendaBloqueReservaClientDTO bloque = bloquePorId.get(r.getIdAgendaBloque());
            TutorReservaClientDTO tutor =
                    tutorIntegracionClient.obtenerTutor(r.getIdTutorUsuario().longValue(), rawJwt);
            salida.add(mapearSolicitudPaseador(r, bloque, tutor));
        }
        salida.sort(Comparator.comparing(
                ReservaPaseadorSolicitudResponseDTO::getFechaSolicitud,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return salida;
    }

    public List<ReservaResponseDTO> listarPorMascota(Integer idMascota) {
        return reservaRepository.findByIdMascota(idMascota).stream()
                .map(ReservaDtoMapper::toReservaResponse)
                .toList();
    }

    public List<ReservaResponseDTO> listarPorAgenda(Integer idAgendaBloque) {
        return reservaRepository.findByIdAgendaBloque(idAgendaBloque).stream()
                .map(ReservaDtoMapper::toReservaResponse)
                .toList();
    }

    public List<ReservaResponseDTO> listarPorEstado(Integer idEstadoReserva) {
        return reservaRepository.findByEstadoReserva_IdEstadoReserva(idEstadoReserva).stream()
                .map(ReservaDtoMapper::toReservaResponse)
                .toList();
    }

    private void validarPaseadorJwt(Integer idPaseador, String rawJwt) {
        if (rawJwt == null || rawJwt.isBlank()) {
            throw new IllegalArgumentException("Se requiere JWT de paseador");
        }
        if (!jwtService.isTokenValid(rawJwt)) {
            throw new IllegalArgumentException("Token inválido o expirado");
        }
        Long idJwt = jwtService.extractPaseadorId(rawJwt);
        if (idJwt == null) {
            throw new IllegalArgumentException("JWT sin claim paseadorId");
        }
        if (!idJwt.equals(idPaseador.longValue())) {
            throw new IllegalArgumentException("idPaseador no coincide con el token");
        }
    }

    private void validarPaseadorPropietarioReserva(Reserva r, String rawJwt) {
        if (!agendaIntegracionClient.isEnabled()) {
            return;
        }
        if (rawJwt == null || rawJwt.isBlank()) {
            throw new IllegalArgumentException("Se requiere JWT de paseador");
        }
        Long paseadorId = jwtService.extractPaseadorId(rawJwt);
        if (paseadorId == null) {
            throw new IllegalArgumentException("JWT sin claim paseadorId");
        }
        AgendaBloqueReservaClientDTO bloque =
                agendaIntegracionClient.obtenerBloquePorId(r.getIdAgendaBloque(), rawJwt);
        if (bloque.getIdUsuario() == null || bloque.getIdUsuario().longValue() != paseadorId) {
            throw new IllegalArgumentException("No tienes permiso para modificar esta reserva");
        }
    }

    private void validarAccesoTimeline(Reserva r, String rawJwt) {
        if (rawJwt == null || rawJwt.isBlank()) {
            throw new IllegalArgumentException("Se requiere JWT");
        }
        if (!jwtService.isTokenValid(rawJwt)) {
            throw new IllegalArgumentException("Token inválido o expirado");
        }

        Long tutorId = jwtService.extractTutorId(rawJwt);
        if (tutorId != null) {
            if (!tutorId.equals(r.getIdTutorUsuario().longValue())) {
                throw new IllegalArgumentException("No tienes permiso para ver esta reserva");
            }
            return;
        }

        Long paseadorId = jwtService.extractPaseadorId(rawJwt);
        if (paseadorId != null) {
            AgendaBloqueReservaClientDTO bloque =
                    agendaIntegracionClient.obtenerBloquePorId(r.getIdAgendaBloque(), rawJwt);
            if (bloque.getIdUsuario() == null || bloque.getIdUsuario().longValue() != paseadorId) {
                throw new IllegalArgumentException("No tienes permiso para ver esta reserva");
            }
            return;
        }

        throw new IllegalArgumentException("JWT sin claim tutorId/paseadorId");
    }

    private boolean isCodigoBloqueado(Reserva r) {
        return r.getCodigoBloqueadoHasta() != null && LocalDateTime.now().isBefore(r.getCodigoBloqueadoHasta());
    }

    private void registrarIntentoFallidoCodigo(Reserva r) {
        int current = r.getCodigoIntentosFallidos() != null ? r.getCodigoIntentosFallidos() : 0;
        int next = current + 1;
        r.setCodigoIntentosFallidos(next);
        if (next >= Math.max(1, codigoMaxIntentos)) {
            r.setCodigoBloqueadoHasta(LocalDateTime.now().plusMinutes(Math.max(1, codigoBloqueoMinutos)));
            r.setCodigoIntentosFallidos(0);
        }
        reservaRepository.save(r);
    }

    private void notificarEncuentroConfirmado(Reserva reserva, String rawJwt) {
        try {
            AgendaBloqueReservaClientDTO bloque =
                    agendaIntegracionClient.obtenerBloquePorId(reserva.getIdAgendaBloque(), rawJwt);
            TutorReservaClientDTO tutor =
                    tutorIntegracionClient.obtenerTutor(reserva.getIdTutorUsuario().longValue(), rawJwt);
            MascotaInternoDetalleResponseDTO mascota =
                    mascotaIntegracionClient.obtenerDetalleInterno(reserva.getIdMascota());

            Integer idPaseador = bloque != null ? bloque.getIdUsuario() : null;
            String mascotaNombre = mascota != null && mascota.getNombre() != null && !mascota.getNombre().isBlank()
                    ? mascota.getNombre()
                    : "Mascota #" + reserva.getIdMascota();
            String direccionInicio = tutor != null && tutor.getDireccion() != null
                    ? lineaDireccionTutor(tutor.getDireccion())
                    : "";
            EncuentroConfirmadoEventDTO event = new EncuentroConfirmadoEventDTO(
                    "ENCUENTRO_CONFIRMADO",
                    reserva.getIdReserva(),
                    reserva.getIdTutorUsuario(),
                    idPaseador,
                    "¡El paseo ha comenzado! Tu mascota está en buenas manos",
                    "Paseo iniciado correctamente",
                    mascotaNombre,
                    direccionInicio,
                    reserva.getFechaInicioReal(),
                    true,
                    true
            );

            messagingTemplate.convertAndSend("/topic/reservas/" + reserva.getIdReserva() + "/encuentro", event);
            messagingTemplate.convertAndSend("/topic/tutor/" + reserva.getIdTutorUsuario() + "/encuentro", event);
            if (idPaseador != null) {
                messagingTemplate.convertAndSend("/topic/paseador/" + idPaseador + "/encuentro", event);
            }
        } catch (RuntimeException ignored) {
            // No interrumpir el flujo transaccional por falla de push.
        }
    }

    private ReservaPaseadorSolicitudResponseDTO mapearSolicitudPaseador(
            Reserva r, AgendaBloqueReservaClientDTO bloque, TutorReservaClientDTO tutor) {
        EstadoReserva estado = r.getEstadoReserva();
        String nombreEstado = estado != null ? estado.getNombreEstado() : null;

        String fechaAgenda = "";
        String horaIni = "";
        String horaFin = "";
        if (bloque != null) {
            LocalDate f = bloque.getFecha();
            if (f != null) {
                fechaAgenda = f.toString();
            }
            horaIni = formatearHoraMinuto(bloque.getHoraInicio());
            horaFin = formatearHoraMinuto(bloque.getHoraFinal());
        }

        String tutorNombre;
        String tutorTel = "";
        String tutorCorreo = "";
        String tutorFoto = "";
        String tutorNotas = "";
        String comuna = "";
        String direccionRef = "";

        if (tutor != null) {
            tutorNombre = nombreCompletoTutor(tutor);
            if (tutor.getTelefono() != null) {
                tutorTel = tutor.getTelefono();
            }
            if (tutor.getCorreo() != null) {
                tutorCorreo = tutor.getCorreo();
            }
            if (tutor.getFotoPerfil() != null) {
                tutorFoto = tutor.getFotoPerfil();
            }
            if (tutor.getBiografia() != null) {
                tutorNotas = tutor.getBiografia();
            }
            TutorReservaClientDTO.DireccionTutorClientDTO d = tutor.getDireccion();
            if (d != null) {
                if (d.getComuna() != null) {
                    comuna = d.getComuna();
                }
                direccionRef = lineaDireccionTutor(d);
            }
        } else {
            tutorNombre = "Tutor #" + r.getIdTutorUsuario();
        }

        if (direccionRef.isBlank() && bloque != null) {
            direccionRef = "Bloque agenda #" + r.getIdAgendaBloque();
        }
        if (comuna.isBlank()) {
            comuna = "—";
        }

        String mascotaNombre = "Mascota #" + r.getIdMascota();
        String mascotaFotoUrl = null;
        String mascotaRaza = "";
        String mascotaTamano = "";
        String mascotaEdad = "";
        String mascotaPeso = "";
        String mascotaSexo = "";
        String mascotaCaracter = "";
        String mascotaCuidados = "";
        try {
            MascotaInternoDetalleResponseDTO detalleMascota = mascotaIntegracionClient.obtenerDetalleInterno(r.getIdMascota());
            if (detalleMascota != null) {
                mascotaFotoUrl = detalleMascota.getFotoPerfil();
                if (detalleMascota.getNombre() != null && !detalleMascota.getNombre().isBlank()) {
                    mascotaNombre = detalleMascota.getNombre();
                }
                mascotaRaza = detalleMascota.getRaza() != null ? detalleMascota.getRaza() : "";
                mascotaTamano = detalleMascota.getTamano() != null ? detalleMascota.getTamano() : "";
                mascotaEdad = detalleMascota.getEdad() != null ? detalleMascota.getEdad() : "";
                mascotaPeso = detalleMascota.getPeso() != null ? detalleMascota.getPeso() : "";
                mascotaSexo = detalleMascota.getSexo() != null ? detalleMascota.getSexo() : "";
                mascotaCaracter = detalleMascota.getCaracter() != null ? detalleMascota.getCaracter() : "";
                mascotaCuidados = detalleMascota.getCuidados() != null ? detalleMascota.getCuidados() : "";
            } else {
                MascotaPortadaUrlResponse portada = mascotaIntegracionClient.obtenerPortadaInterno(r.getIdMascota());
                if (portada != null) {
                    mascotaFotoUrl = portada.getUrl();
                    if (portada.getNombre() != null && !portada.getNombre().isBlank()) {
                        mascotaNombre = portada.getNombre();
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // Si mascotas no está o falla el interno, el panel del paseador sigue mostrando nombre genérico.
        }

        return new ReservaPaseadorSolicitudResponseDTO(
                r.getIdReserva(),
                r.getIdTutorUsuario(),
                r.getIdMascota(),
                r.getIdAgendaBloque(),
                r.getMontoTotal(),
                r.getFechaSolicitud(),
                nombreEstado,
                fechaAgenda,
                horaIni,
                horaFin,
                comuna,
                direccionRef,
                tutorNombre,
                tutorTel,
                tutorCorreo,
                tutorFoto,
                tutorNotas,
                mascotaFotoUrl,
                mascotaNombre,
                r.getCodigoEncuentro(),
                mascotaRaza,
                mascotaTamano,
                mascotaEdad,
                mascotaPeso,
                mascotaSexo,
                mascotaCaracter,
                mascotaCuidados);
    }

    private static String formatearHoraMinuto(LocalDateTime t) {
        if (t == null) {
            return "";
        }
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    private static String nombreCompletoTutor(TutorReservaClientDTO t) {
        StringBuilder sb = new StringBuilder();
        if (t.getPrimerNombre() != null && !t.getPrimerNombre().isBlank()) {
            sb.append(t.getPrimerNombre().trim());
        }
        if (t.getSegundoNombre() != null && !t.getSegundoNombre().isBlank()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(t.getSegundoNombre().trim());
        }
        if (t.getApellidoPaterno() != null && !t.getApellidoPaterno().isBlank()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(t.getApellidoPaterno().trim());
        }
        if (t.getApellidoMaterno() != null && !t.getApellidoMaterno().isBlank()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(t.getApellidoMaterno().trim());
        }
        String s = sb.toString().trim();
        return s.isEmpty() ? "Tutor" : s;
    }

    private static String lineaDireccionTutor(TutorReservaClientDTO.DireccionTutorClientDTO d) {
        List<String> partes = new ArrayList<>();
        if (d.getCalle() != null && !d.getCalle().isBlank()) {
            partes.add(d.getCalle().trim());
        }
        if (d.getNumeracion() != null) {
            partes.add(String.valueOf(d.getNumeracion()));
        }
        if (d.getCasaDepartamento() != null && !d.getCasaDepartamento().isBlank()) {
            partes.add(d.getCasaDepartamento().trim());
        }
        if (d.getCiudad() != null && !d.getCiudad().isBlank()) {
            partes.add(d.getCiudad().trim());
        }
        if (d.getComuna() != null && !d.getComuna().isBlank()) {
            partes.add(d.getComuna().trim());
        }
        return String.join(", ", partes);
    }

    private void validarTutorJwt(Integer idTutorUsuario, String rawJwt) {
        if (!agendaIntegracionClient.isEnabled()) {
            return;
        }
        if (rawJwt == null || rawJwt.isBlank()) {
            throw new IllegalArgumentException("Se requiere JWT de tutor para reservar bloque");
        }
        if (!jwtService.isTokenValid(rawJwt)) {
            throw new IllegalArgumentException("Token inválido o expirado");
        }
        Long tutorId = jwtService.extractTutorId(rawJwt);
        if (tutorId == null) {
            throw new IllegalArgumentException("JWT sin claim tutorId");
        }
        if (!tutorId.equals(idTutorUsuario.longValue())) {
            throw new IllegalArgumentException("idTutorUsuario no coincide con el token");
        }
    }

    private EstadoReserva resolverEstadoDestino(BookingStatusPatchRequestDTO dto) {
        if (dto.getIdEstadoReserva() != null) {
            return estadoReservaService.obtenerEntidad(dto.getIdEstadoReserva());
        }
        if (dto.getDecision() != null) {
            return buscarEstadoPorDecision(dto.getDecision());
        }
        throw new IllegalArgumentException("Debe enviar decision o idEstadoReserva");
    }

    private EstadoReserva buscarEstadoPorDecision(PaseadorDecision decision) {
        String nombre = switch (decision) {
            case ACEPTAR -> EstadoReservaCatalogo.NOMBRE_ACEPTADA;
            case RECHAZAR -> EstadoReservaCatalogo.NOMBRE_RECHAZADA;
            case INICIAR_PASEO -> EstadoReservaCatalogo.NOMBRE_EN_CURSO;
            case FINALIZAR_PASEO -> EstadoReservaCatalogo.NOMBRE_FINALIZADA;
        };
        return estadoReservaService.obtenerPorNombreIgnoreCase(nombre);
    }

    private boolean esEstadoRechazada(EstadoReserva estado) {
        if (estado == null) {
            return false;
        }
        Integer id = estado.getIdEstadoReserva();
        if (id != null && id.equals(EstadoReservaCatalogo.ID_RECHAZADA)) {
            return true;
        }
        return estado.getNombreEstado() != null
                && estado.getNombreEstado().equalsIgnoreCase(EstadoReservaCatalogo.NOMBRE_RECHAZADA);
    }

    private ReservaTutorDetalleResponseDTO toTutorDetalle(Reserva r, String rawJwt) {
        AgendaBloqueResumenDTO bloque = obtenerBloqueSeguro(r.getIdAgendaBloque(), rawJwt);
        MascotaResumenDTO mascota = obtenerMascotaSeguro(r.getIdMascota(), rawJwt);
        PaseadorResumenDTO paseador = bloque == null ? null : obtenerPaseadorSeguro(bloque.getIdUsuario());
        EstadoReserva estado = r.getEstadoReserva();

        return new ReservaTutorDetalleResponseDTO(
                r.getIdReserva(),
                r.getIdTutorUsuario(),
                r.getIdMascota(),
                mascota != null && mascota.getNombre() != null ? mascota.getNombre() : "Mascota #" + r.getIdMascota(),
                r.getIdAgendaBloque(),
                bloque != null ? bloque.getIdUsuario() : null,
                paseador != null && paseador.getNombreCompleto() != null && !paseador.getNombreCompleto().isBlank()
                        ? paseador.getNombreCompleto()
                        : bloque != null ? "Paseador #" + bloque.getIdUsuario() : "Paseador no disponible",
                bloque != null ? bloque.getFecha() : null,
                bloque != null ? bloque.getHoraInicio() : null,
                bloque != null ? bloque.getHoraFinal() : null,
                r.getMontoTotal(),
                r.getIdPago(),
                estado != null ? estado.getIdEstadoReserva() : null,
                estado != null ? estado.getNombreEstado() : null,
                r.getFechaSolicitud(),
                r.getFechaInicioReal(),
                r.getFechaFin(),
                r.getCodigoEncuentro(),
                r.getCodigoEncuentroExpiraEn(),
                r.getMotivoRechazo(),
                r.getDetalleRechazo());
    }

    private AgendaBloqueResumenDTO obtenerBloqueSeguro(Integer idAgendaBloque, String rawJwt) {
        try {
            return agendaIntegracionClient.obtenerBloque(idAgendaBloque, rawJwt);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private MascotaResumenDTO obtenerMascotaSeguro(Integer idMascota, String rawJwt) {
        try {
            return mascotaIntegracionClient.obtenerResumen(idMascota, rawJwt);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private PaseadorResumenDTO obtenerPaseadorSeguro(Integer idPaseador) {
        try {
            return paseadorIntegracionClient.obtenerResumen(idPaseador);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void validarTransicionPaseador(EstadoReserva actual, EstadoReserva nuevo) {
        if (actual.getIdEstadoReserva() != null
                && nuevo.getIdEstadoReserva() != null
                && actual.getIdEstadoReserva().equals(nuevo.getIdEstadoReserva())) {
            return;
        }
        if (esSolicitada(actual)) {
            if (esAceptada(nuevo) || esRechazada(nuevo)) {
                return;
            }
            throw new IllegalArgumentException("Desde SOLICITADA solo se permite ACEPTADA o RECHAZADA");
        }
        if (esAceptada(actual)) {
            if (esEnCurso(nuevo)) {
                return;
            }
            throw new IllegalArgumentException("Desde ACEPTADA solo se permite EN_CURSO");
        }
        if (esEnCurso(actual)) {
            if (esFinalizada(nuevo)) {
                return;
            }
            throw new IllegalArgumentException("Desde EN_CURSO solo se permite FINALIZADA");
        }
        throw new IllegalArgumentException("Transición de estado no permitida para el paseador");
    }

    private void aplicarMarcaTiempoTransicion(Reserva r, EstadoReserva nuevo) {
        LocalDateTime ahora = LocalDateTime.now();
        if (esAceptada(nuevo) && r.getFechaAceptacion() == null) {
            r.setFechaAceptacion(ahora);
            if (r.getFechaInicioReal() == null) {
                r.setFechaInicioReal(ahora);
            }
        }
        if (esEnCurso(nuevo) && r.getFechaInicioReal() == null) {
            r.setFechaInicioReal(ahora);
        }
        if (esFinalizada(nuevo) && r.getFechaFin() == null) {
            r.setFechaFin(ahora);
        }
        if ((esRechazada(nuevo) || esCancelada(nuevo)) && r.getFechaFin() == null) {
            r.setFechaFin(ahora);
        }
    }

    private void aplicarDatosDecisionPaseador(Reserva r, BookingStatusPatchRequestDTO dto, EstadoReserva nuevo) {
        if (esAceptada(nuevo)) {
            if (r.getCodigoEncuentro() == null || r.getCodigoEncuentro() <= 0) {
                r.setCodigoEncuentro(ThreadLocalRandom.current().nextInt(1000, 10000));
            }
            LocalDateTime base = r.getFechaAceptacion() != null ? r.getFechaAceptacion() : LocalDateTime.now();
            r.setCodigoEncuentroExpiraEn(base.plusMinutes(Math.max(1, codigoExpiraMinutos)));
            r.setMotivoRechazo(null);
            r.setDetalleRechazo(null);
            return;
        }
        if (esRechazada(nuevo)) {
            String motivo = dto.getMotivoRechazo();
            String detalle = dto.getDetalleRechazo();
            r.setMotivoRechazo(motivo != null && !motivo.isBlank() ? motivo.trim() : null);
            r.setDetalleRechazo(detalle != null && !detalle.isBlank() ? detalle.trim() : null);
        }
    }

    private static boolean coincideIdONombre(EstadoReserva e, int idEsperado, String nombreEsperado) {
        if (e == null) {
            return false;
        }
        Integer id = e.getIdEstadoReserva();
        if (id != null && id.equals(idEsperado)) {
            return true;
        }
        String n = e.getNombreEstado();
        return n != null && n.trim().equalsIgnoreCase(nombreEsperado);
    }

    private static boolean esSolicitada(EstadoReserva e) {
        return coincideIdONombre(e, EstadoReservaCatalogo.ID_SOLICITADA, EstadoReservaCatalogo.NOMBRE_SOLICITADA);
    }

    private static boolean esAceptada(EstadoReserva e) {
        return coincideIdONombre(e, EstadoReservaCatalogo.ID_ACEPTADA, EstadoReservaCatalogo.NOMBRE_ACEPTADA);
    }

    private static boolean esRechazada(EstadoReserva e) {
        return coincideIdONombre(e, EstadoReservaCatalogo.ID_RECHAZADA, EstadoReservaCatalogo.NOMBRE_RECHAZADA);
    }

    private static boolean esEnCurso(EstadoReserva e) {
        return coincideIdONombre(e, EstadoReservaCatalogo.ID_EN_CURSO, EstadoReservaCatalogo.NOMBRE_EN_CURSO);
    }

    private static boolean esFinalizada(EstadoReserva e) {
        return coincideIdONombre(e, EstadoReservaCatalogo.ID_FINALIZADA, EstadoReservaCatalogo.NOMBRE_FINALIZADA);
    }

    private static boolean esCancelada(EstadoReserva e) {
        return coincideIdONombre(e, EstadoReservaCatalogo.ID_CANCELADA, EstadoReservaCatalogo.NOMBRE_CANCELADA);
    }

    private Reserva obtenerEntidad(Integer id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
    }
}
