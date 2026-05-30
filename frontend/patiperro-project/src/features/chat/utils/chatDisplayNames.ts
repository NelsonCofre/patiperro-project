import type { ChatMessage } from "../types/chat.types";

const GENERIC_SENDER_LABELS = new Set(["usuario", "user"]);

function isGenericSenderLabel(name: string | undefined | null): boolean {
  const trimmed = (name ?? "").trim();
  return trimmed.length === 0 || GENERIC_SENDER_LABELS.has(trimmed.toLowerCase());
}

/**
 * Nombre visible del remitente: usa el de la contraparte de la reserva cuando el payload
 * no trae nombre (historial REST) o viene como "Usuario".
 */
export function resolveChatSenderName(
  message: ChatMessage,
  currentUserId: number,
  counterpartUserId: number | undefined,
  counterpartName: string
): string {
  const counterpart = counterpartName.trim();

  if (message.senderUserId === currentUserId) {
    return message.senderName?.trim() || "";
  }

  if (
    counterpartUserId != null &&
    counterpartUserId > 0 &&
    message.senderUserId === counterpartUserId &&
    counterpart
  ) {
    const raw = message.senderName?.trim() ?? "";
    return isGenericSenderLabel(raw) ? counterpart : raw;
  }

  const raw = message.senderName?.trim() ?? "";
  if (isGenericSenderLabel(raw)) {
    return counterpart || raw || "Usuario";
  }
  return raw;
}

export function enrichChatMessage(
  message: ChatMessage,
  currentUserId: number,
  counterpartUserId: number | undefined,
  counterpartName: string
): ChatMessage {
  return {
    ...message,
    senderName: resolveChatSenderName(
      message,
      currentUserId,
      counterpartUserId,
      counterpartName
    )
  };
}
