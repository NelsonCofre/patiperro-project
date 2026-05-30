package com.patiperro.chat.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class ChatMessageOutbound {
	Integer idMensaje;
	Integer idConversacion;
	Integer idReserva;
	Integer idUsuario;
	String sender;
	String tipo;
	String content;
	/** Alias de {@code content} para paridad con historial REST. */
	String contenido;
	String imageUrl;
	/** Alias de {@code imageUrl} para paridad con historial REST. */
	String urlMedia;
	Instant timestamp;
	String estadoMensaje;
}
