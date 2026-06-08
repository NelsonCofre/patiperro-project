export type ChatRole = "tutor" | "paseador";

export type ChatMessageEstado = "pendiente" | "enviado" | "error";

export type ChatMessageTipo = "TEXTO" | "IMAGEN";

export type ChatMessage = {
  id: string;
  idReserva: number;
  senderUserId: number;
  senderRole: ChatRole;
  senderName: string;
  tipo: ChatMessageTipo;
  content: string;
  imageUrl?: string;
  timestamp: string;
  estado?: ChatMessageEstado;
};

export type TypingEvent = {
  idReserva: number;
  senderUserId: number;
  senderRole: ChatRole;
  senderName: string;
  isTyping: boolean;
};

export type ChatConnectionState =
  | "idle"
  | "loading-history"
  | "connecting"
  | "reconnecting"
  | "connected"
  | "error";

export type ChatWindowProps = {
  isOpen: boolean;
  reservaId: number;
  currentUserId: number;
  currentUserRole: ChatRole;
  currentUserName: string;
  /** Id del tutor o paseador con quien se chatea (para resolver nombres en historial). */
  counterpartUserId?: number;
  counterpartName: string;
  mascotaNombre: string;
  /** Solo paseador en paseo EN CURSO puede subir fotos. */
  canSendPhotos?: boolean;
  onClose: () => void;
};

export type ChatToastPayload = {
  reservaId: number;
  senderName: string;
  snippet: string;
};

