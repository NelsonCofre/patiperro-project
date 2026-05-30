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
	String content;
	Instant timestamp;
	String estadoMensaje;
}
