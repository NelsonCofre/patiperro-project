package com.patiperro.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MensajeUpdateRequest {

	@NotNull
	private Integer idUsuario;

	@NotBlank
	@Size(max = 4000)
	private String contenido;

	@NotNull
	private Integer idEstadoMensaje;
}
