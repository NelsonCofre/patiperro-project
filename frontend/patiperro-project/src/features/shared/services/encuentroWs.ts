import { Client } from "@stomp/stompjs";
import { RESERVA_WS_URL } from "../../../config/api";

export type EncuentroConfirmadoEvent = {
  tipo?: string;
  idReserva?: number;
  idTutorUsuario?: number;
  idPaseadorUsuario?: number;
  mensajeTutor?: string;
  mensajePaseador?: string;
  mascotaNombre?: string;
  direccionInicio?: string;
  horaInicioRegistrada?: string | null;
  trackingActivo?: boolean;
  chatActivo?: boolean;
};

type SubscribeOptions = {
  topic: string;
  onEvent: (event: EncuentroConfirmadoEvent) => void;
  onError?: (message: string) => void;
};

export function subscribeEncuentroTopic({
  topic,
  onEvent,
  onError
}: SubscribeOptions): () => void {
  const client = new Client({
    brokerURL: RESERVA_WS_URL,
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onStompError: (frame) => {
      onError?.(frame.headers.message || "Error STOMP en eventos de encuentro.");
    }
  });

  client.onConnect = () => {
    client.subscribe(topic, (message) => {
      try {
        const payload = JSON.parse(message.body) as EncuentroConfirmadoEvent;
        onEvent(payload);
      } catch {
        onError?.("No se pudo interpretar el evento de encuentro.");
      }
    });
  };

  client.onWebSocketError = () => {
    onError?.("No se pudo conectar al canal en tiempo real de reservas.");
  };

  client.activate();
  return () => {
    client.deactivate();
  };
}
