package com.patiperro.agenda.dto;

import com.patiperro.agenda.model.AgendaBloque;
import com.patiperro.agenda.model.DiaSemana;
import com.patiperro.agenda.model.EstadoBloque;

public final class AgendaDtoMapper {

    private AgendaDtoMapper() {}

    public static EstadoBloqueResponseDTO toEstadoResponse(EstadoBloque e) {
        if (e == null) {
            return null;
        }
        return new EstadoBloqueResponseDTO(e.getIdEstado(), e.getNombre());
    }

    public static DiaSemanaResponseDTO toDiaResponse(DiaSemana d) {
        if (d == null) {
            return null;
        }
        return new DiaSemanaResponseDTO(d.getIdDia(), d.getNombre());
    }

    public static AgendaBloqueResponseDTO toBloqueResponse(AgendaBloque b) {
        return new AgendaBloqueResponseDTO(
                b.getIdAgenda(),
                b.getIdUsuario(),
                b.getHoraInicio(),
                b.getHoraFinal(),
                b.getFecha(),
                toEstadoResponse(b.getEstadoBloque()),
                toDiaResponse(b.getDiaSemana()));
    }
}
