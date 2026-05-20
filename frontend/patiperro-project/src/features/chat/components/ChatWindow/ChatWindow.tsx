import type { FormEvent, KeyboardEvent as ReactKeyboardEvent, MouseEvent as ReactMouseEvent } from "react";
import { useEffect, useMemo, useRef, useState } from "react";
import { useReservaChat } from "../../hooks/useReservaChat";
import type { ChatMessage, ChatWindowProps } from "../../types/chat.types";
import {
  formatChatTimestamp,
  isNearBottom
} from "../../utils/chatFormatters";
import { resolveChatSenderName } from "../../utils/chatDisplayNames";
import styles from "./ChatWindow.module.css";

const CHAT_VISIBILITY_EVENT = "chat-visibility";

function getConnectionLabel(value: string): string {
  if (value === "loading-history") return "Cargando historial";
  if (value === "connecting") return "Conectando";
  if (value === "connected") return "Conectado";
  if (value === "error") return "Sin conexion";
  return "Inactivo";
}

type ChatVisibilityPayload = {
  type: typeof CHAT_VISIBILITY_EVENT;
  idReserva: number;
  isChatOpen: boolean;
  visibilityState: DocumentVisibilityState;
  focused: boolean;
};

function postChatVisibilityToServiceWorker(payload: ChatVisibilityPayload): void {
  if (typeof window === "undefined" || !("serviceWorker" in navigator)) {
    return;
  }

  const controller = navigator.serviceWorker.controller;
  if (controller) {
    controller.postMessage(payload);
    return;
  }

  void navigator.serviceWorker.ready
    .then((registration) => {
      registration.active?.postMessage(payload);
    })
    .catch(() => {
      // Si el SW aun no esta listo, simplemente omitimos esta señal.
    });
}

