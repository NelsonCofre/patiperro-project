import { useEffect, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import DisponibilidadSemanal from "../DisponibilidadSemanal/DisponibilidadSemanal";
import type { PaseadorPerfil } from "../../types/paseadorHome.types";
import styles from "./PerfilPaseadorModal.module.css";

type PerfilPaseadorModalProps = {
  paseador: PaseadorPerfil;
  onClose: () => void;
};

const currencyFormatter = new Intl.NumberFormat("es-CL", {
  style: "currency",
  currency: "CLP",
  maximumFractionDigits: 0
});

const dateFormatter = new Intl.DateTimeFormat("es-CL", {
  day: "2-digit",
  month: "short",
  year: "numeric"
});

export default function PerfilPaseadorModal({ paseador, onClose }: PerfilPaseadorModalProps) {
  const navigate = useNavigate();

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        onClose();
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  const orderedReviews = useMemo(
    () =>
      [...paseador.resenas]
        .sort((a, b) => new Date(b.fecha).getTime() - new Date(a.fecha).getTime())
        .slice(0, 5),
    [paseador.resenas]
  );

  function handleReservar() {
    navigate(`/tutor/solicitud-paseo?paseadorId=${encodeURIComponent(paseador.id)}`);
  }

  return (
    <div className={styles.overlay} onMouseDown={onClose}>
      <section
        className={styles.modal}
        role="dialog"
        aria-modal="true"
        aria-labelledby="perfil-paseador-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <button type="button" className={styles.closeButton} onClick={onClose} aria-label="Cerrar perfil">
          x
        </button>

        <div className={styles.header}>
          <img src={paseador.fotoUrl} alt={`Foto de ${paseador.nombreCompleto}`} className={styles.photo} />
          <div className={styles.headerInfo}>
            <span className={paseador.verificado ? styles.verifiedBadge : styles.unverifiedBadge}>
              {paseador.verificado ? "Verificado" : "No Verificado"}
            </span>
            <h2 id="perfil-paseador-title">{paseador.nombreCompleto}</h2>
            <p>{paseador.bio}</p>
          </div>
        </div>

        <div className={styles.statsGrid} aria-label="Resumen del paseador">
          <article>
            <span>Calificacion</span>
            <strong>{paseador.calificacionPromedio.toFixed(1)}</strong>
          </article>
          <article>
            <span>Paseos realizados</span>
            <strong>{paseador.totalPaseosRealizados}</strong>
          </article>
          <article>
            <span>Radio de cobertura</span>
            <strong>{paseador.radioCoberturaKm} km</strong>
          </article>
        </div>

        <section className={styles.section}>
          <h3>Tarifas por tamaño de perro</h3>
          <div className={styles.rateGrid}>
            {paseador.tarifasPorTamano.map((tarifa) => (
              <article key={tarifa.tamano}>
                <span>{tarifa.tamano}</span>
                <strong>{currencyFormatter.format(tarifa.precio)}</strong>
              </article>
            ))}
          </div>
        </section>

        <section className={styles.section}>
          <h3>Horarios disponibles</h3>
          <DisponibilidadSemanal dias={paseador.disponibilidadProximos7Dias} />
        </section>

        <section className={styles.section}>
          <h3>Últimas reseñas</h3>
          {orderedReviews.length > 0 ? (
            <div className={styles.reviewList}>
              {orderedReviews.map((resena) => (
                <article key={resena.id} className={styles.review}>
                  <div>
                    <strong>{resena.tutorNombre}</strong>
                    <span>{dateFormatter.format(new Date(resena.fecha))}</span>
                  </div>
                  <p>{resena.comentario}</p>
                  <span className={styles.reviewScore}>{resena.calificacion.toFixed(1)}</span>
                </article>
              ))}
            </div>
          ) : (
            <p className={styles.emptyReviews}>Este paseador aún no tiene reseñas</p>
          )}
        </section>

        <div className={styles.actions}>
          <button type="button" className={styles.reserveButton} onClick={handleReservar}>
            Reservar
          </button>
        </div>
      </section>
    </div>
  );
}
