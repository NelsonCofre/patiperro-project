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
 * Fallback MVP: mientras el cliente STOMP no esté instalado en el proyecto,
 * mantenemos una API estable para los módulos que esperan eventos de encuentro.
 * El backend puede seguir notificando por polling/refresh sin romper el build.
 */
export function subscribeEncuentroTopic({
  topic,
  onEvent,
  onError
}: SubscribeOptions): () => void {
  void topic;
  void onEvent;
  onError?.("El canal en tiempo real se encuentra deshabilitado en este entorno.");
  return () => {};
}
