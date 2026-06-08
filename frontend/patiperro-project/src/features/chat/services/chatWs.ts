import { Client } from "@stomp/stompjs";
import { CHAT_WS_BROKER_URL } from "../../../config/api";
import { mapApiMessage } from "./chatApi";
import type { ChatConnectionState, ChatMessage, TypingEvent } from "../types/chat.types";

type ChatMessageOutboundApi = {
  idMensaje?: number | null;
  idReserva?: number | null;
  idUsuario?: number | null;
  sender?: string | null;
  tipo?: string | null;
  content?: string | null;
  contenido?: string | null;
  imageUrl?: string | null;
  urlMedia?: string | null;
  timestamp?: string | null;
  estadoMensaje?: string | null;
};

type ChatTypingEventApi = {
  idReserva?: number | null;
  idUsuario?: number | null;
  sender?: string | null;
  isTyping?: boolean | null;
  timestamp?: string | null;
};

type ReservaChatHandlers = {
  onMessage?: (message: ChatMessage) => void;
  onTyping?: (event: TypingEvent) => void;
  onConnected?: (state: ChatConnectionState) => void;
  onReconnecting?: () => void;
  onDisconnected?: () => void;
  onError?: (message: string) => void;
};

type GlobalMessageListener = (message: ChatMessage) => void;

type PendingConnect = {
  resolve: (client: Client) => void;
  reject: (error: Error) => void;
};

const TOPIC_MESSAGE_PREFIX = "/topic/reserva.";
const TOPIC_TYPING_SUFFIX = ".typing";
const CLIENT_WIRED_FLAG = "__patiperroChatWired";

let stompClient: Client | null = null;
let connectPromise: Promise<Client> | null = null;
let pendingConnects: PendingConnect[] = [];

const reservaHandlers = new Map<number, Set<ReservaChatHandlers>>();
const globalMessageListeners = new Set<GlobalMessageListener>();
const topicSubscriptions = new Map<string, { unsubscribe: () => void }>();
/** Cuántos consumidores (chat abierto + toasts) observan cada reserva. */
const reservaInterest = new Map<number, number>();

function mapOutboundMessage(row: ChatMessageOutboundApi): ChatMessage | null {
  const idReserva = Number(row.idReserva ?? 0);
  if (!idReserva) {
    return null;
  }
  return mapApiMessage(idReserva, {
    idMensaje: row.idMensaje,
    idReserva: row.idReserva,
    idUsuario: row.idUsuario,
    sender: row.sender,
    tipo: row.tipo,
    content: row.content,
    contenido: row.contenido,
    imageUrl: row.imageUrl,
    urlMedia: row.urlMedia,
    timestamp: row.timestamp,
    estadoMensaje: row.estadoMensaje
  });
}

function mapTypingEvent(row: ChatTypingEventApi): TypingEvent | null {
  const idReserva = Number(row.idReserva ?? 0);
  const senderUserId = Number(row.idUsuario ?? 0);
  if (!idReserva || !senderUserId) {
    return null;
  }
  return {
    idReserva,
    senderUserId,
    senderRole: "tutor",
    senderName: String(row.sender ?? "Usuario").trim() || "Usuario",
    isTyping: Boolean(row.isTyping)
  };
}

function dispatchMessage(message: ChatMessage): void {
  reservaHandlers.get(message.idReserva)?.forEach((handler) => handler.onMessage?.(message));
  globalMessageListeners.forEach((listener) => listener(message));
}

function dispatchTyping(event: TypingEvent): void {
  reservaHandlers.get(event.idReserva)?.forEach((handler) => handler.onTyping?.(event));
}

function topicKey(reservaId: number, kind: "messages" | "typing"): string {
  return kind === "messages"
    ? `${TOPIC_MESSAGE_PREFIX}${reservaId}`
    : `${TOPIC_MESSAGE_PREFIX}${reservaId}${TOPIC_TYPING_SUFFIX}`;
}

function hasActiveConsumers(): boolean {
  return reservaHandlers.size > 0 || globalMessageListeners.size > 0 || reservaInterest.size > 0;
}

function notifyConnectionError(message: string): void {
  reservaHandlers.forEach((set) => {
    set.forEach((handler) => handler.onError?.(message));
  });
}

function notifyConnected(): void {
  reservaHandlers.forEach((set) => {
    set.forEach((handler) => handler.onConnected?.("connected"));
  });
}

function notifyReconnecting(): void {
  reservaHandlers.forEach((set) => {
    set.forEach((handler) => handler.onReconnecting?.());
  });
}

