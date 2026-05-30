package com.patiperro.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MensajeCreateRequest {

	@NotNull
	private Integer idUsuario;

	@NotBlank
	@Size(max = 4000)
	private String contenido;

	/** FK a `estado_mensaje.id_estado_mensaje`. Si se omite, se usa el estado sembrado `ENVIADO`. */
	private Integer idEstadoMensaje;
}
