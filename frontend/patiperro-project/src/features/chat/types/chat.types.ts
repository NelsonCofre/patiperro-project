export type ChatRole = "tutor" | "paseador";

export type ChatMessageEstado = "pendiente" | "enviado" | "error";

export type ChatMessage = {
  id: string;
  idReserva: number;
  senderUserId: number;
  senderRole: ChatRole;
  senderName: string;
  content: string;
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
  | "connected"
  | "error";

export type ChatWindowProps = {
  isOpen: boolean;
  reservaId: number;
  currentUserId: number;
  currentUserRole: ChatRole;
  currentUserName: string;
  counterpartName: string;
  mascotaNombre: string;
  onClose: () => void;
};

export type ChatToastPayload = {
  reservaId: number;
  senderName: string;
  snippet: string;
};