function notifyDisconnected(): void {
  reservaHandlers.forEach((set) => {
    set.forEach((handler) => handler.onDisconnected?.());
  });
}

function resolvePendingConnects(client: Client): void {
  const waiters = pendingConnects;
  pendingConnects = [];
  waiters.forEach(({ resolve }) => resolve(client));
}

function rejectPendingConnects(error: Error): void {
  const waiters = pendingConnects;
  pendingConnects = [];
  waiters.forEach(({ reject }) => reject(error));
}

function clearTopicSubscriptions(): void {
  topicSubscriptions.forEach((subscription) => {
    try {
      subscription.unsubscribe();
    } catch {
      // Suscripción STOMP ya inválida tras desconexión.
    }
  });
  topicSubscriptions.clear();
}

function subscribeTopicIfNeeded(client: Client, destination: string): void {
  if (!client.connected || topicSubscriptions.has(destination)) {
    return;
  }

  const subscription = client.subscribe(destination, (frame) => {
    if (!frame.body) return;
    try {
      const payload = JSON.parse(frame.body) as unknown;
      if (destination.endsWith(TOPIC_TYPING_SUFFIX)) {
        const typing = mapTypingEvent(payload as ChatTypingEventApi);
        if (typing) {
          dispatchTyping(typing);
        }
        return;
      }
      const message = mapOutboundMessage(payload as ChatMessageOutboundApi);
      if (message) {
        dispatchMessage(message);
      }
    } catch {
      // JSON inválido
    }
  });

  topicSubscriptions.set(destination, subscription);
}

function syncTopicSubscriptions(client: Client): void {
  if (!client.connected) {
    return;
  }
  reservaInterest.forEach((_count, reservaId) => {
    subscribeTopicIfNeeded(client, topicKey(reservaId, "messages"));
    subscribeTopicIfNeeded(client, topicKey(reservaId, "typing"));
  });
}

function unsubscribeReservaTopics(reservaId: number): void {
  const messageTopic = topicKey(reservaId, "messages");
  const typingTopic = topicKey(reservaId, "typing");
  topicSubscriptions.get(messageTopic)?.unsubscribe();
  topicSubscriptions.get(typingTopic)?.unsubscribe();
  topicSubscriptions.delete(messageTopic);
  topicSubscriptions.delete(typingTopic);
}

function addReservaInterest(reservaId: number): void {
  if (!Number.isFinite(reservaId) || reservaId <= 0) {
    return;
  }
  const next = (reservaInterest.get(reservaId) ?? 0) + 1;
  reservaInterest.set(reservaId, next);
  if (next === 1 && stompClient?.connected) {
    subscribeTopicIfNeeded(stompClient, topicKey(reservaId, "messages"));
    subscribeTopicIfNeeded(stompClient, topicKey(reservaId, "typing"));
  }
}

function removeReservaInterest(reservaId: number): void {
  const current = reservaInterest.get(reservaId) ?? 0;
  if (current <= 1) {
    reservaInterest.delete(reservaId);
    unsubscribeReservaTopics(reservaId);
  } else {
    reservaInterest.set(reservaId, current - 1);
  }
}

function wireClientCallbacks(client: Client): void {
  const wiredClient = client as Client & { [CLIENT_WIRED_FLAG]?: boolean };
  if (wiredClient[CLIENT_WIRED_FLAG]) {
    return;
  }
  wiredClient[CLIENT_WIRED_FLAG] = true;

  client.onConnect = () => {
    syncTopicSubscriptions(client);
    notifyConnected();
    resolvePendingConnects(client);
    connectPromise = null;
  };

  client.onDisconnect = () => {
    clearTopicSubscriptions();
    connectPromise = null;
    if (hasActiveConsumers()) {
      notifyReconnecting();
      return;
    }
    notifyDisconnected();
  };

  client.onWebSocketError = () => {
    const message = "No se pudo conectar al chat en tiempo real.";
    notifyConnectionError(message);
    rejectPendingConnects(new Error(message));
    connectPromise = null;
    if (hasActiveConsumers()) {
      notifyReconnecting();
    }
  };
}

function ensureClient(): Client {
  if (!stompClient) {
    stompClient = new Client({
      brokerURL: CHAT_WS_BROKER_URL,
      reconnectDelay: 2000,
      heartbeatIncoming: 8000,
      heartbeatOutgoing: 8000,
      onStompError: (frame) => {
        const detail = frame.body?.trim() || "Error STOMP en el chat.";
        notifyConnectionError(detail);
        if (hasActiveConsumers()) {
          notifyReconnecting();
        }
      }
    });
    wireClientCallbacks(stompClient);
  }
  return stompClient;
}

