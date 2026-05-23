import type { ChangeEvent, FormEvent, KeyboardEvent as ReactKeyboardEvent, MouseEvent as ReactMouseEvent } from "react";
import { useEffect, useMemo, useRef, useState } from "react";
import ChatImageLightbox from "../ChatImageLightbox/ChatImageLightbox";
import { useReservaChat } from "../../hooks/useReservaChat";
import type { ChatMessage, ChatWindowProps } from "../../types/chat.types";
import {
  formatChatTimestamp,
  isNearBottom
} from "../../utils/chatFormatters";
import {
  CHAT_IMAGE_VALIDATION_MESSAGE,
  resolveChatImageSrc,
  validarImagenChat
} from "../../utils/chatImageUtils";
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
  const modalRef = useRef<HTMLDivElement | null>(null);
  const inputRef = useRef<HTMLTextAreaElement | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
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

  const canSendText = draft.trim().length > 0 && !isSending && !pendingImage;
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
    if (pendingImage) {
      if (!canSendPendingImage) return;
      await sendImage(pendingImage.file, pendingImage.comentario);
      clearPendingImage();
      return;
    }
    if (!canSendText) return;
    await sendMessage();
  }

  function handleComposerKeyDown(event: ReactKeyboardEvent<HTMLTextAreaElement>): void {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      void handleSubmit();
    }
  }

  function handlePickImageClick(): void {
    setImageValidationError(null);
    fileInputRef.current?.click();
  }

  function handleImageSelected(event: ChangeEvent<HTMLInputElement>): void {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;

    const validationError = validarImagenChat(file);
    if (validationError) {
      setImageValidationError(validationError);
      return;
    }

    clearPendingImage();
    setPendingImage({
      file,
      previewUrl: URL.createObjectURL(file),
      comentario: ""
    });
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
        <div className={`${styles.bubble} ${isOwn ? styles.bubbleOwn : styles.bubbleOther}`}>
          <header className={styles.bubbleHeader}>
            <strong>{senderLabel}</strong>
            <span>{formatChatTimestamp(message.timestamp)}</span>
          </header>

          {isImage ? (
            <button
              type="button"
              className={styles.imageButton}
              onClick={() => setLightboxMessage(message)}
            >
              <img
                src={resolveChatImageSrc(message.imageUrl)}
                alt={message.content.trim() || "Foto del paseo"}
                className={styles.messageImage}
              />
            </button>
          ) : (
            <p>{message.content}</p>
          )}

          {isImage && message.content.trim() ? (
            <p className={styles.imageCaption}>{message.content.trim()}</p>
          ) : null}

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
                Ir al ultimo mensaje
              </button>
            </div>
          ) : null}

          {pendingImage ? (
            <div className={styles.imagePreviewPanel}>
              <div className={styles.imagePreviewHeader}>
                <strong>Vista previa de la foto</strong>
                <button type="button" onClick={clearPendingImage} aria-label="Quitar foto">
                  Quitar
                </button>
              </div>
              <div className={styles.imagePreviewBody}>
                <img src={pendingImage.previewUrl} alt="Vista previa" />
                <label className={styles.imageCommentLabel} htmlFor={`chat-image-comment-${reservaId}`}>
                  Comentario opcional
                </label>
                <textarea
                  id={`chat-image-comment-${reservaId}`}
                  className={styles.imageCommentInput}
                  rows={2}
                  value={pendingImage.comentario}
                  onChange={(event) =>
                    setPendingImage((current) =>
                      current ? { ...current, comentario: event.target.value } : current
                    )
                  }
                  placeholder="Ej: Jugando en el parque"
                />
              </div>
            </div>
          ) : null}

          <form className={styles.composer} onSubmit={(event) => void handleSubmit(event)}>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/jpeg,image/png,.jpg,.jpeg,.png"
              className={styles.hiddenFileInput}
              onChange={handleImageSelected}
            />

            <div className={styles.composerTopRow}>
              <label htmlFor={`chat-draft-${reservaId}`} className={styles.composerLabel}>
                {pendingImage ? "Enviar foto con comentario opcional" : "Escribe un mensaje"}
              </label>
              {canSendPhotos ? (
                <button
                  type="button"
                  className={styles.cameraButton}
                  onClick={handlePickImageClick}
                  disabled={isSending || Boolean(pendingImage)}
                  title={CHAT_IMAGE_VALIDATION_MESSAGE}
                >
                  Subir foto
                </button>
              ) : null}
            </div>

            {!pendingImage ? (
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
            ) : null}

            <div className={styles.composerFooter}>
              <span className={styles.helperText}>
                {pendingImage
                  ? "La foto se enviara al tutor en tiempo real."
                  : "Enter envia el mensaje. Shift + Enter agrega un salto de linea."}
              </span>
              <button
                type="submit"
                className={styles.sendButton}
                disabled={pendingImage ? !canSendPendingImage : !canSendText}
              >
                {isSending ? "Enviando..." : pendingImage ? "Enviar foto" : "Enviar"}
              </button>
            </div>
          </form>
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
