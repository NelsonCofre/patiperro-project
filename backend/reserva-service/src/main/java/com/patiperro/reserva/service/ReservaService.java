package com.patiperro.reserva.service;

import com.patiperro.reserva.dto.BookingStatusPatchRequestDTO;
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

import java.util.List;

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
        // IDs de estados comprometidos (ej: 2=Pagado, 3=Confirmado, 4=En Curso) //
        List<Integer> estadosComprometidos = List.of(2, 3, 4); 
        
        // Ejecuta la consulta de existencia en el repositorio //
        return reservaRepository.existsByIdAgendaBloqueInAndEstadoReserva_IdEstadoReservaIn(
            idsAgendaBloque, 
            estadosComprometidos
        );
    }

    public List<ReservaResponseDTO> listarPorTutor(Integer idTutorUsuario) {
        return reservaRepository.findByIdTutorUsuario(idTutorUsuario).stream()
                .map(ReservaDtoMapper::toReservaResponse)
                .toList();
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
        String nombre = decision == PaseadorDecision.ACEPTAR
                ? EstadoReservaCatalogo.NOMBRE_ACEPTADA
                : EstadoReservaCatalogo.NOMBRE_RECHAZADA;
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
        if (actual.getIdEstadoReserva().equals(nuevo.getIdEstadoReserva())) {
            return;
        }
        String an = actual.getNombreEstado() == null ? "" : actual.getNombreEstado().toLowerCase();
        boolean desdePendiente = an.contains("pend") || an.contains("solicit") || an.contains("espera");
        if (!desdePendiente) {
            throw new IllegalArgumentException(
                    "Solo se puede aceptar o rechazar desde estado pendiente o solicitada");
        }
    }

    private Reserva obtenerEntidad(Integer id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
    }
}
