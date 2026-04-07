package com.patiperro.agenda.service;

import com.patiperro.agenda.dto.AgendaDtoMapper;
import com.patiperro.agenda.dto.DiaSemanaRequestDTO;
import com.patiperro.agenda.dto.DiaSemanaResponseDTO;
import com.patiperro.agenda.model.DiaSemana;
import com.patiperro.agenda.repository.DiaSemanaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiaSemanaService {

    private final DiaSemanaRepository repository;

    public List<DiaSemanaResponseDTO> listar() {
        return repository.findAll().stream()
                .map(AgendaDtoMapper::toDiaResponse)
                .toList();
    }

    public DiaSemanaResponseDTO obtener(Integer id) {
        return AgendaDtoMapper.toDiaResponse(obtenerEntidad(id));
    }

    @Transactional
    public DiaSemanaResponseDTO crear(DiaSemanaRequestDTO dto) {
        DiaSemana d = new DiaSemana();
        d.setIdDia(null);
        d.setNombre(dto.getNombre());
        return AgendaDtoMapper.toDiaResponse(repository.save(d));
    }

    @Transactional
    public DiaSemanaResponseDTO actualizar(Integer id, DiaSemanaRequestDTO dto) {
        DiaSemana d = obtenerEntidad(id);
        d.setNombre(dto.getNombre());
        return AgendaDtoMapper.toDiaResponse(repository.save(d));
    }

    @Transactional
    public void eliminar(Integer id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Día de la semana no encontrado");
        }
        repository.deleteById(id);
    }

    private DiaSemana obtenerEntidad(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Día de la semana no encontrado"));
    }
}
