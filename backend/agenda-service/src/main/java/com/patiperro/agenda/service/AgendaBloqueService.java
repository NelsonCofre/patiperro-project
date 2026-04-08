package com.patiperro.agenda.service;

import com.patiperro.agenda.dto.AgendaBloqueRequestDTO;
import com.patiperro.agenda.dto.AgendaBloqueResponseDTO;
import com.patiperro.agenda.dto.AgendaDtoMapper;
import com.patiperro.agenda.model.AgendaBloque;
import com.patiperro.agenda.model.AgendaBloqueoDia;
import com.patiperro.agenda.model.DiaSemana;
import com.patiperro.agenda.model.EstadoBloque;
import com.patiperro.agenda.repository.AgendaBloqueRepository;
import com.patiperro.agenda.repository.AgendaBloqueoDiaRepository;
import com.patiperro.agenda.repository.DiaSemanaRepository;
import com.patiperro.agenda.repository.EstadoBloqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgendaBloqueService {

    private final AgendaBloqueRepository agendaBloqueRepository;
    private final AgendaBloqueoDiaRepository agendaBloqueoDiaRepository;
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

    /**
     * Bloques horarios del paseador en el rango, excluyendo fechas con bloqueo de día personal
     * ({@code agenda_bloqueo_dia}). Pensado para búsqueda de tutores y oferta efectiva.
     */
    public List<AgendaBloqueResponseDTO> listarBloquesOfertables(Integer idUsuario, LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        Set<LocalDate> diasBloqueadosPersonal = agendaBloqueoDiaRepository
                .findByIdUsuarioAndFechaBetweenOrderByFechaAsc(idUsuario, desde, hasta)
                .stream()
                .map(AgendaBloqueoDia::getFecha)
                .collect(Collectors.toSet());
        return agendaBloqueRepository
                .findByIdUsuarioAndFechaBetweenOrderByFechaAscHoraInicioAsc(idUsuario, desde, hasta)
                .stream()
                .filter(b -> !diasBloqueadosPersonal.contains(b.getFecha()))
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
        AgendaBloque bloque = obtenerEntidad(id);
        if (esEstadoReservado(bloque.getEstadoBloque())) {
            throw new IllegalStateException("No se pueden eliminar bloques que están reservados");
        }
        agendaBloqueRepository.deleteById(id);
    }

    private boolean esEstadoReservado(EstadoBloque estadoBloque) {
        if (estadoBloque == null || estadoBloque.getNombre() == null) {
            return false;
        }
        return "reservado".equalsIgnoreCase(estadoBloque.getNombre().trim());
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
