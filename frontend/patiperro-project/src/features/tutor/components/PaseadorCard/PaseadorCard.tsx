import type { PaseadorHome } from "../../types/paseadorHome.types";
import { formatDistanceFromKm } from "../../utils/distanceFormat";
import styles from "./PaseadorCard.module.css";

type PaseadorCardProps = {
  paseador: PaseadorHome;
  onVerPerfil?: (paseador: PaseadorHome) => void;
};

export default function PaseadorCard({ paseador, onVerPerfil }: PaseadorCardProps) {
  return (
    <article className={styles.card}>
      <div className={styles.photoWrap}>
        <img 
          src={paseador.fotoUrl || "https://via.placeholder.com/150"} 
          alt={`Foto de ${paseador.nombre}`} 
          className={styles.photo} 
        />
      </div>

      <div className={styles.content}>
        <div className={styles.header}>
          <div>
            <h3>{paseador.nombre}</h3>
            <p className={styles.distance}>{formatDistanceFromKm(paseador.distanciaKm)} de ti</p>
          </div>
        </div>

        <p className={styles.bio}>{paseador.bio}</p>

        <div className={styles.footer}>
          <button 
            type="button" 
            className={styles.primaryButton} 
            onClick={(e) => {
              e.stopPropagation();
              onVerPerfil?.(paseador);
            }}
          >
            Ver perfil
          </button>
        </div>
      </div>
    </article>
  );
}