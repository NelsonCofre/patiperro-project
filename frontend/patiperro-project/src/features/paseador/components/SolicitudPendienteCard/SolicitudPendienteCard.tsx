import type {
  DecisionSolicitud,
  SolicitudPendientePaseador
} from "../../types/solicitudPaseador.types";
import styles from "./SolicitudPendienteCard.module.css";

type SolicitudPendienteCardProps = {
  solicitud: SolicitudPendientePaseador;
  processingDecision: DecisionSolicitud | null;
  onAccept: (solicitud: SolicitudPendientePaseador) => void;
  onReject: (solicitud: SolicitudPendientePaseador) => void;
  onViewTutor: (solicitud: SolicitudPendientePaseador) => void;
  onViewMap: (solicitud: SolicitudPendientePaseador) => void;
};

const currencyFormatter = new Intl.NumberFormat("es-CL", {
  style: "currency",
  currency: "CLP",
  maximumFractionDigits: 0
});

const dateFormatter = new Intl.DateTimeFormat("es-CL", {
  weekday: "short",
  day: "2-digit",
  month: "short"
});

function formatDateLabel(value: string): string {
  const date = new Date(`${value}T12:00:00`);
  if (Number.isNaN(date.getTime())) return value;
  return dateFormatter.format(date);
}

export default function SolicitudPendienteCard({
  solicitud,
  processingDecision,
  onAccept,
  onReject,
  onViewTutor,
  onViewMap
}: SolicitudPendienteCardProps) {
  const isProcessing = processingDecision != null;
  const accepting = processingDecision === "ACEPTAR";
  const rejecting = processingDecision === "RECHAZAR";

  return (
    <article className={styles.card}>
      <div className={styles.photoColumn}>
        <img
          src={solicitud.mascotaFotoUrl}
          alt={`Foto de ${solicitud.mascotaNombre}`}
          className={styles.petPhoto}
        />
      </div>

      <div className={styles.content}>
        <div className={styles.header}>
          <div>
            <span className={styles.eyebrow}>Solicitud #{solicitud.idReserva}</span>
            <h3>{solicitud.mascotaNombre}</h3>
            <p>
              {solicitud.mascotaRaza} - {solicitud.mascotaTamano} - Tutor:{" "}
              {solicitud.tutorNombre}
            </p>
          </div>
          <span className={styles.status}>{solicitud.estado}</span>
        </div>

        <div className={styles.detailsGrid}>
          <div>
            <span>Fecha</span>
            <strong>{formatDateLabel(solicitud.fecha)}</strong>
          </div>
          <div>
            <span>Horario</span>
            <strong>
              {solicitud.horaInicio} - {solicitud.horaFin}
            </strong>
          </div>
          <div>
            <span>Monto</span>
            <strong>{currencyFormatter.format(solicitud.montoTotal)}</strong>
          </div>
        </div>

        <div className={styles.locationBox}>
          <div className={styles.locationHeader}>
            <span>{solicitud.comuna}</span>
            <button
              type="button"
              className={styles.mapButton}
              onClick={() => onViewMap(solicitud)}
            >
              Ver mapa
            </button>
          </div>
          <p>{solicitud.direccionReferencia}</p>
        </div>

        <div className={styles.actions}>
          <button
            type="button"
            className={styles.tutorButton}
            onClick={() => onViewTutor(solicitud)}
            disabled={isProcessing}
          >
            Ver detalle
          </button>
          <button
            type="button"
            className={styles.rejectButton}
            onClick={() => onReject(solicitud)}
            disabled={isProcessing}
          >
            {rejecting ? <span className={styles.spinner} aria-hidden="true" /> : null}
            {rejecting ? "Rechazando..." : "Rechazar"}
          </button>
          <button
            type="button"
            className={styles.acceptButton}
            onClick={() => onAccept(solicitud)}
            disabled={isProcessing}
          >
            {accepting ? <span className={styles.spinner} aria-hidden="true" /> : null}
            {accepting ? "Aceptando..." : "Aceptar"}
          </button>
        </div>
      </div>
    </article>
  );
}
