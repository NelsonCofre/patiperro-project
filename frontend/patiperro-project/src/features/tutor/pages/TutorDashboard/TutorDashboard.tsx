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
    refLon,
    minRating,
    setMinRating,
    maxPriceFilter,
    setMaxPriceFilter
  } = usePaseadoresHome();

  const hasResults = visiblePaseadores.length > 0;

  const selectedPaseadorPerfil = useMemo(
    () => (selectedPaseador ? buildPaseadorPerfilMock(selectedPaseador) : null),
    [selectedPaseador]
  );

  // --- LÓGICA DE MENSAJES CONTEXTUALES CORREGIDA ---
  const noFilterMatches = !isLoading && !needsReferencePoint && hasActiveFilters && filteredCount === 0;

  const getEmptyStateContent = () => {
  if (!noFilterMatches) {
    return {
      title: "No hay paseadores en esta zona",
      message: "Prueba ampliando el radio o ajustando los filtros de busqueda."
    };
  }

  // Si el usuario movió el slider de distancia (como en tu foto de 3km)
  if (maxDistanceFilterKm !== null) {
    return {
      title: "Fuera de rango",
      message: `No hay paseadores a menos de ${maxDistanceFilterKm} km. Intenta aumentar la distancia maxima en la lista.`
    };
  }

  // Solo si seleccionó estrellas
  if (minRating > 0) {
    return {
      title: "Sin coincidencias de calificación",
      message: "No se encontraron paseadores con esta calificación en tu zona. Intenta ajustar tus criterios de búsqueda."
    };
  }

  return {
    title: "No encontramos paseadores disponibles",
    message: "Intenta limpiar los filtros o ajustar tus criterios de busqueda."
  };
};

  const { title: emptyResultsTitle, message: emptyResultsMessage } = getEmptyStateContent();

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
            minRating={minRating}
            onMinRatingChange={setMinRating}
            maxPrice={maxPriceFilter}
            onMaxPriceChange={setMaxPriceFilter}
          />
        )}

        {/* EL MAPA Y SU PANEL LATERAL */}
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

        {/* ESTADOS DE CARGA Y REFERENCIA */}
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