export default function ChatWindow({
  isOpen,
  reservaId,
  currentUserId,
  currentUserRole,
  currentUserName,
  counterpartUserId,
  counterpartName,
  mascotaNombre,
  onClose
}: ChatWindowProps) {
  const modalRef = useRef<HTMLDivElement | null>(null);
  const inputRef = useRef<HTMLTextAreaElement | null>(null);
  const scrollContainerRef = useRef<HTMLDivElement | null>(null);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const [showJumpToLatest, setShowJumpToLatest] = useState(false);

  const {
    messages,
    draft,
    setDraft,
    typingLabel,
    connectionState,
    isSending,
    historyError,
    sendError,
    sendMessage,
    retryHistory,
    clearSendError
  } = useReservaChat({
    isOpen,
    reservaId,
    currentUserId,
    currentUserRole,
    currentUserName,
    counterpartUserId,
    counterpartName
  });

  const canSend = draft.trim().length > 0 && !isSending;

  const lastMessage = messages[messages.length - 1] ?? null;
  const lastMessageIsOwn = lastMessage?.senderUserId === currentUserId;

  useEffect(() => {
    if (!isOpen) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    window.setTimeout(() => inputRef.current?.focus(), 20);
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    const emitPresence = (isChatOpen: boolean) => {
      postChatVisibilityToServiceWorker({
        type: CHAT_VISIBILITY_EVENT,
        idReserva: reservaId,
        isChatOpen,
        visibilityState: document.visibilityState,
        focused: document.hasFocus()
      });
    };

    const handlePresenceChange = () => {
      emitPresence(true);
    };

    emitPresence(true);

    document.addEventListener("visibilitychange", handlePresenceChange);
    window.addEventListener("focus", handlePresenceChange);
    window.addEventListener("blur", handlePresenceChange);

    return () => {
      emitPresence(false);
      document.removeEventListener("visibilitychange", handlePresenceChange);
      window.removeEventListener("focus", handlePresenceChange);
      window.removeEventListener("blur", handlePresenceChange);
    };
  }, [isOpen, reservaId]);

  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
        return;
      }

      if (event.key !== "Tab" || !modalRef.current) {
        return;
      }

      const focusable = modalRef.current.querySelectorAll<HTMLElement>(
        'button, textarea, [href], input, select, [tabindex]:not([tabindex="-1"])'
      );
      if (focusable.length === 0) return;

      const first = focusable[0];
      const last = focusable[focusable.length - 1];

      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, onClose]);

  useEffect(() => {
    if (!isOpen || !scrollContainerRef.current) return;
    const container = scrollContainerRef.current;
    const nearBottom = isNearBottom(container);

    if (nearBottom || lastMessageIsOwn || messages.length <= 1) {
      messagesEndRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
      setShowJumpToLatest(false);
      return;
    }

    setShowJumpToLatest(true);
  }, [isOpen, lastMessageIsOwn, messages]);

  const sortedMessages = useMemo(
    () => [...messages].sort((a, b) => a.timestamp.localeCompare(b.timestamp)),
    [messages]
  );

  function handleOverlayClick(event: ReactMouseEvent<HTMLDivElement>): void {
    if (event.target === event.currentTarget) {
      onClose();
    }
  }

  async function handleSubmit(event?: FormEvent): Promise<void> {
    event?.preventDefault();
    if (!canSend) return;
    await sendMessage();
  }

  function handleComposerKeyDown(event: ReactKeyboardEvent<HTMLTextAreaElement>): void {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      void handleSubmit();
    }
  }

  function renderMessage(message: ChatMessage) {
    const isOwn = message.senderUserId === currentUserId;
    const senderLabel = isOwn
      ? "Tu"
      : resolveChatSenderName(
          message,
          currentUserId,
          counterpartUserId,
          counterpartName
        );
    return (
      <article
        key={message.id}
        className={`${styles.messageRow} ${isOwn ? styles.messageOwn : styles.messageOther}`}
      >
        <div className={`${styles.bubble} ${isOwn ? styles.bubbleOwn : styles.bubbleOther}`}>
          <header className={styles.bubbleHeader}>
            <strong>{senderLabel}</strong>
            <span>{formatChatTimestamp(message.timestamp)}</span>
          </header>
          <p>{message.content}</p>
          {message.estado === "error" ? (
            <small className={styles.messageError}>No se pudo entregar.</small>
          ) : null}
        </div>
      </article>
    );
  }

  if (!isOpen) return null;

  return (
    <div
      className={styles.overlay}
      role="presentation"
      onClick={handleOverlayClick}
    >
      <section
        ref={modalRef}
        className={styles.modal}
        role="dialog"
        aria-modal="true"
        aria-label={`Chat de la reserva ${reservaId}`}
      >
        <header className={styles.header}>
          <div>
            <p className={styles.eyebrow}>Coordinacion del encuentro</p>
            <h2>Chat del paseo</h2>
            <p className={styles.headerMeta}>
              Reserva #{reservaId} - {mascotaNombre} con {counterpartName}
            </p>
          </div>
          <div className={styles.headerActions}>
            <span
              className={`${styles.connectionBadge} ${
                connectionState === "connected"
                  ? styles.connectionConnected
                  : connectionState === "error"
                    ? styles.connectionError
                    : styles.connectionLoading
              }`}
            >
              {getConnectionLabel(connectionState)}
            </span>
            <button type="button" className={styles.closeButton} onClick={onClose}>
              Cerrar
            </button>
          </div>
        </header>

        <div
          ref={scrollContainerRef}
          className={styles.messagesViewport}
          onScroll={() => {
            if (!scrollContainerRef.current) return;
            if (isNearBottom(scrollContainerRef.current)) {
              setShowJumpToLatest(false);
            }
          }}
        >
          {historyError ? (
            <div className={styles.stateCard} role="alert">
              <strong>No se pudo cargar el historial</strong>
              <p>{historyError}</p>
              <button type="button" onClick={() => void retryHistory()}>
                Reintentar
              </button>
            </div>
          ) : sortedMessages.length === 0 && connectionState === "loading-history" ? (
            <div className={styles.stateCard}>
              <strong>Cargando conversacion</strong>
              <p>Estamos preparando el historial de este paseo.</p>
            </div>
          ) : sortedMessages.length === 0 ? (
            <div className={styles.stateCard}>
              <strong>Aun no hay mensajes en esta conversacion</strong>
              <p>Usen este chat para coordinar el encuentro del paseo en tiempo real.</p>
            </div>
          ) : (
            sortedMessages.map(renderMessage)
          )}
          <div ref={messagesEndRef} />
        </div>

        <div className={styles.statusRow}>
          <span className={styles.typingLabel}>{typingLabel || " "}</span>
          {sendError ? (
            <button
              type="button"
              className={styles.inlineErrorButton}
              onClick={clearSendError}
            >
              {sendError}
            </button>
          ) : null}
        </div>

        {showJumpToLatest ? (
          <div className={styles.jumpRow}>
            <button
              type="button"
              className={styles.jumpButton}
              onClick={() => {
                messagesEndRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
                setShowJumpToLatest(false);
              }}
            >
              Ir al ultimo mensaje
            </button>
          </div>
        ) : null}

        <form className={styles.composer} onSubmit={(event) => void handleSubmit(event)}>
          <label htmlFor={`chat-draft-${reservaId}`} className={styles.composerLabel}>
            Escribe un mensaje
          </label>
          <textarea
            id={`chat-draft-${reservaId}`}
            ref={inputRef}
            className={styles.textarea}
            rows={3}
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            onKeyDown={handleComposerKeyDown}
            placeholder="Escribe para coordinar el encuentro..."
          />
          <div className={styles.composerFooter}>
            <span className={styles.helperText}>
              Enter envia el mensaje. Shift + Enter agrega un salto de linea.
            </span>
            <button
              type="submit"
              className={styles.sendButton}
              disabled={!canSend}
            >
              {isSending ? "Enviando..." : "Enviar"}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
