package com.patiperro.agenda.service;

import com.patiperro.agenda.dto.AgendaBloqueoDiaRequestDTO;
import com.patiperro.agenda.dto.AgendaBloqueoDiaResponseDTO;
import com.patiperro.agenda.dto.AgendaDtoMapper;
import com.patiperro.agenda.model.AgendaBloqueoDia;
import com.patiperro.agenda.repository.AgendaBloqueoDiaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgendaBloqueoDiaService {

    private final AgendaBloqueoDiaRepository repository;

    public List<AgendaBloqueoDiaResponseDTO> listar() {
        return repository.findAll().stream()
                .map(AgendaDtoMapper::toBloqueoDiaResponse)
                .toList();
    }

    public List<AgendaBloqueoDiaResponseDTO> listarPorUsuario(Integer idUsuario) {
        return repository.findByIdUsuarioOrderByFechaAsc(idUsuario).stream()
                .map(AgendaDtoMapper::toBloqueoDiaResponse)
                .toList();
    }

    public List<AgendaBloqueoDiaResponseDTO> listarPorUsuarioYRango(Integer idUsuario, LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        return repository.findByIdUsuarioAndFechaBetweenOrderByFechaAsc(idUsuario, desde, hasta).stream()
                .map(AgendaDtoMapper::toBloqueoDiaResponse)
                .toList();
    }

    public AgendaBloqueoDiaResponseDTO obtener(Integer id) {
        return AgendaDtoMapper.toBloqueoDiaResponse(obtenerEntidad(id));
    }

    @Transactional
    public AgendaBloqueoDiaResponseDTO crear(AgendaBloqueoDiaRequestDTO dto) {
        validarFechaNoPasada(dto.getFecha());
        if (repository.existsByIdUsuarioAndFecha(dto.getIdUsuario(), dto.getFecha())) {
            throw new IllegalArgumentException("Ya existe un bloqueo de día para ese paseador en la fecha indicada");
        }
        AgendaBloqueoDia n = new AgendaBloqueoDia();
        n.setIdBloqueo(null);
        n.setIdUsuario(dto.getIdUsuario());
        n.setFecha(dto.getFecha());
        n.setMotivo(dto.getMotivo());
        n.setCreadoEn(null);
        return AgendaDtoMapper.toBloqueoDiaResponse(repository.save(n));
    }

    @Transactional
    public AgendaBloqueoDiaResponseDTO actualizar(Integer id, AgendaBloqueoDiaRequestDTO dto) {
        AgendaBloqueoDia e = obtenerEntidad(id);
        validarFechaNoPasada(dto.getFecha());
        boolean cambiaParUsuarioFecha = !e.getIdUsuario().equals(dto.getIdUsuario())
                || !e.getFecha().equals(dto.getFecha());
        if (cambiaParUsuarioFecha
                && repository.existsByIdUsuarioAndFecha(dto.getIdUsuario(), dto.getFecha())) {
            throw new IllegalArgumentException("Ya existe un bloqueo de día para ese paseador en la fecha indicada");
        }
        e.setIdUsuario(dto.getIdUsuario());
        e.setFecha(dto.getFecha());
        e.setMotivo(dto.getMotivo());
        return AgendaDtoMapper.toBloqueoDiaResponse(repository.save(e));
    }

    @Transactional
    public void eliminar(Integer id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Bloqueo de día no encontrado");
        }
        repository.deleteById(id);
    }

    /**
     * Quita el bloqueo personal del paseador para una fecha concreta (desbloqueo por día).
     */
    @Transactional
    public void eliminarPorUsuarioYFecha(Integer idUsuario, LocalDate fecha) {
        AgendaBloqueoDia e = repository
                .findByIdUsuarioAndFecha(idUsuario, fecha)
                .orElseThrow(() -> new IllegalArgumentException("Bloqueo de día no encontrado"));
        repository.delete(e);
    }

    public boolean existeBloqueo(Integer idUsuario, LocalDate fecha) {
        return repository.existsByIdUsuarioAndFecha(idUsuario, fecha);
    }

    private AgendaBloqueoDia obtenerEntidad(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bloqueo de día no encontrado"));
    }

    private void validarFechaNoPasada(LocalDate fecha) {
        if (fecha.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("No puedes bloquear fechas pasadas");
        }
    }
}
