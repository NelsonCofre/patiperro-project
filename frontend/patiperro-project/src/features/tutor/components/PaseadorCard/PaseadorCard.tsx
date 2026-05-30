import { useEffect, useState } from "react";
import type { PaseadorHome } from "../../types/paseadorHome.types";
import { resenaApi } from "../../services/resenaApi";
import { formatDistanceFromKm } from "../../utils/distanceFormat";
import VerifiedBadge from "../VerifiedBadge/VerifiedBadge";
import styles from "./PaseadorCard.module.css";

type PaseadorCardProps = {
  paseador: PaseadorHome;
  onVerPerfil?: (paseador: PaseadorHome) => void;
};

export default function PaseadorCard({ paseador, onVerPerfil }: PaseadorCardProps) {
  const [promedioReal, setPromedioReal] = useState<number>(paseador.calificacionPromedio);

  useEffect(() => {
    async function cargarPromedio() {
      try {
        const id = Number(paseador.id);
        if (!isNaN(id)) {
          const rating = await resenaApi.obtenerPromedioPaseador(id);
          setPromedioReal(rating || 0);
        }
      } catch (error) {
        console.error("Error al obtener rating para card:", error);
      }
    }
    cargarPromedio();
  }, [paseador.id]);

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
          <div className={styles.nameRow}>
            <h3>{paseador.nombre}</h3>
            <div className={styles.ratingBadge}>
              <span className={styles.starIcon}>★</span>
              <span>{promedioReal > 0 ? promedioReal.toFixed(1) : "Nuevo"}</span>
            </div>
          </div>
          <p className={styles.distance}>{formatDistanceFromKm(paseador.distanciaKm)} de ti</p>
          <VerifiedBadge verified={paseador.verificado} variant="list" />
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
