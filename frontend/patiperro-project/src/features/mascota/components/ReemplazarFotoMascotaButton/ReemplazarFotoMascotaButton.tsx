import { useRef, useState, type ChangeEvent } from "react";
import { uploadMascotaFotoPerfil } from "../../services/mascotaService";
import { validateMascotaPhoto } from "../../utils/mascotaValidators";
import { resolveApiUrl } from "../../../../config/api";
import type { MascotaListItem } from "../../types/mascota.types";
import styles from "./ReemplazarFotoMascotaButton.module.css";

type Props = {
  mascota: MascotaListItem;
  onPhotoUpdated: (idMascota: number, fotoPerfilPath: string, fotoPerfilUrl: string) => void;
};

export default function ReemplazarFotoMascotaButton({ mascota, onPhotoUpdated }: Props) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [statusMessage, setStatusMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const tieneFoto = Boolean(mascota.fotoPerfilPath);
  const buttonLabel = isUploading
    ? "Guardando foto..."
    : tieneFoto
      ? "Reemplazar foto"
      : "Subir foto";

  async function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null;
    event.target.value = "";

    if (!file) return;

    setErrorMessage("");
    setStatusMessage("");

    const validationError = validateMascotaPhoto(file, { required: true });
    if (validationError) {
      setErrorMessage(validationError);
      return;
    }

    setIsUploading(true);
    try {
      const result = await uploadMascotaFotoPerfil(mascota.idMascota, file);
      const fotoPerfilUrl = resolveApiUrl(result.fotoPerfil);
      onPhotoUpdated(mascota.idMascota, result.fotoPerfil, fotoPerfilUrl);
      setStatusMessage(
        tieneFoto
          ? "Foto actualizada. La imagen anterior fue reemplazada."
          : "Foto guardada correctamente."
      );
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : "No se pudo guardar la foto.");
    } finally {
      setIsUploading(false);
    }
  }

  return (
    <div className={styles.wrap}>
      <input
        ref={inputRef}
        type="file"
        accept="image/jpeg,image/png"
        className={styles.hiddenInput}
        aria-hidden="true"
        tabIndex={-1}
        onChange={(event) => void handleFileChange(event)}
      />
      <button
        type="button"
        className={styles.button}
        disabled={isUploading}
        onClick={() => inputRef.current?.click()}
      >
        {buttonLabel}
      </button>
      {statusMessage ? <p className={styles.success}>{statusMessage}</p> : null}
      {errorMessage ? (
        <p className={styles.error} role="alert">
          {errorMessage}
        </p>
      ) : null}
    </div>
  );
}
