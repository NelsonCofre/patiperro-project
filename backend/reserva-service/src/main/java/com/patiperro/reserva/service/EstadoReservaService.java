package com.patiperro.reserva.service;

import com.patiperro.reserva.dto.EstadoReservaRequestDTO;
import com.patiperro.reserva.dto.EstadoReservaResponseDTO;
import com.patiperro.reserva.dto.ReservaDtoMapper;
import com.patiperro.reserva.model.EstadoReserva;
import com.patiperro.reserva.repository.EstadoReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EstadoReservaService {

    private final EstadoReservaRepository repository;

    public List<EstadoReservaResponseDTO> listarTodos() {
        return repository.findAll().stream()
                .map(ReservaDtoMapper::toEstadoResponse)
                .toList();
    }

    public EstadoReservaResponseDTO obtenerPorId(Integer id) {
        return ReservaDtoMapper.toEstadoResponse(obtenerEntidad(id));
    }

    @Transactional
    public EstadoReservaResponseDTO crear(EstadoReservaRequestDTO dto) {
        EstadoReserva e = new EstadoReserva();
        e.setIdEstadoReserva(null);
        e.setNombreEstado(dto.getNombreEstado());
        return ReservaDtoMapper.toEstadoResponse(repository.save(e));
    }

    @Transactional
    public EstadoReservaResponseDTO actualizar(Integer id, EstadoReservaRequestDTO dto) {
        EstadoReserva e = obtenerEntidad(id);
        e.setNombreEstado(dto.getNombreEstado());
        return ReservaDtoMapper.toEstadoResponse(repository.save(e));
    }

    @Transactional
    public void eliminar(Integer id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Estado de reserva no encontrado");
        }
        repository.deleteById(id);
    }

    public EstadoReserva obtenerEntidad(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estado de reserva no encontrado"));
    }

    public EstadoReserva obtenerPorNombreIgnoreCase(String nombreEstado) {
        return repository.findByNombreEstadoIgnoreCase(nombreEstado)
                .orElseThrow(() -> new IllegalArgumentException("Estado de reserva no encontrado: " + nombreEstado));
    }
}
