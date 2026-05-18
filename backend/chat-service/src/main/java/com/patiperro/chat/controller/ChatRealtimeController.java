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

@Controller
@RequiredArgsConstructor
public class ChatRealtimeController {

	private final ChatService chatService;
	private final SimpMessagingTemplate messagingTemplate;
	private final NotificacionChatPushIntegracionClient notificacionChatPushIntegracionClient;

	@MessageMapping("/chat.send")
	public void sendMessage(@Valid ChatMessageInbound request) {
		ChatMessageOutbound outbound = chatService.enviarMensajeRealtime(request);
		messagingTemplate.convertAndSend("/topic/reserva." + outbound.getIdReserva(), outbound);

		// Web Push best-effort (no bloquea el chat si notification-service falla).
		Integer destinatarioId = chatService.resolverDestinatarioPush(
				outbound.getIdReserva(),
				outbound.getIdUsuario());
		if (destinatarioId != null) {
			String preview = outbound.getContent();
			if (preview != null && preview.length() > 120) {
				preview = preview.substring(0, 119) + "…";
			}
			notificacionChatPushIntegracionClient.notificarNuevoMensaje(new ChatNuevoMensajePushIntegracionRequest(
					destinatarioId,
					outbound.getIdReserva(),
					outbound.getIdConversacion(),
					outbound.getIdMensaje(),
					outbound.getSender(),
					preview != null ? preview : "",
					"/chat/reserva/" + outbound.getIdReserva()));
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