function teardownClientIfIdle(): void {
  if (hasActiveConsumers()) {
    return;
  }
  connectPromise = null;
  rejectPendingConnects(new Error("Chat desconectado."));
  pendingConnects = [];
  clearTopicSubscriptions();
  stompClient?.deactivate();
  stompClient = null;
}

async function ensureConnected(): Promise<Client> {
  const client = ensureClient();
  if (client.connected) {
    syncTopicSubscriptions(client);
    return client;
  }

  if (connectPromise) {
    return connectPromise;
  }

  connectPromise = new Promise<Client>((resolve, reject) => {
    pendingConnects.push({ resolve, reject });
    client.activate();
  }).finally(() => {
    connectPromise = null;
  });

  return connectPromise;
}

function refreshConnectionAfterWake(): void {
  if (!hasActiveConsumers()) {
    return;
  }

  const client = stompClient;
  if (!client) {
    void ensureConnected().catch(() => undefined);
    return;
  }

  if (!client.connected) {
    notifyReconnecting();
    void ensureConnected().catch(() => undefined);
    return;
  }

  clearTopicSubscriptions();
  syncTopicSubscriptions(client);
  notifyConnected();
}

if (typeof document !== "undefined") {
  document.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "visible") {
      window.setTimeout(refreshConnectionAfterWake, 300);
    }
  });

  window.addEventListener("online", () => {
    window.setTimeout(refreshConnectionAfterWake, 300);
  });
}

export function forceReconnectChat(): void {
  if (!hasActiveConsumers()) {
    return;
  }

  const client = stompClient;
  clearTopicSubscriptions();
  connectPromise = null;
  rejectPendingConnects(new Error("Reconexion manual del chat."));

  if (!client) {
    void ensureConnected().catch(() => undefined);
    return;
  }

  notifyReconnecting();

  client.deactivate();
  window.setTimeout(() => {
    if (!hasActiveConsumers()) {
      return;
    }
    void ensureConnected().catch(() => undefined);
  }, 300);
}

export function subscribeReservaChat(
  reservaId: number,
  handlers: ReservaChatHandlers
): () => void {
  addReservaInterest(reservaId);

  if (!reservaHandlers.has(reservaId)) {
    reservaHandlers.set(reservaId, new Set());
  }
  reservaHandlers.get(reservaId)?.add(handlers);

  handlers.onConnected?.("connecting");
  void ensureConnected().catch(() => {
    handlers.onError?.("No se pudo conectar al chat en tiempo real.");
  });

  return () => {
    reservaHandlers.get(reservaId)?.delete(handlers);
    if (reservaHandlers.get(reservaId)?.size === 0) {
      reservaHandlers.delete(reservaId);
    }
    removeReservaInterest(reservaId);
    teardownClientIfIdle();
  };
}

export function subscribeChatMessages(
  reservaIds: number[],
  listener: GlobalMessageListener
): () => void {
  const uniqueIds = [...new Set(reservaIds.filter((id) => Number.isFinite(id) && id > 0))];
  uniqueIds.forEach((id) => addReservaInterest(id));
  globalMessageListeners.add(listener);

  void ensureConnected().catch(() => undefined);

  return () => {
    globalMessageListeners.delete(listener);
    uniqueIds.forEach((id) => removeReservaInterest(id));
    teardownClientIfIdle();
  };
}

export async function sendReservaChatMessage(message: ChatMessage): Promise<void> {
  const client = await ensureConnected();
  if (!client.connected) {
    throw new Error("El chat se esta reconectando. Intenta de nuevo en unos segundos.");
  }
  client.publish({
    destination: "/app/chat.send",
    body: JSON.stringify({
      sender: message.senderName,
      content: message.content,
      timestamp: message.timestamp,
      idReserva: message.idReserva,
      idUsuario: message.senderUserId
    })
  });
}

export async function sendTypingSignal(event: TypingEvent): Promise<void> {
  const client = await ensureConnected();
  if (!client.connected) {
    return;
  }
  client.publish({
    destination: "/app/chat.typing",
    body: JSON.stringify({
      idReserva: event.idReserva,
      idUsuario: event.senderUserId,
      sender: event.senderName,
      isTyping: event.isTyping,
      timestamp: new Date().toISOString()
    })
  });
}
