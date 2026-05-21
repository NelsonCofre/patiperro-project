package com.patiperro.chat.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChatNuevoMensajePushIntegracionRequestTest {

	@Test
	void previewImagenSinComentarioUsaTextoFijo() {
		ChatMessageOutbound outbound = ChatMessageOutbound.builder()
				.idMensaje(1)
				.idConversacion(2)
				.idReserva(10)
				.idUsuario(99)
				.sender("Paseador")
				.tipo("IMAGEN")
				.content("")
				.imageUrl("/api/chat/media/abc.jpg")
				.timestamp(Instant.now())
				.estadoMensaje("ENVIADO")
				.build();

		ChatNuevoMensajePushIntegracionRequest request =
				ChatNuevoMensajePushIntegracionRequest.desdeMensajeRealtime(5, outbound);

		assertNotNull(request);
		assertEquals("📷 Foto del paseo", request.contenidoPreview());
	}

	@Test
	void previewImagenConComentarioUsaComentario() {
		ChatMessageOutbound outbound = ChatMessageOutbound.builder()
				.idMensaje(1)
				.idConversacion(2)
				.idReserva(10)
				.idUsuario(99)
				.sender("Paseador")
				.tipo("IMAGEN")
				.content("Mira qué feliz")
				.imageUrl("/api/chat/media/abc.jpg")
				.timestamp(Instant.now())
				.estadoMensaje("ENVIADO")
				.build();

		ChatNuevoMensajePushIntegracionRequest request =
				ChatNuevoMensajePushIntegracionRequest.desdeMensajeRealtime(5, outbound);

		assertNotNull(request);
		assertEquals("Mira qué feliz", request.contenidoPreview());
	}
}
