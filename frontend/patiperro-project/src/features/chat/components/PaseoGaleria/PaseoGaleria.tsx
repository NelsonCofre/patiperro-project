import { useEffect, useState } from "react";
import ChatImageLightbox from "../ChatImageLightbox/ChatImageLightbox";
import { fetchGaleriaPaseo, type GaleriaPaseoItem } from "../../services/chatApi";
import { formatChatTimestamp } from "../../utils/chatFormatters";
import { resolveChatImageSrc } from "../../utils/chatImageUtils";
import styles from "./PaseoGaleria.module.css";

type PaseoGaleriaProps = {
  reservaId: number;
  idUsuario: number;
};

export default function PaseoGaleria({ reservaId, idUsuario }: PaseoGaleriaProps) {
  const [items, setItems] = useState<GaleriaPaseoItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<GaleriaPaseoItem | null>(null);

  useEffect(() => {
    if (!Number.isFinite(reservaId) || reservaId <= 0 || !Number.isFinite(idUsuario) || idUsuario <= 0) {
      setItems([]);
      setIsLoading(false);
      return;
    }

    let active = true;
    setIsLoading(true);
    setError(null);

    void fetchGaleriaPaseo(reservaId, idUsuario)
      .then((data) => {
        if (!active) return;
        setItems(data);
      })
      .catch((err) => {
        if (!active) return;
        setError(err instanceof Error ? err.message : "No se pudo cargar la galería.");
        setItems([]);
      })
      .finally(() => {
        if (active) {
          setIsLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [idUsuario, reservaId]);

  return (
    <section className={styles.section} aria-label="Galería del paseo">
      <div className={styles.header}>
        <h3>Fotos del paseo</h3>
        {!isLoading && items.length > 0 ? (
          <span>{items.length} foto{items.length === 1 ? "" : "s"}</span>
        ) : null}
      </div>

      {isLoading ? (
        <p className={styles.stateText}>Cargando fotos del paseo...</p>
      ) : error ? (
        <p className={styles.errorText} role="alert">
          {error}
        </p>
      ) : items.length === 0 ? (
        <p className={styles.stateText}>
          El paseador no compartió fotos durante este paseo.
        </p>
      ) : (
        <div className={styles.grid}>
          {items.map((item) => (
            <button
              key={item.idMensaje}
              type="button"
              className={styles.thumbButton}
              onClick={() => setSelected(item)}
            >
              <img
                src={resolveChatImageSrc(item.imageUrl)}
                alt={item.content.trim() || "Foto del paseo"}
              />
              {item.content.trim() ? (
                <span className={styles.thumbCaption}>{item.content.trim()}</span>
              ) : (
                <span className={styles.thumbCaption}>
                  {formatChatTimestamp(item.timestamp)}
                </span>
              )}
            </button>
          ))}
        </div>
      )}

      {selected ? (
        <ChatImageLightbox
          imageUrl={selected.imageUrl}
          caption={selected.content.trim() || undefined}
          onClose={() => setSelected(null)}
        />
      ) : null}
    </section>
  );
}
