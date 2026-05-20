package com.patiperro.chat.controller;

import com.patiperro.chat.dto.ChatMessageInbound;
import com.patiperro.chat.dto.ChatMessageOutbound;
import com.patiperro.chat.dto.ChatTypingEvent;
import com.patiperro.chat.service.ChatService;
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

	/**
	 * 1) Persiste mensaje · 2) {@code /topic/reserva.{id}} · 3) listener backend decide si dispara push.
	 */
	@MessageMapping("/chat.send")
	public void sendMessage(@Valid ChatMessageInbound request) {
		ChatMessageOutbound outbound = chatService.enviarMensajeRealtime(request);
		messagingTemplate.convertAndSend("/topic/reserva." + outbound.getIdReserva(), outbound);
	}

	@MessageMapping("/chat.typing")
	public void publishTyping(@Valid ChatTypingEvent event) {
		ChatTypingEvent validated = chatService.publicarTyping(event);
		messagingTemplate.convertAndSend(
				"/topic/reserva." + validated.getIdReserva() + ".typing",
				validated);
	}
}
