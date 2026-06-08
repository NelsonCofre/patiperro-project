const ALLOWED_IMAGE_TYPES = ["image/jpeg", "image/png", "image/gif", "image/webp"];
const MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024;

export function validateProfilePhoto(file: File | null): string | undefined {
  if (!file) return "Debes seleccionar una imagen";

  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    return "Formato no permitido. Usa JPG, PNG, GIF o WEBP.";
  }

  if (file.size > MAX_IMAGE_SIZE_BYTES) {
    return "La imagen es demasiado pesada. El máximo permitido es 10 MB.";
  }

  return undefined;
}
