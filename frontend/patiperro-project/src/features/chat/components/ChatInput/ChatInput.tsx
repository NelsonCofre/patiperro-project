import type { FormEvent, KeyboardEvent as ReactKeyboardEvent, RefObject } from "react";
import { useRef } from "react";
import {
  CHAT_IMAGE_VALIDATION_MESSAGE,
  validarImagenChat
} from "../../utils/chatImageUtils";
import styles from "./ChatInput.module.css";

type PendingImagePreview = {
  file: File;
  previewUrl: string;
  comentario: string;
};

type ChatInputProps = {
  reservaId: number;
  canSendPhotos: boolean;
  draft: string;
  isSending: boolean;
  pendingImage: PendingImagePreview | null;
  textareaRef: RefObject<HTMLTextAreaElement | null>;
  setDraft: (value: string) => void;
  setPendingImage: (
    updater:
      | PendingImagePreview
      | null
      | ((current: PendingImagePreview | null) => PendingImagePreview | null)
  ) => void;
  setImageValidationError: (value: string | null) => void;
  clearPendingImage: () => void;
  onSubmit: (event?: FormEvent) => Promise<void>;
};

export default function ChatInput({
  reservaId,
  canSendPhotos,
  draft,
  isSending,
  pendingImage,
  textareaRef,
  setDraft,
  setPendingImage,
  setImageValidationError,
  clearPendingImage,
  onSubmit
}: ChatInputProps) {
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const canSendText = draft.trim().length > 0 && !isSending && !pendingImage;
  const canSendPendingImage = Boolean(pendingImage) && !isSending;

  function handleComposerKeyDown(event: ReactKeyboardEvent<HTMLTextAreaElement>): void {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      void onSubmit();
    }
  }

  function handlePickImageClick(): void {
    setImageValidationError(null);
    fileInputRef.current?.click();
  }

  function handleImageSelected(event: React.ChangeEvent<HTMLInputElement>): void {
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

  return (
    <div
      className={`${styles.composerShell}${
        pendingImage ? ` ${styles.composerShellWithPreview}` : ""
      }`}
    >
      <form className={styles.composer} onSubmit={(event) => void onSubmit(event)}>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/jpeg,image/png,.jpg,.jpeg,.png,image/*"
          className={styles.hiddenFileInput}
          onChange={handleImageSelected}
        />

        {pendingImage ? (
          <div className={styles.imagePreviewPanel}>
            <div className={styles.imagePreviewHeader}>
              <strong>Vista previa de la foto</strong>
              <button type="button" onClick={clearPendingImage} aria-label="Quitar foto">
                Quitar
              </button>
            </div>
            <div className={styles.imagePreviewBody}>
              <div className={styles.imagePreviewThumb}>
                <img src={pendingImage.previewUrl} alt="Vista previa" />
              </div>
              <div className={styles.imagePreviewFields}>
                <label
                  className={styles.imageCommentLabel}
                  htmlFor={`chat-image-comment-${reservaId}`}
                >
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
          </div>
        ) : (
          <>
            <div className={styles.composerTopRow}>
              <label htmlFor={`chat-draft-${reservaId}`} className={styles.composerLabel}>
                Escribe un mensaje
              </label>
              {canSendPhotos ? (
                <button
                  type="button"
                  className={styles.cameraButton}
                  onClick={handlePickImageClick}
                  disabled={isSending}
                  title={CHAT_IMAGE_VALIDATION_MESSAGE}
                >
                  Subir foto
                </button>
              ) : null}
            </div>

            <textarea
              id={`chat-draft-${reservaId}`}
              ref={textareaRef}
              className={styles.textarea}
              rows={3}
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              onKeyDown={handleComposerKeyDown}
              placeholder="Escribe para coordinar el encuentro..."
            />
          </>
        )}

        <div className={styles.composerFooter}>
          <span className={styles.helperText}>
            {pendingImage
              ? "La foto se enviará en tiempo real. Puedes agregar un comentario opcional."
              : "Enter envía el mensaje. Shift + Enter agrega un salto de línea."}
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
    </div>
  );
}
