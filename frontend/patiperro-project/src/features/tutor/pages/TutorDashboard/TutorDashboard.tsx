// Home principal del tutor.
// Prioriza la accion de registrar una mascota y resume el estado inicial del rol.
import { Link } from "react-router-dom";
import TutorNavbar from "../../components/TutorNavbar/TutorNavbar";
import styles from "./TutorDashboard.module.css";

const NEXT_STEPS = [
  "Registrar los datos principales de tu mascota",
  "Anadir una foto clara para reconocerla mejor",
  "Completar cuidados y comportamiento para futuros paseos"
];

const EMPTY_CARDS = [
  {
    label: "Mascotas registradas",
    value: "0",
    helper: "Cuando anadas tu primera mascota, aparecera resumida aqui."
  },
  {
    label: "Perfiles completos",
    value: "0",
    helper: "Te ayudara a saber cuantas mascotas tienen toda su informacion lista."
  },
  {
    label: "Favoritos del tutor",
    value: "0",
    helper: "Este espacio puede crecer mas adelante con accesos a paseadores guardados."
  }
];

export default function TutorDashboard() {
  return (
    <main className={styles.page}>
      <TutorNavbar />

      <section className={styles.hero}>
        <div className={styles.heroContent}>
          <p className={styles.eyebrow}>Home del Tutor</p>
          <h1 className={styles.title}>Centraliza el cuidado de tu mascota desde aqui</h1>
          <p className={styles.description}>
            Empieza creando el perfil de tu mascota para tener lista su
            informacion, sus necesidades y su presentacion dentro de Patiperro.
          </p>

          <div className={styles.heroRibbon}>
            <span className={styles.heroRibbonTag}>Primer objetivo</span>
            <p>
              Registra a tu mascota para comenzar a construir su ficha y dejar
              preparado el flujo del tutor.
            </p>
          </div>
        </div>

        <div className={styles.heroBadge}>
          <span className={styles.heroBadgeLabel}>Estado actual</span>
          <strong>Sin mascotas registradas</strong>
          <p className={styles.heroBadgeText}>
            Tu home del tutor esta listo. El siguiente paso recomendado es crear
            la ficha de tu primera mascota.
          </p>
          <Link to="/tutor/mascota/nueva" className={styles.primaryButton}>
            Anadir mascota
          </Link>
        </div>
      </section>

      <section className={styles.metricsSection}>
        <div className={styles.sectionHeading}>
          <p className={styles.cardEyebrow}>Resumen inicial</p>
          <h2>Tu espacio del tutor empieza aqui</h2>
        </div>

        <div className={styles.metricsGrid}>
          {EMPTY_CARDS.map((card) => (
            <article key={card.label} className={styles.metricCard}>
              <span className={styles.metricLabel}>{card.label}</span>
              <strong className={styles.metricValue}>{card.value}</strong>
              <p className={styles.metricHelper}>{card.helper}</p>
            </article>
          ))}
        </div>
      </section>

      <section className={styles.contentGrid}>
        <article className={`${styles.card} ${styles.primaryCard}`}>
          <div className={styles.cardHeader}>
            <div>
              <p className={styles.cardEyebrow}>Siguiente accion</p>
              <h2>Anade la ficha de tu mascota</h2>
            </div>
            <span className={styles.statusChip}>Pendiente</span>
          </div>

          <p className={styles.cardText}>
            Crear la ficha de tu mascota te permitira registrar datos basicos,
            comportamiento, cuidados especiales y una foto principal para que
            su perfil quede preparado.
          </p>

          <div className={styles.stepList}>
            {NEXT_STEPS.map((step, index) => (
              <div key={step} className={styles.stepItem}>
                <span className={styles.stepIndex}>0{index + 1}</span>
                <span>{step}</span>
              </div>
            ))}
          </div>

          <div className={styles.actionRow}>
            <Link to="/tutor/mascota/nueva" className={styles.primaryButton}>
              Ir a crear mascota
            </Link>
          </div>
        </article>

        <article className={`${styles.card} ${styles.secondaryCard}`}>
          <div className={styles.cardHeader}>
            <div>
              <p className={styles.cardEyebrow}>Preparacion</p>
              <h2>Que datos te pediremos</h2>
            </div>
          </div>

          <p className={styles.cardText}>
            El formulario de mascota estara organizado por pasos para que el
            proceso sea mas claro y no tengas que ingresar todo de golpe.
          </p>

          <div className={styles.infoStack}>
            <div className={styles.infoPill}>Datos basicos</div>
            <div className={styles.infoPill}>Perfil y cuidados</div>
            <div className={styles.infoPill}>Foto principal</div>
          </div>
        </article>
      </section>
    </main>
  );
}
