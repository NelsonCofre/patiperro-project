import { useEffect, useMemo, useRef, useState } from "react";
import { fetchChatHistory, uploadChatImage } from "../services/chatApi";
import {
  forceReconnectChat,
  sendReservaChatMessage,
  sendTypingSignal,
  subscribeReservaChat
} from "../services/chatWs";
import type {
  ChatConnectionState,
  ChatMessage,
  ChatRole,
  TypingEvent
} from "../types/chat.types";
import { enrichChatMessage } from "../utils/chatDisplayNames";

type UseReservaChatOptions = {
  isOpen: boolean;
  reservaId: number;
  currentUserId: number;
  currentUserRole: ChatRole;
  currentUserName: string;
  counterpartUserId?: number;
  counterpartName: string;
};

function isLocalOptimisticId(id: string): boolean {
  return id.startsWith("local-");
}

/** Quita el eco local cuando el servidor devuelve el mismo mensaje por STOMP. */
function dropOwnOptimisticEcho(
  current: ChatMessage[],
  incoming: ChatMessage,
  currentUserId: number
): ChatMessage[] {
  if (incoming.senderUserId !== currentUserId) {
    return current;
  }
  return current.filter(
    (item) =>
      !(
        item.senderUserId === currentUserId &&
        (item.estado === "pendiente" || isLocalOptimisticId(item.id)) &&
        (item.tipo === "IMAGEN"
          ? incoming.tipo === "IMAGEN" &&
            (isLocalOptimisticId(item.id) ||
              (item.imageUrl && item.imageUrl === incoming.imageUrl))
          : item.content === incoming.content)
      )
  );
}

function mergeMessages(
  current: ChatMessage[],
  incoming: ChatMessage
): ChatMessage[] {
  const existingIndex = current.findIndex((item) => item.id === incoming.id);
  if (existingIndex >= 0) {
    const next = [...current];
    next[existingIndex] = {
      ...next[existingIndex],
      ...incoming
    };
    return next;
  }
  return [...current, incoming].sort((a, b) => {
    const ta = new Date(a.timestamp).getTime();
    const tb = new Date(b.timestamp).getTime();
    if (Number.isNaN(ta) || Number.isNaN(tb)) {
      return a.timestamp.localeCompare(b.timestamp);
    }
    return ta - tb;
  });
}

