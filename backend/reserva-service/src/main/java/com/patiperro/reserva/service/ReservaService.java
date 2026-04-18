package com.patiperro.reserva.service;

import com.patiperro.reserva.dto.BookingStatusPatchRequestDTO;
import com.patiperro.reserva.dto.integracion.AgendaBloqueReservaClientDTO;
import com.patiperro.reserva.dto.PaseadorDecision;
import com.patiperro.reserva.dto.ReservaDtoMapper;
import com.patiperro.reserva.dto.ReservaRequestDTO;
import com.patiperro.reserva.dto.ReservaResponseDTO;
import com.patiperro.reserva.model.EstadoReserva;
import com.patiperro.reserva.model.EstadoReservaCatalogo;
import com.patiperro.reserva.model.Reserva;
import com.patiperro.reserva.repository.ReservaRepository;
import com.patiperro.reserva.security.JwtService;
import com.patiperro.reserva.support.AgendaIntegracionClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final EstadoReservaService estadoReservaService;
    private final AgendaIntegracionClient agendaIntegracionClient;
    private final JwtService jwtService;

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
        r.setFechaSolicitud(dto.getFechaSolicitud());
        r.setFechaAceptacion(dto.getFechaAceptacion());
        r.setMontoTotal(dto.getMontoTotal());
        r.setIdPago(dto.getIdPago());
        r.setEstadoReserva(estado);
        r.setFechaInicioReal(dto.getFechaInicioReal());
        r.setFechaFin(dto.getFechaFin());
        r.setCodigoEncuentro(dto.getCodigoEncuentro());

        Reserva saved = reservaRepository.save(r);
        agendaIntegracionClient.marcarBloqueReservado(saved.getIdAgendaBloque(), rawJwt);
        return ReservaDtoMapper.toReservaResponse(saved);
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
        return ReservaDtoMapper.toReservaResponse(reservaRepository.save(r));
    }

    @Transactional
    public ReservaResponseDTO aplicarDecisionPaseador(Integer idReserva, BookingStatusPatchRequestDTO dto, String rawJwt) {
        Reserva r = obtenerEntidad(idReserva);
        EstadoReserva actual = r.getEstadoReserva();
        EstadoReserva nuevo = resolverEstadoDestino(dto);
        validarTransicionPaseador(actual, nuevo);

        r.setEstadoReserva(nuevo);
        aplicarMarcaTiempoTransicion(r, nuevo);
        Reserva saved = reservaRepository.save(r);

        if (esEstadoRechazada(nuevo)) {
            agendaIntegracionClient.marcarBloqueDisponible(saved.getIdAgendaBloque(), rawJwt);
        }
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
        }
        if (esEnCurso(nuevo) && r.getFechaInicioReal() == null) {
            r.setFechaInicioReal(ahora);
        }
        if (esFinalizada(nuevo) && r.getFechaFin() == null) {
            r.setFechaFin(ahora);
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

    private Reserva obtenerEntidad(Integer id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
    }
}
