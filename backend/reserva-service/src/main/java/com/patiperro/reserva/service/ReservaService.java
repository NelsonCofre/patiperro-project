package com.patiperro.reserva.service;

import com.patiperro.reserva.dto.ReservaDtoMapper;
import com.patiperro.reserva.dto.ReservaRequestDTO;
import com.patiperro.reserva.dto.ReservaResponseDTO;
import com.patiperro.reserva.model.EstadoReserva;
import com.patiperro.reserva.model.Reserva;
import com.patiperro.reserva.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final EstadoReservaService estadoReservaService;

    public List<ReservaResponseDTO> listarTodas() {
        return reservaRepository.findAll().stream()
                .map(ReservaDtoMapper::toReservaResponse)
                .toList();
    }

    public ReservaResponseDTO obtenerPorId(Integer id) {
        return ReservaDtoMapper.toReservaResponse(obtenerEntidad(id));
    }

    @Transactional
    public ReservaResponseDTO crear(ReservaRequestDTO dto) {
        EstadoReserva estado = estadoReservaService.obtenerEntidad(dto.getIdEstadoReserva());
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
        return ReservaDtoMapper.toReservaResponse(reservaRepository.save(r));
    }

    @Transactional
    public ReservaResponseDTO actualizar(Integer id, ReservaRequestDTO dto) {
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
    public void eliminar(Integer id) {
        if (!reservaRepository.existsById(id)) {
            throw new IllegalArgumentException("Reserva no encontrada");
        }
        reservaRepository.deleteById(id);
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

    private Reserva obtenerEntidad(Integer id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
    }
}
