package com.patiperro.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConversacionCreateRequest {

	@NotNull
	private Integer idReserva;

	/** FK a `estado_chat.id_estado` (MER: estado_chat_id_estado). */
	@NotNull
	private Integer idEstadoChat;
}