export function useReservaChat({
  isOpen,
  reservaId,
  currentUserId,
  currentUserRole,
  currentUserName,
  counterpartUserId,
  counterpartName
}: UseReservaChatOptions) {
  const enrichMessage = (message: ChatMessage): ChatMessage =>
    enrichChatMessage(message, currentUserId, counterpartUserId, counterpartName);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [draft, setDraft] = useState("");
  const [typingUsers, setTypingUsers] = useState<TypingEvent[]>([]);
  const [connectionState, setConnectionState] = useState<ChatConnectionState>("idle");
  const [isSending, setIsSending] = useState(false);
  const [historyError, setHistoryError] = useState<string | null>(null);
  const [sendError, setSendError] = useState<string | null>(null);
  const typingTimeoutRef = useRef<number | null>(null);

  function clearTypingTimeout(): void {
    if (typingTimeoutRef.current != null) {
      window.clearTimeout(typingTimeoutRef.current);
      typingTimeoutRef.current = null;
    }
  }

  async function notifyStoppedTyping(): Promise<void> {
    if (!Number.isFinite(reservaId) || reservaId <= 0) return;
    await sendTypingSignal({
      idReserva: reservaId,
      senderUserId: currentUserId,
      senderRole: currentUserRole,
      senderName: currentUserName,
      isTyping: false
    });
  }

  useEffect(() => {
    if (!isOpen || !Number.isFinite(reservaId) || reservaId <= 0) {
      clearTypingTimeout();
      setTypingUsers([]);
      setConnectionState("idle");
      return;
    }

    let active = true;
    setConnectionState("loading-history");
    setHistoryError(null);

    void fetchChatHistory(reservaId)
      .then((history) => {
        if (!active) return;
        setMessages(history.map(enrichMessage));
        setConnectionState("connecting");
      })
      .catch((error) => {
        if (!active) return;
        setHistoryError(
          error instanceof Error ? error.message : "No se pudo cargar el historial."
        );
        setConnectionState("error");
      });

    const unsubscribe = subscribeReservaChat(reservaId, {
      onConnected: () => {
        if (!active) return;
        setConnectionState("connected");
        setSendError(null);
      },
      onReconnecting: () => {
        if (!active) return;
        setConnectionState("reconnecting");
      },
      onDisconnected: () => {
        if (!active) return;
        setConnectionState("idle");
      },
      onError: (message) => {
        if (!active) return;
        setConnectionState("reconnecting");
        setSendError(message);
      },
      onMessage: (message) => {
        if (!active) return;
        const enriched = enrichMessage(message);
        setMessages((prev) =>
          mergeMessages(dropOwnOptimisticEcho(prev, enriched, currentUserId), enriched)
        );
      },
      onTyping: (event) => {
        if (!active || event.senderUserId === currentUserId) return;
        setTypingUsers((prev) => {
          const filtered = prev.filter(
            (item) => item.senderUserId !== event.senderUserId
          );
          if (!event.isTyping) return filtered;
          return [...filtered, event];
        });
      }
    });

    return () => {
      active = false;
      clearTypingTimeout();
      setTypingUsers([]);
      setConnectionState("idle");
      unsubscribe();
    };
  }, [counterpartName, counterpartUserId, currentUserId, isOpen, reservaId]);

  useEffect(() => {
    if (!isOpen) return;

    if (!draft.trim()) {
      clearTypingTimeout();
      void notifyStoppedTyping();
      return;
    }

    void sendTypingSignal({
      idReserva: reservaId,
      senderUserId: currentUserId,
      senderRole: currentUserRole,
      senderName: currentUserName,
      isTyping: true
    });

    clearTypingTimeout();
    typingTimeoutRef.current = window.setTimeout(() => {
      typingTimeoutRef.current = null;
      void notifyStoppedTyping();
    }, 1800);

    return () => {
      clearTypingTimeout();
    };
  }, [currentUserId, currentUserName, currentUserRole, draft, isOpen, reservaId]);

  const typingLabel = useMemo(() => {
    const activeTyping = typingUsers.filter((item) => item.isTyping);
    if (activeTyping.length === 0) return "";
    if (activeTyping.length === 1) {
      return `${activeTyping[0].senderName} esta escribiendo...`;
    }
    return "Hay personas escribiendo...";
  }, [typingUsers]);

  async function sendMessage(): Promise<void> {
    if (isSending) return;
    const content = draft.trim();
    if (!content) return;

    setIsSending(true);
    setSendError(null);

    const optimisticMessage: ChatMessage = {
      id: `local-${reservaId}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      idReserva: reservaId,
      senderUserId: currentUserId,
      senderRole: currentUserRole,
      senderName: currentUserName,
      tipo: "TEXTO",
      content,
      timestamp: new Date().toISOString(),
      estado: "pendiente"
    };

    setMessages((prev) => mergeMessages(prev, optimisticMessage));
    setDraft("");

    try {
      await sendReservaChatMessage(optimisticMessage);
      setMessages((prev) =>
        prev.map((item) =>
          item.id === optimisticMessage.id ? { ...item, estado: "enviado" } : item
        )
      );
    } catch (error) {
      const message =
        error instanceof Error ? error.message : "No se pudo enviar el mensaje.";
      setSendError(message);
      setMessages((prev) =>
        prev.map((item) =>
          item.id === optimisticMessage.id ? { ...item, estado: "error" } : item
        )
      );
    } finally {
      setIsSending(false);
      clearTypingTimeout();
      void notifyStoppedTyping();
    }
  }

  async function sendImage(file: File, comentario?: string): Promise<void> {
    if (isSending) return;

    setIsSending(true);
    setSendError(null);

    const previewUrl = URL.createObjectURL(file);
    const optimisticId = `local-img-${reservaId}-${Date.now()}`;
    const optimisticMessage: ChatMessage = {
      id: optimisticId,
      idReserva: reservaId,
      senderUserId: currentUserId,
      senderRole: currentUserRole,
      senderName: currentUserName,
      tipo: "IMAGEN",
      content: comentario?.trim() ?? "",
      imageUrl: previewUrl,
      timestamp: new Date().toISOString(),
      estado: "pendiente"
    };

    setMessages((prev) => mergeMessages(prev, optimisticMessage));

    try {
      const saved = await uploadChatImage(
        reservaId,
        currentUserId,
        currentUserName,
        file,
        comentario
      );
      setMessages((prev) => {
        const withoutLocal = prev.filter((item) => item.id !== optimisticId);
        return mergeMessages(
          dropOwnOptimisticEcho(withoutLocal, enrichMessage(saved), currentUserId),
          enrichMessage(saved)
        );
      });
    } catch (error) {
      const message =
        error instanceof Error ? error.message : "No se pudo enviar la foto.";
      setSendError(message);
      setMessages((prev) =>
        prev.map((item) =>
          item.id === optimisticId ? { ...item, estado: "error" } : item
        )
      );
    } finally {
      URL.revokeObjectURL(previewUrl);
      setIsSending(false);
    }
  }

  async function retryConnection(): Promise<void> {
    setConnectionState("reconnecting");
    setSendError(null);
    forceReconnectChat();
  }

  async function retryHistory(): Promise<void> {
    setConnectionState("loading-history");
    setHistoryError(null);
    try {
      const history = await fetchChatHistory(reservaId);
      setMessages(history.map(enrichMessage));
      setConnectionState("reconnecting");
      forceReconnectChat();
    } catch (error) {
      setHistoryError(
        error instanceof Error ? error.message : "No se pudo cargar el historial."
      );
      setConnectionState("error");
    }
  }

  function clearSendError(): void {
    setSendError(null);
  }

  return {
    messages,
    draft,
    setDraft,
    typingUsers,
    typingLabel,
    connectionState,
    isSending,
    historyError,
    sendError,
    sendMessage,
    sendImage,
    retryHistory,
    retryConnection,
    clearSendError
  };
}
