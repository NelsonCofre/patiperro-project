import styles from "./PaseoEnCursoCard.module.css";

type Props = {
  statusMessage: string;
  actorLabel: string;
  actorNombre: string;
  actorFotoUrl?: string | null;
  mascotaNombre: string;
  horaInicioRegistrada?: string | null;
  comuna?: string | null;
  locationLabel?: string;
  locationValue?: string | null;
  locationHint?: string;
  chatLabel?: string;
  onOpenChat?: () => void;
  /** Vista reducida para listados del paseador (sin bloques duplicados ni chat ancho). */
  variant?: "default" | "compact";
};

function formatStartTime(value?: string | null): string {
  if (!value) return "Hora pendiente de confirmación";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("es-CL", {
    day: "2-digit",
    month: "long",
    hour: "2-digit",
    minute: "2-digit"
  });
}

function getInitials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("") || "P";
}

export default function PaseoEnCursoCard({
  statusMessage,
  actorLabel,
  actorNombre,
  actorFotoUrl,
  mascotaNombre,
  horaInicioRegistrada,
  comuna,
  locationLabel = "Dirección del tutor",
  locationValue,
  locationHint,
  chatLabel = "Abrir chat",
  onOpenChat,
  variant = "default"
}: Props) {
  const isCompact = variant === "compact";
  const comunaLabel = comuna?.trim();
  const direccion = locationValue?.trim();
  const hasLocation = Boolean(comunaLabel || direccion);
  const inicioLabel = formatStartTime(horaInicioRegistrada);

  if (isCompact) {
    return (
      <section
        className={`${styles.card} ${styles.cardCompact}`}
        aria-label="Paseo en curso"
      >
        <div className={styles.compactHeader}>
          <span className={styles.liveBadgeCompact}>En curso</span>
          <p className={styles.compactStatus}>{statusMessage}</p>
          <span className={styles.compactInicio}>Inicio: {inicioLabel}</span>
        </div>
        {hasLocation ? (
          <p className={styles.locationInline}>
            <span className={styles.locationInlineLabel}>{locationLabel}</span>
            {comunaLabel ? `${comunaLabel} · ` : ""}
            {direccion || "Sin calle registrada"}
          </p>
        ) : null}
      </section>
    );
  }

  return (
    <section className={styles.card} aria-label="Paseo en curso">
      <div className={styles.header}>
        <div>
          <p className={styles.eyebrow}>Paseo en curso</p>
          <h3>{statusMessage}</h3>
        </div>
        <span className={styles.liveBadge}>EN CURSO</span>
      </div>

      <div className={styles.locationBlock}>
        <div className={styles.locationIcon} aria-hidden="true" />
        <div className={styles.locationContent}>
          <span className={styles.locationEyebrow}>{locationLabel}</span>
          {comunaLabel ? <strong className={styles.locationComuna}>{comunaLabel}</strong> : null}
          <p className={styles.locationAddress}>
            {direccion || (!comunaLabel ? "Dirección no disponible" : "Sin calle registrada")}
          </p>
          {locationHint && hasLocation ? <p className={styles.locationHint}>{locationHint}</p> : null}
          {!hasLocation ? (
            <p className={styles.locationHint}>
              Completa calle y comuna en tu perfil de tutor para que el paseador llegue al domicilio correcto.
            </p>
          ) : null}
        </div>
      </div>

      <div className={styles.profileRow}>
        {actorFotoUrl ? (
          <img src={actorFotoUrl} alt={`Foto de ${actorNombre}`} className={styles.actorPhoto} />
        ) : (
          <div className={styles.actorFallback} aria-hidden="true">
            {getInitials(actorNombre)}
          </div>
        )}

        <div className={styles.profileText}>
          <span>{actorLabel}</span>
          <strong>{actorNombre}</strong>
        </div>
      </div>

      <div className={styles.summaryGrid}>
        <div>
          <span>Mascota</span>
          <strong>{mascotaNombre}</strong>
        </div>
        <div>
          <span>Inicio registrado</span>
          <strong>{formatStartTime(horaInicioRegistrada)}</strong>
        </div>
      </div>

      <button type="button" className={styles.chatButton} onClick={onOpenChat}>
        {chatLabel}
      </button>
    </section>
  );
}
