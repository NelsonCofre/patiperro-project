package com.patiperro.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConversacionUpdateRequest {

	@NotNull
	private Integer idReserva;

	@NotNull
	private Integer idEstadoChat;
}
