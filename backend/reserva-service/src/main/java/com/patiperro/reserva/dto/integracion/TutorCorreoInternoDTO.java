package com.patiperro.reserva.dto.integracion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TutorCorreoInternoDTO(String correo) {
}
