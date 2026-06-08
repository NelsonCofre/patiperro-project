import type { FormEvent, MouseEvent as ReactMouseEvent } from "react";
import { useEffect, useMemo, useRef, useState } from "react";
import ChatInput from "../ChatInput/ChatInput";
import ChatImageLightbox from "../ChatImageLightbox/ChatImageLightbox";
import ImageMessage from "../ImageMessage/ImageMessage";
import { useActiveChatReserva } from "../../context/ActiveChatContext";
import { useChatUnread } from "../../context/ChatUnreadContext";
import { useReservaChat } from "../../hooks/useReservaChat";
import type { ChatMessage, ChatWindowProps } from "../../types/chat.types";
import {
  formatChatTimestamp,
  isNearBottom
} from "../../utils/chatFormatters";
import { resolveChatSenderName } from "../../utils/chatDisplayNames";
import { subtituloChatPaseo } from "../../../shared/utils/displayLabels";
import styles from "./ChatWindow.module.css";

const CHAT_VISIBILITY_EVENT = "chat-visibility";

function getConnectionLabel(value: string): string {
  if (value === "loading-history") return "Cargando historial";
  if (value === "connecting") return "Conectando";
  if (value === "reconnecting") return "Reconectando";
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

type PendingImagePreview = {
  file: File;
  previewUrl: string;
  comentario: string;
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
  canSendPhotos = false,
  onClose
}: ChatWindowProps) {
  const { setActiveChatReservaId } = useActiveChatReserva();
  const { clearUnreadForReserva } = useChatUnread();
  const modalRef = useRef<HTMLDivElement | null>(null);
  const inputRef = useRef<HTMLTextAreaElement | null>(null);
  const scrollContainerRef = useRef<HTMLDivElement | null>(null);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const [showJumpToLatest, setShowJumpToLatest] = useState(false);
  const [lightboxMessage, setLightboxMessage] = useState<ChatMessage | null>(null);
  const [pendingImage, setPendingImage] = useState<PendingImagePreview | null>(null);
  const [imageValidationError, setImageValidationError] = useState<string | null>(null);

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
    sendImage,
    retryHistory,
    retryConnection,
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

  const canSendPendingImage = Boolean(pendingImage) && !isSending;

  const lastMessage = messages[messages.length - 1] ?? null;
  const lastMessageIsOwn = lastMessage?.senderUserId === currentUserId;

  useEffect(() => {
    if (!isOpen) {
      setPendingImage((current) => {
        if (current) {
          URL.revokeObjectURL(current.previewUrl);
        }
        return null;
      });
      setImageValidationError(null);
      setLightboxMessage(null);
    }
  }, [isOpen]);

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

    setActiveChatReservaId(reservaId);
    clearUnreadForReserva(reservaId);
    return () => setActiveChatReservaId(null);
  }, [clearUnreadForReserva, isOpen, reservaId, setActiveChatReservaId]);

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
        if (lightboxMessage) {
          setLightboxMessage(null);
          return;
        }
        if (pendingImage) {
          clearPendingImage();
          return;
        }
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
  }, [isOpen, lightboxMessage, onClose, pendingImage]);

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

  function clearPendingImage(): void {
    setPendingImage((current) => {
      if (current) {
        URL.revokeObjectURL(current.previewUrl);
      }
      return null;
    });
    setImageValidationError(null);
  }

  function handleOverlayClick(event: ReactMouseEvent<HTMLDivElement>): void {
    if (event.target === event.currentTarget) {
      onClose();
    }
  }

  async function handleSubmit(event?: FormEvent): Promise<void> {
    event?.preventDefault();
    const canSendText = draft.trim().length > 0 && !isSending && !pendingImage;
    if (pendingImage) {
      if (!canSendPendingImage) return;
      await sendImage(pendingImage.file, pendingImage.comentario);
      clearPendingImage();
      return;
    }
    if (!canSendText) return;
    await sendMessage();
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
    const isImage = message.tipo === "IMAGEN" && Boolean(message.imageUrl);

    return (
      <article
        key={message.id}
        className={`${styles.messageRow} ${isOwn ? styles.messageOwn : styles.messageOther}`}
      >
        <div
          className={`${styles.bubble} ${isOwn ? styles.bubbleOwn : styles.bubbleOther}${
            isImage ? ` ${styles.bubbleMedia}` : ""
          }`}
        >
          <header className={styles.bubbleHeader}>
            <strong>{senderLabel}</strong>
            <span>{formatChatTimestamp(message.timestamp)}</span>
          </header>

          {isImage ? (
            <ImageMessage
              message={message}
              onOpen={setLightboxMessage}
            />
          ) : (
            <p>{message.content}</p>
          )}

          {message.estado === "error" ? (
            <small className={styles.messageError}>No se pudo entregar.</small>
          ) : null}
        </div>
      </article>
    );
  }

  if (!isOpen) return null;

  return (
    <>
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
          aria-label={`Chat del paseo de ${mascotaNombre}`}
        >
          <header className={styles.header}>
            <div className={styles.headerMain}>
              <p className={styles.eyebrow}>Coordinacion del encuentro</p>
              <h2>Chat del paseo</h2>
              <p className={styles.headerMeta}>
                {subtituloChatPaseo({ mascotaNombre, counterpartName })}
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
              {connectionState === "reconnecting" ||
              connectionState === "connecting" ||
              connectionState === "error" ? (
                <button
                  type="button"
                  className={styles.reconnectButton}
                  onClick={() => void retryConnection()}
                >
                  Reconectar
                </button>
              ) : null}
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
                <strong>Cargando conversación</strong>
                <p>Estamos preparando el historial de este paseo.</p>
              </div>
            ) : sortedMessages.length === 0 ? (
              <div className={styles.stateCard}>
                <strong>Aún no hay mensajes en esta conversación</strong>
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
            ) : imageValidationError ? (
              <button
                type="button"
                className={styles.inlineErrorButton}
                onClick={() => setImageValidationError(null)}
              >
                {imageValidationError}
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
                Ir al último mensaje
              </button>
            </div>
          ) : null}

          <ChatInput
            reservaId={reservaId}
            canSendPhotos={canSendPhotos}
            draft={draft}
            isSending={isSending}
            pendingImage={pendingImage}
            textareaRef={inputRef}
            setDraft={setDraft}
            setPendingImage={setPendingImage}
            setImageValidationError={setImageValidationError}
            clearPendingImage={clearPendingImage}
            onSubmit={handleSubmit}
          />
        </section>
      </div>

      {lightboxMessage?.imageUrl ? (
        <ChatImageLightbox
          imageUrl={lightboxMessage.imageUrl}
          caption={lightboxMessage.content.trim() || undefined}
          onClose={() => setLightboxMessage(null)}
        />
      ) : null}
    </>
  );
}
