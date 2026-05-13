package com.patiperro.chat.config;

import com.patiperro.chat.model.EstadoChat;
import com.patiperro.chat.model.EstadoMensaje;
import com.patiperro.chat.repository.EstadoChatRepository;
import com.patiperro.chat.repository.EstadoMensajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Inserta filas mínimas en catálogos si la base está vacía (MER: estado_chat, estado_mensaje).
 */
@Component
@Order(Integer.MAX_VALUE)
@RequiredArgsConstructor
public class ChatEstadosSeedRunner implements ApplicationRunner {

	public static final String ESTADO_CHAT_ABIERTA = "ABIERTA";
	public static final String ESTADO_CHAT_CERRADA = "CERRADA";
	public static final String ESTADO_MENSAJE_ENVIADO = "ENVIADO";
	public static final String ESTADO_MENSAJE_LEIDO = "LEIDO";

	private final EstadoChatRepository estadoChatRepository;
	private final EstadoMensajeRepository estadoMensajeRepository;

	@Override
	public void run(ApplicationArguments args) {
		if (estadoChatRepository.count() == 0) {
			estadoChatRepository.save(estado(ESTADO_CHAT_ABIERTA));
			estadoChatRepository.save(estado(ESTADO_CHAT_CERRADA));
		}
		if (estadoMensajeRepository.count() == 0) {
			estadoMensajeRepository.save(estadoMensaje(ESTADO_MENSAJE_ENVIADO));
			estadoMensajeRepository.save(estadoMensaje(ESTADO_MENSAJE_LEIDO));
		}
	}

	private static EstadoChat estado(String nombre) {
		EstadoChat e = new EstadoChat();
		e.setNombre(nombre);
		return e;
	}

	private static EstadoMensaje estadoMensaje(String nombre) {
		EstadoMensaje e = new EstadoMensaje();
		e.setNombre(nombre);
		return e;
	}
}
