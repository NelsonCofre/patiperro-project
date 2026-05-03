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

/**
 * Placeholder hasta integrar STOMP (SockJS + @stomp/stompjs) contra el broker del backend.
 * No emite eventos ni errores: la UI debe actualizarse por polling o tras acciones HTTP.
 */
export function subscribeEncuentroTopic({
  topic,
  onEvent,
  onError
}: SubscribeOptions): () => void {
  void topic;
  void onEvent;
  void onError;
  return () => {};
}
