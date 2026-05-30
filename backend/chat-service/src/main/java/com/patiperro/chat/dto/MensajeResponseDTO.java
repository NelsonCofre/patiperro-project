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
	String tipo;
	/** Texto del mensaje (alias JSON: {@code content}). */
	String contenido;
	String content;
	/** Ruta relativa de imagen (alias JSON: {@code imageUrl}). */
	String urlMedia;
	String imageUrl;
	Instant fechaEnvio;
	Instant timestamp;
	Integer idEstadoMensaje;
	String nombreEstadoMensaje;
}
