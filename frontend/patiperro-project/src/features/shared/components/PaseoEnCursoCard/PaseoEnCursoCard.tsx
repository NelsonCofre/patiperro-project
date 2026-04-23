import styles from "./PaseoEnCursoCard.module.css";

type Props = {
  statusMessage: string;
  actorLabel: string;
  actorNombre: string;
  actorFotoUrl?: string | null;
  mascotaNombre: string;
  horaInicioRegistrada?: string | null;
  locationLabel?: string;
  locationValue?: string | null;
  chatLabel?: string;
  onOpenChat?: () => void;
};

function formatStartTime(value?: string | null): string {
  if (!value) return "Hora pendiente de confirmacion";
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
  locationLabel,
  locationValue,
  chatLabel = "Abrir chat",
  onOpenChat
}: Props) {
  return (
    <section className={styles.card} aria-label="Paseo en curso">
      <div className={styles.header}>
        <div>
          <p className={styles.eyebrow}>Paseo en curso</p>
          <h3>{statusMessage}</h3>
        </div>
        <span className={styles.liveBadge}>EN_CURSO</span>
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
        {locationLabel && locationValue ? (
          <div>
            <span>{locationLabel}</span>
            <strong>{locationValue}</strong>
          </div>
        ) : null}
      </div>

      <button type="button" className={styles.chatButton} onClick={onOpenChat}>
        {chatLabel}
      </button>
    </section>
  );
}
