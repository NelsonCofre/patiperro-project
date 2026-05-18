package com.patiperro.chat.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class MensajeResponseDTO {
	Integer id;
	Integer idConversacion;
	Integer idUsuario;
	String contenido;
	Instant fechaEnvio;
	Integer idEstadoMensaje;
	String nombreEstadoMensaje;
}
