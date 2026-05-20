package com.patiperro.chat.event;

import com.patiperro.chat.dto.ChatNuevoMensajePushIntegracionRequest;
import com.patiperro.chat.service.ChatService;
import com.patiperro.chat.support.NotificacionChatPushIntegracionClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener interno que reacciona al guardado exitoso del mensaje y dispara la
 * integración best-effort con notification-service.
 */
@Component
@RequiredArgsConstructor
public class ChatPushNotificationListener {

    private final ChatService chatService;
    private final NotificacionChatPushIntegracionClient notificacionChatPushIntegracionClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMensajePersistido(ChatMensajePersistidoEvent event) {
        if (event == null || event.outbound() == null) {
            return;
        }

        Integer destinatarioId = chatService.resolverDestinatarioPush(
                event.outbound().getIdReserva(),
                event.outbound().getIdUsuario());

        ChatNuevoMensajePushIntegracionRequest pushPayload =
                ChatNuevoMensajePushIntegracionRequest.desdeMensajeRealtime(destinatarioId, event.outbound());

        if (pushPayload != null) {
            notificacionChatPushIntegracionClient.notificarNuevoMensaje(pushPayload);
        }
    }
}
