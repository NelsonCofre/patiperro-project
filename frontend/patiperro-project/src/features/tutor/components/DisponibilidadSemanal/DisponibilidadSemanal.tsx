import type { DiaDisponibilidad } from "../../types/paseadorHome.types";
import styles from "./DisponibilidadSemanal.module.css";

type DisponibilidadSemanalProps = {
  dias: DiaDisponibilidad[];
};

export default function DisponibilidadSemanal({ dias }: DisponibilidadSemanalProps) {
  return (
    <div className={styles.weekGrid} aria-label="Disponibilidad para los proximos 7 dias">
      {dias.map((dia) => (
        <article key={dia.fecha} className={styles.day}>
          <h4>{dia.etiqueta}</h4>
          <div className={styles.blocks}>
            {dia.bloques.length > 0 ? (
              dia.bloques.map((bloque) => (
                <span key={`${dia.fecha}-${bloque.inicio}-${bloque.fin}`}>
                  {bloque.inicio} - {bloque.fin}
                </span>
              ))
            ) : (
              <span className={styles.emptyBlock}>Sin horarios</span>
            )}
          </div>
        </article>
      ))}
    </div>
  );
}
