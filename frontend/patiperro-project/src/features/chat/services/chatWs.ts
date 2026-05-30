import { RESERVA_WS_URL } from "../../../config/api";
import { persistLocalChatMessage } from "./chatApi";
import type {
  ChatConnectionState,
  ChatMessage,
  TypingEvent
} from "../types/chat.types";

type ChatEnvelope =
  | { kind: "message"; payload: ChatMessage }
  | { kind: "typing"; payload: TypingEvent };

type ReservaChatHandlers = {
  onMessage?: (message: ChatMessage) => void;
  onTyping?: (event: TypingEvent) => void;
  onConnected?: (state: ChatConnectionState) => void;
  onDisconnected?: () => void;
  onError?: (message: string) => void;
};

type GlobalMessageListener = (message: ChatMessage) => void;

const CHANNEL_NAME = "patiperro-chat-events";
const STORAGE_EVENT_KEY = "patiperro_chat_event_v1";

const reservaListeners = new Set<(envelope: ChatEnvelope) => void>();
const globalMessageListeners = new Set<GlobalMessageListener>();

let broadcastChannel: BroadcastChannel | null = null;
let storageListenerReady = false;

function getBroadcastChannel(): BroadcastChannel | null {
  if (typeof window === "undefined" || typeof BroadcastChannel === "undefined") {
    return null;
  }
  if (!broadcastChannel) {
    broadcastChannel = new BroadcastChannel(CHANNEL_NAME);
    broadcastChannel.onmessage = (event: MessageEvent<ChatEnvelope>) => {
      if (!event.data) return;
      dispatchEnvelope(event.data, false);
    };
  }
  return broadcastChannel;
}

function ensureStorageListener(): void {
  if (storageListenerReady || typeof window === "undefined") {
    return;
  }
  storageListenerReady = true;
  window.addEventListener("storage", (event) => {
    if (event.key !== STORAGE_EVENT_KEY || !event.newValue) return;
    try {
      const envelope = JSON.parse(event.newValue) as ChatEnvelope;
      dispatchEnvelope(envelope, false);
    } catch {
      // no-op
    }
  });
}

function dispatchEnvelope(envelope: ChatEnvelope, persist = true): void {
  if (persist && envelope.kind === "message") {
    persistLocalChatMessage(envelope.payload);
  }

  reservaListeners.forEach((listener) => listener(envelope));
  if (envelope.kind === "message") {
    globalMessageListeners.forEach((listener) => listener(envelope.payload));
  }
}

function publishEnvelope(envelope: ChatEnvelope): void {
  dispatchEnvelope(envelope, true);

  const channel = getBroadcastChannel();
  if (channel) {
    channel.postMessage(envelope);
    return;
  }

  if (typeof window !== "undefined") {
    window.localStorage.setItem(STORAGE_EVENT_KEY, JSON.stringify(envelope));
    window.localStorage.removeItem(STORAGE_EVENT_KEY);
  }
}

export function subscribeReservaChat(
  reservaId: number,
  handlers: ReservaChatHandlers
): () => void {
  getBroadcastChannel();
  ensureStorageListener();

  const listener = (envelope: ChatEnvelope) => {
    if (envelope.kind === "message" && envelope.payload.idReserva === reservaId) {
      handlers.onMessage?.(envelope.payload);
    }
    if (envelope.kind === "typing" && envelope.payload.idReserva === reservaId) {
      handlers.onTyping?.(envelope.payload);
    }
  };

  reservaListeners.add(listener);
  handlers.onConnected?.("connected");

  return () => {
    reservaListeners.delete(listener);
    handlers.onDisconnected?.();
  };
}

export function subscribeChatMessages(
  listener: GlobalMessageListener
): () => void {
  getBroadcastChannel();
  ensureStorageListener();
  globalMessageListeners.add(listener);
  return () => {
    globalMessageListeners.delete(listener);
  };
}

export async function sendReservaChatMessage(message: ChatMessage): Promise<void> {
  void RESERVA_WS_URL;
  publishEnvelope({
    kind: "message",
    payload: {
      ...message,
      estado: "enviado"
    }
  });
}

export async function sendTypingSignal(event: TypingEvent): Promise<void> {
  publishEnvelope({
    kind: "typing",
    payload: event
  });
}

