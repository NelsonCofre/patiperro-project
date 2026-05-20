package com.patiperro.chat.event;

import com.patiperro.chat.dto.ChatMessageOutbound;

/**
 * Evento interno emitido cuando un mensaje de chat ya quedó persistido y puede
 * gatillar acciones secundarias como Web Push.
 */
public record ChatMensajePersistidoEvent(ChatMessageOutbound outbound) {
}
