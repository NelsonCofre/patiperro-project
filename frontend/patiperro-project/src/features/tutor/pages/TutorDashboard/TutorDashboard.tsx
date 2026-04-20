import { Link } from "react-router-dom";
import { useMemo, useState } from "react";
import PerfilPaseadorModal from "../../components/PerfilPaseadorModal/PerfilPaseadorModal";
import PaseadoresFilterBar from "../../components/PaseadoresFilterBar/PaseadoresFilterBar";
import TutorNavbar from "../../components/TutorNavbar/TutorNavbar";
import { usePaseadoresHome } from "../../hooks/usePaseadoresHome";
import type { PaseadorHome } from "../../types/paseadorHome.types";
import styles from "./TutorDashboard.module.css";
import PaseadoresMap from '../../components/PaseadoresMap/PaseadoresMap';
import { buildPaseadorPerfilMock } from "../../utils/paseadorPerfilMock";

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
    requestTutorLocation,
    needsReferencePoint,
    filteredCount,
    queryText,
    setQueryText,
    availabilityDate,
    setAvailabilityDate,
    availabilityStartTime,
    setAvailabilityStartTime,
    availabilityEndTime,
    setAvailabilityEndTime,
    availabilityFilterError,
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
    refLat,
    refLon
  } = usePaseadoresHome();

  const hasResults = visiblePaseadores.length > 0;

  const selectedPaseadorPerfil = useMemo(
  () => (selectedPaseador ? buildPaseadorPerfilMock(selectedPaseador) : null),
  [selectedPaseador]
);
  const noFilterMatches =
    !isLoading && !needsReferencePoint && hasActiveFilters && filteredCount === 0;
  const emptyResultsTitle = noFilterMatches
    ? "No encontramos paseadores disponibles"
    : "No hay paseadores en esta zona";
  const emptyResultsMessage = noFilterMatches
    ? "No encontramos paseadores disponibles en este horario cerca de tu ubicacion. Intenta ampliar el rango de busqueda."
    : "Prueba ampliando el radio o ajustando los filtros de busqueda.";
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

      {/* --- SECCIÓN HERO --- */}
      <section className={styles.hero}>
        <div className={styles.heroContent}>
          <p className={styles.eyebrow}>Home del Tutor</p>
          <h1 className={styles.title}>Encuentra paseadores disponibles cerca de tu perro</h1>
          <p className={styles.description}>
            Revisa paseadores cercanos por distancia y biografia, y elige con mas confianza.
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
          </div>
        </aside>
      </section>

      {/* --- RESUMEN RÁPIDO --- */}
      <section className={styles.summaryGrid}>
        <article className={styles.summaryCard}>
          <span>Radio de busqueda</span>
          <strong>{needsReferencePoint ? "—" : `${searchRadiusKm} km`}</strong>
        </article>
        <article className={styles.summaryCard}>
          <span>Coincidencias</span>
          <strong>{needsReferencePoint ? "—" : filteredCount}</strong>
        </article>
        <article className={styles.summaryCard}>
          <span>Filtros</span>
          <strong>{hasActiveFilters ? "Activos" : "Listos"}</strong>
        </article>
      </section>

      {/* --- SECCIÓN DE MAPA Y RESULTADOS --- */}
      <section id="paseadores-cercanos" className={styles.walkersSection}>
        <div className={styles.sectionHeading}>
          <div>
            <p className={styles.cardEyebrow}>Explora el mapa</p>
            <h2>Paseadores en tu area</h2>
          </div>
          <span className={styles.refreshHint}>
            Ultima actualizacion · {lastUpdatedLabel}
          </span>
        </div>

        {profileLoadDone && !needsReferencePoint && (
          <PaseadoresFilterBar
            searchRadiusKm={searchRadiusKm}
            minSearchRadiusKm={minSearchRadiusKm}
            maxSearchRadiusKm={maxSearchRadiusKm}
            onApplySearchRadiusKm={applySearchRadiusKm}
            queryText={queryText}
            onQueryTextChange={setQueryText}
            availabilityDate={availabilityDate}
            onAvailabilityDateChange={setAvailabilityDate}
            availabilityStartTime={availabilityStartTime}
            onAvailabilityStartTimeChange={setAvailabilityStartTime}
            availabilityEndTime={availabilityEndTime}
            onAvailabilityEndTimeChange={setAvailabilityEndTime}
            availabilityFilterError={availabilityFilterError}
            sortMode={sortMode}
            onSortModeChange={setSortMode}
            maxDistanceFilterKm={maxDistanceFilterKm}
            onMaxDistanceFilterKmChange={setMaxDistanceFilterKm}
            totalFromApi={paseadores.length}
            filteredCount={filteredCount}
            hasActiveFilters={hasActiveFilters}
            onResetFilters={resetFilters}
          />
        )}

        {/* EL MAPA Y SU PANEL LATERAL SIEMPRE AQUÍ */}
        {refLat && refLon && !needsReferencePoint && (
          <div style={{ marginTop: '20px' }}>
            <PaseadoresMap 
              centroLat={refLat} 
              centroLng={refLon} 
              paseadores={visiblePaseadores} 
              onVerPerfil={setSelectedPaseador}
              emptyTitle={emptyResultsTitle}
              emptyMessage={emptyResultsMessage}
            />
          </div>
        )}

        {/* SOLAMENTE RENDERIZAMOS CARGA O PUNTO DE REFERENCIA FALTANTE */}
        {isLoading ? (
          <div className={styles.walkersList} style={{ marginTop: '20px' }}>
            {SKELETON_CARDS.map((item) => (
              <article key={item} className={styles.skeletonCard}>
                <div className={styles.skeletonPhoto} />
                <div className={styles.skeletonContent}>
                  <span /> <span /> <span /> <span />
                </div>
              </article>
            ))}
          </div>
        ) : needsReferencePoint ? (
          <article className={styles.emptyState}>
            <div className={styles.emptyIllustration}>?</div>
            <div>
              <h3>Activa tu ubicacion para ver el mapa</h3>
              <button type="button" className={styles.primaryButton} onClick={requestTutorLocation}>
                Usar mi ubicacion
              </button>
            </div>
          </article>
        ) : (
          /* SI NO ESTÁ CARGANDO Y HAY PUNTO DE REFERENCIA, 
             EL MAPA YA SE ESTÁ MOSTRANDO ARRIBA CON SUS PROPIOS MENSAJES.
             SOLO AGREGAMOS EL BOTÓN DE "CARGAR MÁS" SI HAY RESULTADOS.
          */
          hasResults && hasMore && (
            <div className={styles.loadMoreRow}>
              <button type="button" className={styles.secondaryButton} onClick={loadMore}>
                Cargar mas paseadores
              </button>
            </div>
          )
        )}
      </section>

      {/* MODAL DE PERFIL */}
      {selectedPaseadorPerfil && (
        <PerfilPaseadorModal
          paseador={selectedPaseadorPerfil}
          onClose={() => setSelectedPaseador(null)}
        />
      )}
    </main>
  );
}
