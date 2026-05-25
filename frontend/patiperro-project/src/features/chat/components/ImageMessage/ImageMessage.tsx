import type { ChatMessage } from "../../types/chat.types";
import { resolveChatImageSrc } from "../../utils/chatImageUtils";
import styles from "./ImageMessage.module.css";

type ImageMessageProps = {
  message: ChatMessage;
  onOpen: (message: ChatMessage) => void;
};

export default function ImageMessage({ message, onOpen }: ImageMessageProps) {
  if (!message.imageUrl) return null;

  return (
    <>
      <button
        type="button"
        className={styles.imageButton}
        onClick={() => onOpen(message)}
      >
        <img
          src={resolveChatImageSrc(message.imageUrl)}
          alt={message.content.trim() || "Foto del paseo"}
          className={styles.messageImage}
        />
      </button>

      {message.content.trim() ? (
        <p className={styles.imageCaption}>{message.content.trim()}</p>
      ) : null}
    </>
  );
}
