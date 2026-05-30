package com.patiperro.chat.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class ConversacionResponseDTO {
	Integer id;
	Integer idReserva;
	Instant fechaCreacion;
	Integer idEstadoChat;
	String nombreEstadoChat;
}
