import { useEffect } from "react";
import type { MouseEvent as ReactMouseEvent } from "react";
import { chatMediaDownloadUrl } from "../../services/chatApi";
import { resolveChatImageSrc } from "../../utils/chatImageUtils";
import styles from "./ChatImageLightbox.module.css";

type ChatImageLightboxProps = {
  imageUrl: string;
  caption?: string;
  alt?: string;
  onClose: () => void;
};

export default function ChatImageLightbox({
  imageUrl,
  caption,
  alt = "Foto del paseo",
  onClose
}: ChatImageLightboxProps) {
  const resolvedSrc = resolveChatImageSrc(imageUrl);
  const downloadHref = chatMediaDownloadUrl(resolveChatImageSrc(imageUrl));

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [onClose]);

  function handleOverlayClick(event: ReactMouseEvent<HTMLDivElement>): void {
    if (event.target === event.currentTarget) {
      onClose();
    }
  }

  return (
    <div
      className={styles.overlay}
      role="presentation"
      onClick={handleOverlayClick}
    >
      <section
        className={styles.panel}
        role="dialog"
        aria-modal="true"
        aria-label="Vista ampliada de foto del paseo"
      >
        <div className={styles.toolbar}>
          <strong>Foto del paseo</strong>
          <div className={styles.toolbarActions}>
            <a href={downloadHref} download target="_blank" rel="noreferrer">
              Descargar
            </a>
            <button type="button" onClick={onClose}>
              Cerrar
            </button>
          </div>
        </div>

        <figure className={styles.figure}>
          <img src={resolvedSrc} alt={alt} />
          {caption?.trim() ? (
            <figcaption className={styles.caption}>{caption.trim()}</figcaption>
          ) : null}
        </figure>
      </section>
    </div>
  );
}
