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

  // Bloquear el scroll del body cuando el modal está abierto
  useEffect(() => {
    document.body.style.overflow = 'hidden';
    
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        onClose();
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = 'unset';
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [onClose]);

  const orderedReviews = useMemo(
    () =>
      [...paseador.resenas]
        .sort((a, b) => new Date(b.fecha).getTime() - new Date(a.fecha).getTime())
        .slice(0, 5),
    [paseador.resenas]
  );

  function handleReservar() {
    // IMPORTANTE: Verifica que la ruta coincida con la de tu App.tsx (/tutor/solicitud)
    navigate(`/tutor/solicitud?paseadorId=${encodeURIComponent(paseador.id)}`);
  }

  return (
    <div className={styles.overlay} onMouseDown={onClose}>
      <section
        className={styles.modal}
        role="dialog"
        aria-modal="true"
        aria-labelledby="perfil-paseador-title"
        onMouseDown={(event) => event.stopPropagation()} // Evita cerrar al hacer click dentro
      >
        <button type="button" className={styles.closeButton} onClick={onClose} aria-label="Cerrar perfil">
          &times;
        </button>

        <div className={styles.header}>
          <img 
            src={paseador.fotoUrl || "https://via.placeholder.com/150"} 
            alt={`Foto de ${paseador.nombreCompleto}`} 
            className={styles.photo} 
          />
          <div className={styles.headerInfo}>
            <span className={paseador.verificado ? styles.verifiedBadge : styles.unverifiedBadge}>
              {paseador.verificado ? "✔ Verificado" : "No Verificado"}
            </span>
            <h2 id="perfil-paseador-title">{paseador.nombreCompleto}</h2>
            <p className={styles.bio}>{paseador.bio}</p>
          </div>
        </div>

        <div className={styles.statsGrid}>
          <article className={styles.statItem}>
            <span>Calificación</span>
            <strong>⭐ {paseador.calificacionPromedio.toFixed(1)}</strong>
          </article>
          <article className={styles.statItem}>
            <span>Paseos</span>
            <strong>{paseador.totalPaseosRealizados}</strong>
          </article>
          <article className={styles.statItem}>
            <span>Cobertura</span>
            <strong>{paseador.radioCoberturaKm} km</strong>
          </article>
        </div>

        <div className={styles.scrollableContent}>
          <section className={styles.section}>
            <h3>Tarifas por tamaño</h3>
            <div className={styles.rateGrid}>
              {paseador.tarifasPorTamano.map((tarifa) => (
                <article key={tarifa.tamano} className={styles.rateItem}>
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
                    <div className={styles.reviewHeader}>
                      <strong>{resena.tutorNombre}</strong>
                      <span className={styles.reviewDate}>{dateFormatter.format(new Date(resena.fecha))}</span>
                    </div>
                    <p>{resena.comentario}</p>
                    <span className={styles.reviewScore}>★ {resena.calificacion.toFixed(1)}</span>
                  </article>
                ))}
              </div>
            ) : (
              <p className={styles.emptyReviews}>Este paseador aún no tiene reseñas</p>
            )}
          </section>
        </div>

        <div className={styles.actions}>
          <button type="button" className={styles.reserveButton} onClick={handleReservar}>
            Reservar Paseo
          </button>
        </div>
      </section>
    </div>
  );
}