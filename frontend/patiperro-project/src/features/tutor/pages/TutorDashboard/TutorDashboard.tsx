import { Link } from "react-router-dom";
import { useState } from "react";
import PerfilPaseadorModal from "../../components/PerfilPaseadorModal/PerfilPaseadorModal";
import PaseadorCard from "../../components/PaseadorCard/PaseadorCard";
import PaseadoresFilterBar from "../../components/PaseadoresFilterBar/PaseadoresFilterBar";
import TutorNavbar from "../../components/TutorNavbar/TutorNavbar";
import { usePaseadoresHome } from "../../hooks/usePaseadoresHome";
import type { PaseadorHome } from "../../types/paseadorHome.types";
import styles from "./TutorDashboard.module.css";
import PaseadoresMap from '../../components/PaseadoresMap/PaseadoresMap';

const SKELETON_CARDS = ["skeleton-1", "skeleton-2", "skeleton-3"];

export default function TutorDashboard() {
  const [selectedPaseador, setSelectedPaseador] = useState<PaseadorHome | null>(null);
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
    requestTutorLocation,
    listError,
    needsReferencePoint,
    filteredCount,
    queryText,
    setQueryText,
    maxDistanceFilterKm,
    setMaxDistanceFilterKm,
    sortMode,
    setSortMode,
    resetFilters,
    hasActiveFilters,
    profileLoadDone,
    applySearchRadiusKm,
    minSearchRadiusKm,
    maxSearchRadiusKm,
    refLat,    // <-- Agrega esta
    refLon
  } = usePaseadoresHome();

  const hasResults = visiblePaseadores.length > 0;
  const noFilterMatches =
    !isLoading && !needsReferencePoint && paseadores.length > 0 && filteredCount === 0;
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
          Revisa paseadores cercanos por distancia y biografia, y elige con mas confianza cuando
          quieras preparar una solicitud de paseo.
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
          <input
            type="text"
            className={styles.locationInputMuted}
            placeholder="Direccion manual (proximamente)"
            readOnly
            aria-readonly="true"
            title="Por ahora usa tu ubicacion o la direccion guardada en tu perfil"
          />
        </div>
      </aside>
    </section>

    <section className={styles.summaryGrid}>
      <article className={styles.summaryCard}>
        <span>Radio de busqueda</span>
        <strong>{needsReferencePoint ? "—" : `${searchRadiusKm} km`}</strong>
        <p>
          Lo defines tu en la seccion de filtros (valor que se envia al servidor como cobertura
          maxima).
        </p>
      </article>
      <article className={styles.summaryCard}>
        <span>Coincidencias</span>
        <strong>{needsReferencePoint ? "—" : filteredCount}</strong>
        <p>
          {needsReferencePoint || paseadores.length === 0
            ? "Activa ubicacion o espera resultados."
            : `${filteredCount} visibles con filtros · ${paseadores.length} en radio ${searchRadiusKm} km.`}
        </p>
      </article>
      <article className={styles.summaryCard}>
        <span>Filtros</span>
        <strong>{hasActiveFilters ? "Activos" : "Listos"}</strong>
        <p>
          Busca por texto, acorta la distancia maxima en pantalla y cambia el criterio de orden.
        </p>
      </article>
    </section>

    <section id="paseadores-cercanos" className={styles.walkersSection}>
      <div className={styles.sectionHeading}>
        <div>
          <p className={styles.cardEyebrow}>Paseadores cercanos</p>
          <h2>Disponibles para futuros paseos</h2>
        </div>
        <span className={styles.refreshHint}>
          Ultima actualizacion · {lastUpdatedLabel}
          {listError ? ` · ${listError}` : ""}
        </span>
      </div>

      {profileLoadDone && !needsReferencePoint ? (
        <>
          <PaseadoresFilterBar
            searchRadiusKm={searchRadiusKm}
            minSearchRadiusKm={minSearchRadiusKm}
            maxSearchRadiusKm={maxSearchRadiusKm}
            onApplySearchRadiusKm={applySearchRadiusKm}
            queryText={queryText}
            onQueryTextChange={setQueryText}
            sortMode={sortMode}
            onSortModeChange={setSortMode}
            maxDistanceFilterKm={maxDistanceFilterKm}
            onMaxDistanceFilterKmChange={setMaxDistanceFilterKm}
            totalFromApi={paseadores.length}
            filteredCount={filteredCount}
            hasActiveFilters={hasActiveFilters}
            onResetFilters={resetFilters}
          />

          {/* INTEGRACIÓN DEL MAPA: Se muestra si existen coordenadas de referencia */}
          {refLat && refLon && (
            <div style={{ marginTop: '20px' }}>
              <PaseadoresMap 
                centroLat={refLat} 
                centroLng={refLon} 
                paseadores={visiblePaseadores} 
              />
            </div>
          )}
        </>
      ) : null}

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
      ) : needsReferencePoint ? (
        <article className={styles.emptyState}>
          <div className={styles.emptyIllustration}>?</div>
          <div>
            <p className={styles.cardEyebrow}>Sin punto de busqueda</p>
            <h3>Activa tu ubicacion o completa las coordenadas de tu direccion</h3>
            <p>
              Para listar paseadores cercanos necesitamos latitud y longitud. Puedes usar el
              boton «Usar mi ubicacion» o, si ya guardaste direccion al registrarte, que el
              sistema haya geocodificado tu domicilio.
            </p>
            <button type="button" className={styles.primaryButton} onClick={requestTutorLocation}>
              Usar mi ubicacion
            </button>
          </div>
        </article>
      ) : noFilterMatches ? (
        <article className={styles.emptyState}>
          <div className={styles.emptyIllustration}>F</div>
          <div>
            <p className={styles.cardEyebrow}>Sin coincidencias</p>
            <h3>Ningun paseador cumple los filtros actuales</h3>
            <p>
              Prueba otra palabra de busqueda, sube el limite de distancia o revisa el orden de la
              lista.
            </p>
            <div className={styles.emptyActions}>
              <button type="button" className={styles.primaryButton} onClick={resetFilters}>
                Limpiar filtros
              </button>
            </div>
          </div>
        </article>
      ) : hasResults ? (
        <>
          <div className={styles.walkersList}>
            {visiblePaseadores.map((paseador) => (
              <PaseadorCard
                key={paseador.id}
                paseador={paseador}
                onVerPerfil={setSelectedPaseador}
              />
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
              Sube el radio de cobertura en los filtros de arriba o usa el atajo para ampliar 5
              km.
            </p>
            <div className={styles.emptyActions}>
              <button type="button" className={styles.secondaryButton} onClick={expandSearch}>
                Ampliar 5 km
              </button>
            </div>
          </div>
        </article>
      )}
    </section>
    {selectedPaseador ? (
      <PerfilPaseadorModal
        paseador={selectedPaseador}
        onClose={() => setSelectedPaseador(null)}
      />
    ) : null}
  </main>
);
}
