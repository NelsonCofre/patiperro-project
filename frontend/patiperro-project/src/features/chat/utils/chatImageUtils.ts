import { resolveApiUrl } from "../../../config/api";

export const CHAT_IMAGE_MAX_BYTES = 10 * 1024 * 1024;

export const CHAT_IMAGE_VALIDATION_MESSAGE =
  "Solo puedes enviar imágenes (JPG/PNG) de hasta 10MB para asegurar la rapidez del chat";

export function validarImagenChat(file: File): string | null {
  if (!file) return CHAT_IMAGE_VALIDATION_MESSAGE;
  if (file.size > CHAT_IMAGE_MAX_BYTES) return CHAT_IMAGE_VALIDATION_MESSAGE;

  const name = file.name.toLowerCase();
  if (!name.endsWith(".jpg") && !name.endsWith(".jpeg") && !name.endsWith(".png")) {
    return CHAT_IMAGE_VALIDATION_MESSAGE;
  }

  const mime = file.type.toLowerCase().split(";")[0].trim();
  if (mime && mime !== "image/jpeg" && mime !== "image/png") {
    return CHAT_IMAGE_VALIDATION_MESSAGE;
  }

  return null;
}

export function resolveChatImageSrc(imageUrl?: string): string {
  if (!imageUrl) return "";
  if (imageUrl.startsWith("blob:")) return imageUrl;
  return resolveApiUrl(imageUrl);
}
