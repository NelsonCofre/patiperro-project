// Home principal del paseador.
// Resume perfil, agenda y métricas reales del rol.
import { Link, useNavigate } from "react-router-dom";
import DailySchedulePanel from "../../components/DailySchedulePanel/DailySchedulePanel";
import PaseadorNavbar from "../../components/PaseadorNavbar/PaseadorNavbar";
import { usePaseadorDashboardSummary } from "../../hooks/usePaseadorDashboardSummary";
import type { DailyScheduleItem } from "../../types/dailySchedule.types";
import styles from "./PaseadorDashboard.module.css";

export default function PaseadorDashboard() {
  const navigate = useNavigate();
  const {
    isLoading,
    loadError,
    metrics,
    profileStatus,
    checklist,
    bloquesActivos,
    agendaObjective,
    heroHighlights,
    activeWeekdays,
    weekdayPills,
    nombrePaseador,
    dailyTours,
    dailyToursError,
    reload
  } = usePaseadorDashboardSummary();

  function openTourDetail(tour: DailyScheduleItem) {
    const estado = (tour.nombreEstado ?? "").toUpperCase();
    if (estado.includes("CURSO")) {
      navigate("/paseador/dashboard/solicitudes/en-curso");
      return;
    }
    navigate("/paseador/dashboard/solicitudes/aceptadas");
  }

  const checklistDone = checklist.filter((item) => item.done).length;

  return (
    <main className={styles.page}>
      <PaseadorNavbar />

      <section className={styles.hero}>
        <div className={styles.heroContent}>
          <p className={styles.eyebrow}>Home del paseador</p>
          <h1 className={styles.title}>
            {nombrePaseador ? `Hola, ${nombrePaseador.split(" ")[0]}` : "Gestiona tu servicio desde un solo lugar"}
          </h1>
          <p className={styles.description}>
            Revisa tu actividad, disponibilidad y saldo con datos actualizados de Patiperro.
          </p>

          {loadError ? (
            <div className={styles.heroRibbon} role="alert">
              <span className={styles.heroRibbonTag}>Aviso</span>
              <p>{loadError}</p>
            </div>
          ) : null}

          <div className={styles.heroHighlights}>
            {heroHighlights.map((item) => (
              <div key={item.label} className={styles.highlightCard}>
                <span className={styles.highlightLabel}>{item.label}</span>
                <strong>{item.text}</strong>
              </div>
            ))}
          </div>
        </div>

        <div className={styles.heroBadge}>
          <span className={styles.heroBadgeLabel}>Estado del perfil</span>
          <strong>{isLoading ? "Cargando…" : profileStatus}</strong>
          <p className={styles.heroBadgeText}>
            {checklistDone === checklist.length && checklist.length > 0
              ? "Completaste los pasos clave para operar en la plataforma."
              : "Completa los pasos pendientes para maximizar tu visibilidad."}
          </p>
          <div className={styles.heroBadgeMeta}>
            <span>{bloquesActivos > 0 ? `${bloquesActivos} bloques activos` : "Sin bloques activos"}</span>
            <span>{dailyTours.length > 0 ? `${dailyTours.length} paseo(s) hoy` : "Sin paseos hoy"}</span>
          </div>
        </div>
      </section>

      <section className={styles.metricsSection}>
        <div className={styles.sectionHeading}>
          <p className={styles.cardEyebrow}>Resumen de actividad</p>
          <h2>Tus métricas</h2>
        </div>

        <div className={styles.metricsGrid}>
          {metrics.map((metric) => (
            <article key={metric.label} className={styles.metricCard}>
              <span className={styles.metricLabel}>{metric.label}</span>
              <strong className={styles.metricValue}>{isLoading ? "…" : metric.value}</strong>
              <p className={styles.metricHelper}>{metric.helper}</p>
            </article>
          ))}
        </div>
      </section>

      <DailySchedulePanel
        tours={dailyTours}
        isLoading={isLoading}
        error={dailyToursError}
        onRetry={() => void reload()}
        onOpenTour={openTourDetail}
      />

      <section className={styles.contentGrid}>
        <article className={`${styles.card} ${styles.profileCard}`}>
          <div className={styles.cardHeader}>
            <div>
              <p className={styles.cardEyebrow}>Perfil de paseador</p>
              <h2>Configura tu presencia en la plataforma</h2>
            </div>
            <span className={styles.statusChip}>{isLoading ? "…" : profileStatus}</span>
          </div>

          <p className={styles.cardText}>
            Completa la configuración de tu perfil para publicar tarifas, mostrar disponibilidad y
            recibir reservas con confianza.
          </p>

          <div className={styles.profileChecklist}>
            {checklist.map((item) => (
              <Link
                key={item.step}
                to={item.route}
                className={`${styles.checkItem} ${item.done ? styles.checkItemDone : ""}`}
              >
                <span className={styles.checkIcon}>{item.done ? "✓" : item.step}</span>
                <span>{item.text}</span>
              </Link>
            ))}
          </div>

          <div className={styles.actionRow}>
            <Link
              to="/paseador/dashboard/configuracion"
              className={styles.primaryButton}
            >
              Configurar mi servicio
            </Link>
            <Link to="/paseador/dashboard/perfil" className={styles.secondaryButton}>
              Editar perfil
            </Link>
          </div>
        </article>

        <article className={`${styles.card} ${styles.agendaCard}`}>
          <div className={styles.cardHeader}>
            <div>
              <p className={styles.cardEyebrow}>Mi agenda</p>
              <h2>Define tu disponibilidad</h2>
            </div>
          </div>

          <p className={styles.cardText}>
            Marca los días y bloques horarios en los que puedes trabajar para recibir solicitudes
            acordes a tu rutina.
          </p>

          <div className={styles.agendaSummary}>
            <div className={styles.agendaSummaryCard}>
              <span className={styles.agendaSummaryLabel}>Bloques activos</span>
              <strong>{isLoading ? "…" : bloquesActivos}</strong>
            </div>
            <div className={styles.agendaSummaryCard}>
              <span className={styles.agendaSummaryLabel}>Próximo objetivo</span>
              <strong>{isLoading ? "…" : agendaObjective}</strong>
            </div>
          </div>

          <div className={styles.miniCalendar}>
            {weekdayPills.map((day) => (
              <div
                key={day.short}
                className={`${styles.dayPill} ${activeWeekdays.has(day.short) ? styles.dayPillActive : ""}`}
              >
                {day.short}
              </div>
            ))}
          </div>

          <Link to="/paseador/dashboard/agenda" className={styles.primaryButton}>
            Configurar disponibilidad
          </Link>
        </article>
      </section>
    </main>
  );
}
