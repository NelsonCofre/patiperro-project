import type { PaseadorHome } from "../../types/paseadorHome.types";
import styles from "./PaseadorCard.module.css";

type PaseadorCardProps = {
  paseador: PaseadorHome;
};

function formatPrice(value: number) {
  return new Intl.NumberFormat("es-CL", {
    style: "currency",
    currency: "CLP",
    maximumFractionDigits: 0
  }).format(value);
}

function renderStars(rating: number) {
  const rounded = Math.round(rating);

  return Array.from({ length: 5 }, (_, index) => (
    <span key={index} className={index < rounded ? styles.starActive : styles.starMuted}>
      *
    </span>
  ));
}

export default function PaseadorCard({ paseador }: PaseadorCardProps) {
  return (
    <article className={styles.card}>
      <div className={styles.photoWrap}>
        <img src={paseador.fotoUrl} alt={`Foto de ${paseador.nombre}`} className={styles.photo} />
        <span className={styles.availabilityDot}>Disponible</span>
      </div>

      <div className={styles.content}>
        <div className={styles.header}>
          <div>
            <h3>{paseador.nombre}</h3>
            <p>{paseador.distanciaKm.toFixed(1)} km de tu ubicacion</p>
          </div>
          <strong>{formatPrice(paseador.precioBase)}</strong>
        </div>

        <div className={styles.ratingRow}>
          <span className={styles.stars} aria-label={`Calificacion ${paseador.calificacionPromedio}`}>
            {renderStars(paseador.calificacionPromedio)}
          </span>
          <span>{paseador.calificacionPromedio.toFixed(1)}</span>
        </div>

        <p className={styles.bio}>{paseador.bio}</p>

        <div className={styles.footer}>
          <span>Proximo bloque: {paseador.proximoBloque}</span>
          <button type="button">Ver perfil</button>
        </div>
      </div>
    </article>
  );
}

