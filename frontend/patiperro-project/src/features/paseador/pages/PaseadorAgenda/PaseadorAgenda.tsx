// Vista inicial de disponibilidad del paseador.
// Carga bloques reales del microservicio agenda (grid semanal sigue siendo maqueta visual).
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import PaseadorNavbar from "../../components/PaseadorNavbar/PaseadorNavbar";
import {
  fetchBloquesPorUsuario,
  readStoredPaseadorId,
  type AgendaBloqueDTO
} from "../../services/agendaService";
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
  const [bloques, setBloques] = useState<AgendaBloqueDTO[] | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelado = false;
    const id = readStoredPaseadorId();
    if (id == null) {
      setCargando(false);
      setError(
        "No hay id de paseador en sesión. Cierra sesión y vuelve a iniciar sesión para sincronizar la agenda."
      );
      setBloques([]);
      return;
    }
    (async () => {
      try {
        setCargando(true);
        setError(null);
        const lista = await fetchBloquesPorUsuario(id);
        if (!cancelado) {
          setBloques(lista);
        }
      } catch (e) {
        if (!cancelado) {
          setError(e instanceof Error ? e.message : "No se pudo cargar la agenda.");
          setBloques([]);
        }
      } finally {
        if (!cancelado) {
          setCargando(false);
        }
      }
    })();
    return () => {
      cancelado = true;
    };
  }, []);

  const textoEstadoAgenda = () => {
    if (cargando) return "Cargando…";
    if (error) return error;
    if (!bloques || bloques.length === 0) return "Sin bloques configurados";
    return `${bloques.length} bloque${bloques.length === 1 ? "" : "s"} en el sistema`;
  };

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
          <strong>{textoEstadoAgenda()}</strong>
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
            Esta vista deja preparada la estructura de tu disponibilidad. Los
            bloques guardados en el backend se reflejan en el resumen superior; el
            calendario de ejemplo no esta aun enlazado franja a franja con la API.
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
