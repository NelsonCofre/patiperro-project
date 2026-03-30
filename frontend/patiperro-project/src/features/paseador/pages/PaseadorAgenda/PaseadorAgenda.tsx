// Vista inicial de disponibilidad del paseador.
// Funciona como base visual antes de implementar la logica real de bloques horarios.
import { Link } from "react-router-dom";
import PaseadorNavbar from "../../components/PaseadorNavbar/PaseadorNavbar";
import styles from "./PaseadorAgenda.module.css";

const DAYS = [
  "Lunes",
  "Martes",
  "Miercoles",
  "Jueves",
  "Viernes",
  "Sabado",
  "Domingo"
];

const TIME_SLOTS = ["08:00 - 11:00", "12:00 - 15:00", "16:00 - 19:00"];

export default function PaseadorAgenda() {
  return (
    <main className={styles.page}>
      <PaseadorNavbar />

      <section className={styles.hero}>
        <div>
          <p className={styles.eyebrow}>Disponibilidad</p>
          <h1 className={styles.title}>Organiza tu agenda semanal</h1>
          <p className={styles.description}>
            Define los dias y bloques horarios en los que puedes trabajar para
            preparar tu servicio antes de recibir reservas.
          </p>
        </div>

        <div className={styles.infoCard}>
          <span className={styles.infoLabel}>Estado de agenda</span>
          <strong>Sin bloques configurados</strong>
        </div>
      </section>

      <section className={styles.board}>
        <article className={styles.mainCard}>
          <div className={styles.sectionHeader}>
            <div>
              <p className={styles.cardEyebrow}>Mi Agenda</p>
              <h2>Selecciona tus bloques disponibles</h2>
            </div>
            <span className={styles.emptyChip}>Inicial</span>
          </div>

          <p className={styles.supportText}>
            Esta vista deja preparada la estructura de tu disponibilidad. Mas
            adelante aqui podras marcar horarios reales y guardarlos en la
            plataforma.
          </p>

          <div className={styles.calendarGrid}>
            {DAYS.map((day) => (
              <article key={day} className={styles.dayCard}>
                <header className={styles.dayHeader}>
                  <h3>{day}</h3>
                  <span>Sin bloques</span>
                </header>

                <div className={styles.slotList}>
                  {TIME_SLOTS.map((slot) => (
                    <div key={`${day}-${slot}`} className={styles.slotPill}>
                      {slot}
                    </div>
                  ))}
                </div>
              </article>
            ))}
          </div>
        </article>

        <aside className={styles.sideCard}>
          <p className={styles.cardEyebrow}>Accion rapida</p>
          <h2>Tu disponibilidad aun no esta configurada</h2>
          <p className={styles.supportText}>
            Empieza agregando bloques semanales para mostrar cuando puedes
            aceptar paseos.
          </p>

          <button type="button" className={styles.primaryButton}>
            Agregar bloques horarios
          </button>

          <Link to="/paseador/dashboard" className={styles.secondaryLink}>
            Volver al dashboard
          </Link>
        </aside>
      </section>
    </main>
  );
}
