package com.patiperro.chat.controller;

import com.patiperro.chat.dto.ChatMessageInbound;
import com.patiperro.chat.dto.ChatMessageOutbound;
import com.patiperro.chat.dto.ChatTypingEvent;
import com.patiperro.chat.service.ChatService;
import com.patiperro.chat.dto.ChatNuevoMensajePushIntegracionRequest;
import com.patiperro.chat.support.NotificacionChatPushIntegracionClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * STOMP realtime del chat por reserva ({@code /app/chat.send}, {@code /app/chat.typing}).
 * Web Push: tras publicar en el topic, best-effort vía notification-service (no en typing).
 */
@Controller
@RequiredArgsConstructor
public class ChatRealtimeController {

	private final ChatService chatService;
	private final SimpMessagingTemplate messagingTemplate;
	private final NotificacionChatPushIntegracionClient notificacionChatPushIntegracionClient;

	/**
	 * 1) Persiste mensaje · 2) {@code /topic/reserva.{id}} · 3) push al interlocutor (si integración activa).
	 */
	@MessageMapping("/chat.send")
	public void sendMessage(@Valid ChatMessageInbound request) {
		ChatMessageOutbound outbound = chatService.enviarMensajeRealtime(request);
		messagingTemplate.convertAndSend("/topic/reserva." + outbound.getIdReserva(), outbound);

		// Best-effort: errores HTTP no propagan al WebSocket (ver NotificacionChatPushIntegracionClient).
		Integer destinatarioId = chatService.resolverDestinatarioPush(
				outbound.getIdReserva(),
				outbound.getIdUsuario());
		ChatNuevoMensajePushIntegracionRequest pushPayload =
				ChatNuevoMensajePushIntegracionRequest.desdeMensajeRealtime(destinatarioId, outbound);
		if (pushPayload != null) {
			notificacionChatPushIntegracionClient.notificarNuevoMensaje(pushPayload);
		}
	}

	@MessageMapping("/chat.typing")
	public void publishTyping(@Valid ChatTypingEvent event) {
		ChatTypingEvent validated = chatService.publicarTyping(event);
		messagingTemplate.convertAndSend(
				"/topic/reserva." + validated.getIdReserva() + ".typing",
				validated);
	}
}
