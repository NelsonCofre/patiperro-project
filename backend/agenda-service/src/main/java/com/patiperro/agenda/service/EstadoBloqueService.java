package com.patiperro.agenda.service;

import com.patiperro.agenda.dto.AgendaDtoMapper;
import com.patiperro.agenda.dto.EstadoBloqueRequestDTO;
import com.patiperro.agenda.dto.EstadoBloqueResponseDTO;
import com.patiperro.agenda.model.EstadoBloque;
import com.patiperro.agenda.repository.EstadoBloqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EstadoBloqueService {

    private final EstadoBloqueRepository repository;

    public List<EstadoBloqueResponseDTO> listar() {
        return repository.findAll().stream()
                .map(AgendaDtoMapper::toEstadoResponse)
                .toList();
    }

    public EstadoBloqueResponseDTO obtener(Integer id) {
        return AgendaDtoMapper.toEstadoResponse(obtenerEntidad(id));
    }

    @Transactional
    public EstadoBloqueResponseDTO crear(EstadoBloqueRequestDTO dto) {
        EstadoBloque e = new EstadoBloque();
        e.setIdEstado(null);
        e.setNombre(dto.getNombre());
        return AgendaDtoMapper.toEstadoResponse(repository.save(e));
    }

    @Transactional
    public EstadoBloqueResponseDTO actualizar(Integer id, EstadoBloqueRequestDTO dto) {
        EstadoBloque e = obtenerEntidad(id);
        e.setNombre(dto.getNombre());
        return AgendaDtoMapper.toEstadoResponse(repository.save(e));
    }

    @Transactional
    public void eliminar(Integer id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Estado de bloque no encontrado");
        }
        repository.deleteById(id);
    }

    private EstadoBloque obtenerEntidad(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estado de bloque no encontrado"));
    }
}
