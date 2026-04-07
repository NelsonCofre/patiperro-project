package com.patiperro.agenda.service;

import com.patiperro.agenda.dto.AgendaBloqueRequestDTO;
import com.patiperro.agenda.dto.AgendaBloqueResponseDTO;
import com.patiperro.agenda.dto.AgendaDtoMapper;
import com.patiperro.agenda.model.AgendaBloque;
import com.patiperro.agenda.model.DiaSemana;
import com.patiperro.agenda.model.EstadoBloque;
import com.patiperro.agenda.repository.AgendaBloqueRepository;
import com.patiperro.agenda.repository.DiaSemanaRepository;
import com.patiperro.agenda.repository.EstadoBloqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgendaBloqueService {

    private final AgendaBloqueRepository agendaBloqueRepository;
    private final EstadoBloqueRepository estadoBloqueRepository;
    private final DiaSemanaRepository diaSemanaRepository;

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
    public void eliminar(Integer id) {
        if (!agendaBloqueRepository.existsById(id)) {
            throw new IllegalArgumentException("Bloque de agenda no encontrado");
        }
        agendaBloqueRepository.deleteById(id);
    }

    private AgendaBloque obtenerEntidad(Integer id) {
        return agendaBloqueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bloque de agenda no encontrado"));
    }

    private void validarRangoHorario(AgendaBloqueRequestDTO dto) {
        if (dto.getHoraFinal().isBefore(dto.getHoraInicio())
                || dto.getHoraFinal().isEqual(dto.getHoraInicio())) {
            throw new IllegalArgumentException("hora_final debe ser posterior a hora_inicio");
        }
    }

    private EstadoBloque resolverEstado(Integer idEstado) {
        if (idEstado == null) {
            throw new IllegalArgumentException("estado_bloque es obligatorio (idEstado)");
        }
        return estadoBloqueRepository.findById(idEstado)
                .orElseThrow(() -> new IllegalArgumentException("Estado de bloque no encontrado: " + idEstado));
    }

    private DiaSemana resolverDia(Integer idDia) {
        if (idDia == null) {
            throw new IllegalArgumentException("dia_semana es obligatorio (idDia)");
        }
        return diaSemanaRepository.findById(idDia)
                .orElseThrow(() -> new IllegalArgumentException("Día de la semana no encontrado: " + idDia));
    }
}
