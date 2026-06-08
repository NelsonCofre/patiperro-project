import type { DailyScheduleItem } from "../../types/dailySchedule.types";
import styles from "./DailySchedulePanel.module.css";

type DailySchedulePanelProps = {
  tours: DailyScheduleItem[];
  isLoading: boolean;
  error: string;
  onRetry: () => void;
  onOpenTour: (tour: DailyScheduleItem) => void;
};

function parseTourStartMs(tour: DailyScheduleItem): number {
  const parsed = Date.parse(tour.inicioProgramado);
  if (Number.isFinite(parsed)) return parsed;
  const fallback = Date.parse(`${tour.fechaAgenda}T${tour.horaInicio}`);
  return Number.isFinite(fallback) ? fallback : Number.MAX_SAFE_INTEGER;
}

function summarizeTodayTours(tours: DailyScheduleItem[]): {
  nextTourId: number | null;
  upcomingCount: number;
} {
  const now = Date.now();
  const upcoming = tours
    .map((tour) => ({ tour, startMs: parseTourStartMs(tour) }))
    .filter(({ startMs }) => startMs >= now)
    .sort((a, b) => a.startMs - b.startMs);

  return {
    nextTourId: upcoming[0]?.tour.idReserva ?? null,
    upcomingCount: upcoming.length
  };
}

function formatHourRange(start: string, end: string): string {
  const normalize = (value: string): string => {
    const trimmed = value.trim();
    if (!trimmed) return "--:--";
    return trimmed.slice(0, 5);
  };

  return `${normalize(start)} - ${normalize(end)}`;
}

function formatAddress(tour: DailyScheduleItem): string {
  const direccion = tour.direccionReferencia.trim();
  const comuna = tour.comuna.trim();
  if (direccion && comuna) return `${direccion}, ${comuna}`;
  return direccion || comuna || "Ubicación por confirmar";
}

function getStatusClass(nombreEstado: string | null): string {
  const estado = (nombreEstado ?? "").toUpperCase();
  if (estado.includes("CURSO")) return styles.statusInProgress;
  if (estado.includes("FINAL")) return styles.statusFinished;
  if (estado.includes("ACEPT") || estado.includes("PAGAD") || estado.includes("CONFIRM")) {
    return styles.statusAccepted;
  }
  return styles.statusDefault;
}

function getStatusLabel(nombreEstado: string | null): string {
  const label = nombreEstado?.trim();
  return label || "Confirmado";
}

export default function DailySchedulePanel({
  tours,
  isLoading,
  error,
  onRetry,
  onOpenTour
}: DailySchedulePanelProps) {
  const { nextTourId, upcomingCount } = summarizeTodayTours(tours);

  return (
    <section className={styles.panel} aria-labelledby="daily-schedule-title">
      <div className={styles.header}>
        <div>
          <p className={styles.eyebrow}>Mis Paseos de Hoy</p>
          <h2 id="daily-schedule-title" className={styles.title}>
            Tu panel diario de servicios confirmados
          </h2>
          <p className={styles.description}>
            Revisa tus paseos del día en orden cronológico y entra rápido al flujo de gestión de cada reserva.
          </p>
        </div>

        <div className={styles.summary}>
          <span className={styles.summaryLabel}>Vista de hoy</span>
          <strong>{tours.length} paseo{tours.length === 1 ? "" : "s"} programado{tours.length === 1 ? "" : "s"}</strong>
          <small>
            {upcomingCount > 0
              ? `${upcomingCount} pendiente${upcomingCount === 1 ? "" : "s"} de iniciar`
              : "No quedan paseos pendientes para el resto del dia."}
          </small>
        </div>
      </div>

      {isLoading ? (
        <div className={styles.loading} role="status" aria-live="polite">
          Cargando tus paseos de hoy...
        </div>
      ) : error ? (
        <div className={styles.errorState} role="alert">
          <h3 className={styles.errorTitle}>No pudimos cargar tus paseos de hoy</h3>
          <p className={styles.errorText}>{error}</p>
          <button type="button" className={styles.retryButton} onClick={onRetry}>
            Reintentar
          </button>
        </div>
      ) : tours.length === 0 ? (
        <div className={styles.emptyState}>
          <h3 className={styles.emptyTitle}>Dia libre</h3>
          <p className={styles.emptyText}>No tienes paseos para hoy. Aprovecha de revisar tu agenda o preparar tu disponibilidad.</p>
        </div>
      ) : (
        <div className={styles.list}>
          {tours.map((tour) => {
            const isNext = nextTourId === tour.idReserva;
            return (
              <button
                key={tour.idReserva}
                type="button"
                className={`${styles.card} ${isNext ? styles.nextTour : ""}`}
                onClick={() => onOpenTour(tour)}
              >
                <div className={styles.cardHeader}>
                  <div>
                    <h3 className={styles.cardTitle}>{tour.mascotaNombre}</h3>
                    <span className={styles.timeRange}>
                      {formatHourRange(tour.horaInicio, tour.horaFin)}
                    </span>
                  </div>

                  <div className={styles.cardTags}>
                    {isNext ? <span className={styles.tag}>Siguiente</span> : null}
                    <span className={`${styles.statusTag} ${getStatusClass(tour.nombreEstado)}`}>
                      {getStatusLabel(tour.nombreEstado)}
                    </span>
                  </div>
                </div>

                <div className={styles.cardBody}>
                  <div>
                    <span className={styles.detailLabel}>Dirección o sector</span>
                    <span className={styles.detailValue}>{formatAddress(tour)}</span>
                  </div>
                  <div>
                    <span className={styles.detailLabel}>Comuna</span>
                    <span className={styles.detailValue}>{tour.comuna || "Por confirmar"}</span>
                  </div>
                  <div>
                    <span className={styles.detailLabel}>Gestion</span>
                    <span className={styles.detailValue}>
                      Entra al detalle de la reserva para iniciar el paseo o revisar la ubicación.
                    </span>
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      )}
    </section>
  );
}
