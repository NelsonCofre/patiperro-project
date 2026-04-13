import { Link, useSearchParams } from "react-router-dom";
import TutorNavbar from "../../components/TutorNavbar/TutorNavbar";
import styles from "./SolicitudPaseo.module.css";

export default function SolicitudPaseo() {
  const [searchParams] = useSearchParams();
  const paseadorId = searchParams.get("paseadorId") ?? "";

  return (
    <main className={styles.page}>
      <TutorNavbar />

      <section className={styles.shell}>
        <div className={styles.heading}>
          <p>Solicitud de paseo</p>
          <h1>Reserva con tu paseador seleccionado</h1>
          <span>
            {paseadorId
              ? `Paseador preseleccionado: ${paseadorId}`
              : "Selecciona un paseador desde el home del tutor."}
          </span>
        </div>

        <form className={styles.form}>
          <label>
            Paseador
            <input value={paseadorId} readOnly aria-readonly="true" />
          </label>

          <label>
            Fecha del paseo
            <input type="date" disabled />
          </label>

          <label>
            Bloque horario
            <select disabled defaultValue="">
              <option value="">Se completara al conectar disponibilidad</option>
            </select>
          </label>

          <label>
            Comentarios para el paseador
            <textarea disabled placeholder="Proximamente conectado al backend" />
          </label>

          <div className={styles.actions}>
            <Link to="/tutor/dashboard">Volver al home</Link>
            <button type="button" disabled>
              Enviar solicitud
            </button>
          </div>
        </form>
      </section>
    </main>
  );
}
