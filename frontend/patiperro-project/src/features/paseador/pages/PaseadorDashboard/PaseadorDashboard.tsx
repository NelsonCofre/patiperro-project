// Home principal del paseador.
// Resume perfil, agenda y metricas iniciales del rol.
import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import DailySchedulePanel from "../../components/DailySchedulePanel/DailySchedulePanel";
import PaseadorNavbar from "../../components/PaseadorNavbar/PaseadorNavbar";
import { fetchDailySchedulePanel } from "../../services/dailyScheduleService";
import type { DailyScheduleItem } from "../../types/dailySchedule.types";
import styles from "./PaseadorDashboard.module.css";

const METRICS = [
  {
    label: "Paseos Realizados",
    value: "0",
    helper: "Tu historial aparecerá aquí cuando completes tu perfil."
  },
  {
    label: "Calificacion Media",
    value: "0",
    helper: "Se actualizara cuando recibas tus primeras resenas."
  },
  {
    label: "Saldo Disponible",
    value: "$0",
    helper: "Veras tus pagos pendientes y disponibles en este panel."
  }
];

const AGENDA_DAYS = ["Lun", "Mar", "Mie", "Jue", "Vie", "Sab", "Dom"];
const PROFILE_CHECKLIST = [
  { step: "01", text: "Definir que tipo de servicio ofreces" },
  { step: "02", text: "Completar tu disponibilidad semanal" },
  { step: "03", text: "Revisar la informacion visible de tu perfil" }
];

export default function PaseadorDashboard() {
  const navigate = useNavigate();
  const profileStatus = "Incompleto";
  const [dailyTours, setDailyTours] = useState<DailyScheduleItem[]>([]);
  const [isLoadingDailyTours, setIsLoadingDailyTours] = useState(true);
  const [dailyToursError, setDailyToursError] = useState("");

  async function loadDailyTours() {
    setIsLoadingDailyTours(true);
    setDailyToursError("");
    try {
      const data = await fetchDailySchedulePanel();
      setDailyTours(data);
    } catch (error) {
      setDailyToursError(
        error instanceof Error ? error.message : "No se pudieron cargar los paseos de hoy."
      );
    } finally {
      setIsLoadingDailyTours(false);
    }
  }

  useEffect(() => {
    void loadDailyTours();
  }, []);

  function openTourDetail(tour: DailyScheduleItem) {
    const estado = (tour.nombreEstado ?? "").toUpperCase();
    if (estado.includes("CURSO")) {
      navigate("/paseador/dashboard/solicitudes/en-curso");
      return;
    }
    navigate("/paseador/dashboard/solicitudes/aceptadas");
  }

  return (
    <main className={styles.page}>
      <PaseadorNavbar />

      <section className={styles.hero}>
        <div className={styles.heroContent}>
          <p className={styles.eyebrow}>Home del Paseador</p>
          <h1 className={styles.title}>Gestiona tu servicio desde un solo lugar</h1>
          <p className={styles.description}>
            Completa tu perfil profesional, define tu disponibilidad y sigue tu
            actividad inicial dentro de Patiperro.
          </p>

          <div className={styles.heroRibbon}>
            <span className={styles.heroRibbonTag}>Dashboard inicial</span>
            <p>
              Este espacio te ayuda a activar tu perfil paso a paso, sin perder
              de vista tus prioridades del dia.
            </p>
          </div>

          <div className={styles.heroHighlights}>
            <div className={styles.highlightCard}>
              <span className={styles.highlightLabel}>Siguiente paso recomendado</span>
              <strong>Configura tu servicio para poder aparecer como disponible</strong>
            </div>
            <div className={styles.highlightCard}>
              <span className={styles.highlightLabel}>Enfoque del dia</span>
              <strong>Deja lista tu agenda semanal antes de comenzar a recibir paseos</strong>
            </div>
          </div>
        </div>

        <div className={styles.heroBadge}>
          <span className={styles.heroBadgeLabel}>Estado actual</span>
          <strong>{profileStatus}</strong>
          <p className={styles.heroBadgeText}>
            Te faltan algunos pasos para activar tu presencia dentro de la
            plataforma.
          </p>
          <div className={styles.heroBadgeMeta}>
            <span>Perfil profesional</span>
            <span>Agenda semanal</span>
          </div>
        </div>
      </section>

      <section className={styles.metricsSection}>
        <div className={styles.sectionHeading}>
          <p className={styles.cardEyebrow}>Resumen de actividad</p>
          <h2>Tus metricas iniciales</h2>
        </div>

        <div className={styles.metricsGrid}>
          {METRICS.map((metric) => (
            <article key={metric.label} className={styles.metricCard}>
              <span className={styles.metricLabel}>{metric.label}</span>
              <strong className={styles.metricValue}>{metric.value}</strong>
              <p className={styles.metricHelper}>{metric.helper}</p>
            </article>
          ))}
        </div>
      </section>

      <DailySchedulePanel
        tours={dailyTours}
        isLoading={isLoadingDailyTours}
        error={dailyToursError}
        onRetry={() => void loadDailyTours()}
        onOpenTour={openTourDetail}
      />

      <section className={styles.contentGrid}>
        <article className={`${styles.card} ${styles.profileCard}`}>
          <div className={styles.cardHeader}>
            <div>
              <p className={styles.cardEyebrow}>Perfil de Paseador</p>
              <h2>Configura tu presencia en la plataforma</h2>
            </div>
            <span className={styles.statusChip}>{profileStatus}</span>
          </div>

          <p className={styles.cardText}>
            Completa la configuracion de tu perfil para comenzar a mostrar tus
            servicios, definir que tipo de paseos ofreces y preparar tu cuenta
            para futuras reservas.
          </p>

          <div className={styles.profileChecklist}>
            {PROFILE_CHECKLIST.map((item) => (
              <div key={item.step} className={styles.checkItem}>
                <span className={styles.checkIcon}>{item.step}</span>
                <span>{item.text}</span>
              </div>
            ))}
          </div>

          <div className={styles.actionRow}>
            <Link
              to="/paseador/dashboard/configuracion"
              className={styles.primaryButton}
            >
              Configurar mi Servicio
            </Link>
            <button type="button" className={styles.secondaryButton}>
              Editar Perfil de Paseador
            </button>
          </div>
        </article>

        <article className={`${styles.card} ${styles.agendaCard}`}>
          <div className={styles.cardHeader}>
            <div>
              <p className={styles.cardEyebrow}>Mi Agenda</p>
              <h2>Define tu disponibilidad</h2>
            </div>
          </div>

          <p className={styles.cardText}>
            Marca los dias y bloques horarios en los que puedes trabajar para
            empezar a organizar tu servicio.
          </p>

          <div className={styles.agendaSummary}>
            <div className={styles.agendaSummaryCard}>
              <span className={styles.agendaSummaryLabel}>Bloques activos</span>
              <strong>0</strong>
            </div>
            <div className={styles.agendaSummaryCard}>
              <span className={styles.agendaSummaryLabel}>Proximo objetivo</span>
              <strong>Crear tu primera semana laboral</strong>
            </div>
          </div>

          <div className={styles.miniCalendar}>
            {AGENDA_DAYS.map((day) => (
              <div key={day} className={styles.dayPill}>
                {day}
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
