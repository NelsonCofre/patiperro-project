import { Link } from "react-router-dom";
import PaseadorCard from "../../components/PaseadorCard/PaseadorCard";
import TutorNavbar from "../../components/TutorNavbar/TutorNavbar";
import { usePaseadoresHomeMock } from "../../hooks/usePaseadoresHomeMock";
import styles from "./TutorDashboard.module.css";

const SKELETON_CARDS = ["skeleton-1", "skeleton-2", "skeleton-3"];

export default function TutorDashboard() {
  const {
    isLoading,
    searchRadiusKm,
    lastUpdatedLabel,
    locationStatus,
    coordinates,
    locationError,
    paseadores,
    visiblePaseadores,
    hasMore,
    loadMore,
    expandSearch,
    requestTutorLocation
  } = usePaseadoresHomeMock();

  const hasResults = visiblePaseadores.length > 0;
  const locationMessage =
    locationStatus === "requesting"
      ? "Solicitando ubicacion..."
      : locationStatus === "granted" && coordinates
        ? `Ubicacion detectada: ${coordinates.latitude.toFixed(4)}, ${coordinates.longitude.toFixed(4)}`
        : locationStatus === "idle"
          ? "Puedes usar tu ubicacion actual o ingresar una direccion manual."
          : locationError;

  return (
    <main className={styles.page}>
      <TutorNavbar />

      <section className={styles.hero}>
        <div className={styles.heroContent}>
          <p className={styles.eyebrow}>Home del Tutor</p>
          <h1 className={styles.title}>Encuentra paseadores disponibles cerca de tu perro</h1>
          <p className={styles.description}>
            Revisa paseadores cercanos, compara distancia, calificacion y precio base, y
            elige con mas confianza cuando quieras preparar una solicitud de paseo.
          </p>

          <div className={styles.heroActions}>
            <a href="#paseadores-cercanos" className={styles.primaryButton}>
              Ver paseadores cercanos
            </a>
            <Link to="/tutor/mascota/nueva" className={styles.secondaryButton}>
              Anadir mascota
            </Link>
          </div>
        </div>

        <aside className={styles.locationPanel}>
          <span className={styles.locationLabel}>Ubicacion del tutor</span>
          <strong>{locationStatus === "granted" ? "Buscando cerca de ti" : "Define tu ubicacion"}</strong>
          <p>{locationMessage}</p>
          <div className={styles.locationActions}>
            <button
              type="button"
              onClick={requestTutorLocation}
              disabled={locationStatus === "requesting"}
            >
              {locationStatus === "requesting" ? "Solicitando..." : "Usar mi ubicacion"}
            </button>
            <input type="text" placeholder="Ingresa tu direccion manualmente" />
          </div>
        </aside>
      </section>

      <section className={styles.summaryGrid}>
        <article className={styles.summaryCard}>
          <span>Radio actual</span>
          <strong>{searchRadiusKm} km</strong>
          <p>Radio visual por defecto para encontrar paseadores disponibles.</p>
        </article>
        <article className={styles.summaryCard}>
          <span>Resultados mock</span>
          <strong>{paseadores.length}</strong>
          <p>Datos temporales de frontend, listos para reemplazar por backend.</p>
        </article>
        <article className={styles.summaryCard}>
          <span>Orden recomendado</span>
          <strong>Distancia</strong>
          <p>La lista se presenta de menor a mayor distancia del tutor.</p>
        </article>
      </section>

      <section id="paseadores-cercanos" className={styles.walkersSection}>
        <div className={styles.sectionHeading}>
          <div>
            <p className={styles.cardEyebrow}>Paseadores cercanos</p>
            <h2>Disponibles para futuros paseos</h2>
          </div>
          <span className={styles.refreshHint}>
            Actualizacion automatica cada 30 segundos · {lastUpdatedLabel}
          </span>
        </div>

        {isLoading ? (
          <div className={styles.walkersList} aria-label="Cargando paseadores cercanos">
            {SKELETON_CARDS.map((item) => (
              <article key={item} className={styles.skeletonCard}>
                <div className={styles.skeletonPhoto} />
                <div className={styles.skeletonContent}>
                  <span />
                  <span />
                  <span />
                  <span />
                </div>
              </article>
            ))}
          </div>
        ) : hasResults ? (
          <>
            <div className={styles.walkersList}>
              {visiblePaseadores.map((paseador) => (
                <PaseadorCard key={paseador.id} paseador={paseador} />
              ))}
            </div>

            {hasMore ? (
              <div className={styles.loadMoreRow}>
                <button type="button" className={styles.secondaryButton} onClick={loadMore}>
                  Cargar mas paseadores
                </button>
              </div>
            ) : null}
          </>
        ) : (
          <article className={styles.emptyState}>
            <div className={styles.emptyIllustration}>P</div>
            <div>
              <p className={styles.cardEyebrow}>Sin resultados</p>
              <h3>No hay paseadores disponibles dentro del radio actual</h3>
              <p>
                Puedes ampliar la busqueda para revisar opciones un poco mas lejanas a tu
                ubicacion.
              </p>
              <button type="button" className={styles.primaryButton} onClick={expandSearch}>
                Ampliar busqueda
              </button>
            </div>
          </article>
        )}
      </section>
    </main>
  );
}
