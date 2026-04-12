import type { PaseadorHome } from "../../types/paseadorHome.types";
import styles from "./PaseadorCard.module.css";

type PaseadorCardProps = {
  paseador: PaseadorHome;
};

/**
 * Tarjeta simplificada: precio, calificacion y proximo bloque se ocultan hasta tener APIs
 * (configuracion publica, reseñas, agenda agregada).
 */
export default function PaseadorCard({ paseador }: PaseadorCardProps) {
  return (
    <article className={styles.card}>
      <div className={styles.photoWrap}>
        <img src={paseador.fotoUrl} alt={`Foto de ${paseador.nombre}`} className={styles.photo} />
      </div>

      <div className={styles.content}>
        <div className={styles.header}>
          <div>
            <h3>{paseador.nombre}</h3>
            <p>{paseador.distanciaKm.toFixed(1)} km de tu ubicacion</p>
          </div>
        </div>

        <p className={styles.bio}>{paseador.bio}</p>

        <div className={styles.footer}>
          <button type="button">Ver perfil</button>
        </div>
      </div>
    </article>
  );
}
