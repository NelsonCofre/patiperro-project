import { useEffect, useState } from "react";
import type { PaseadoresSortMode } from "../../hooks/usePaseadoresHome";
import styles from "./PaseadoresFilterBar.module.css";

type Props = {
  searchRadiusKm: number;
  minSearchRadiusKm: number;
  maxSearchRadiusKm: number;
  onApplySearchRadiusKm: (km: number) => void;
  queryText: string;
  onQueryTextChange: (value: string) => void;
  availabilityDate: string;
  onAvailabilityDateChange: (value: string) => void;
  availabilityStartTime: string;
  onAvailabilityStartTimeChange: (value: string) => void;
  availabilityEndTime: string;
  onAvailabilityEndTimeChange: (value: string) => void;
  availabilityFilterError: string;
  sortMode: PaseadoresSortMode;
  onSortModeChange: (value: PaseadoresSortMode) => void;
  maxDistanceFilterKm: number | null;
  onMaxDistanceFilterKmChange: (value: number | null) => void;
  totalFromApi: number;
  filteredCount: number;
  hasActiveFilters: boolean;
  onResetFilters: () => void;
};

const SORT_OPTIONS: { value: PaseadoresSortMode; label: string }[] = [
  { value: "distance", label: "Mas cercanos" },
  { value: "name", label: "Nombre (A-Z)" },
  { value: "coverage", label: "Mayor cobertura" }
];

export default function PaseadoresFilterBar({
  searchRadiusKm,
  minSearchRadiusKm,
  maxSearchRadiusKm,
  onApplySearchRadiusKm,
  queryText,
  onQueryTextChange,
  availabilityDate,
  onAvailabilityDateChange,
  availabilityStartTime,
  onAvailabilityStartTimeChange,
  availabilityEndTime,
  onAvailabilityEndTimeChange,
  availabilityFilterError,
  sortMode,
  onSortModeChange,
  maxDistanceFilterKm,
  onMaxDistanceFilterKmChange,
  totalFromApi,
  filteredCount,
  hasActiveFilters,
  onResetFilters
}: Props) {
  const [radiusDraft, setRadiusDraft] = useState(() => String(searchRadiusKm));

  useEffect(() => {
    setRadiusDraft(String(searchRadiusKm));
  }, [searchRadiusKm]);

  const applyRadiusFromDraft = () => {
    const raw = radiusDraft.trim().replace(",", ".");
    if (raw === "") {
      setRadiusDraft(String(searchRadiusKm));
      return;
    }
    const n = Number.parseFloat(raw);
    if (!Number.isFinite(n)) {
      setRadiusDraft(String(searchRadiusKm));
      return;
    }
    onApplySearchRadiusKm(Math.round(n));
  };

  const sliderMax = Math.max(1, Math.round(searchRadiusKm));
  const sliderValue =
    maxDistanceFilterKm != null
      ? Math.min(Math.round(maxDistanceFilterKm), sliderMax)
      : sliderMax;

  const onSliderInput = (raw: number) => {
    const v = Math.min(Math.max(1, raw), sliderMax);
    if (v >= sliderMax) {
      onMaxDistanceFilterKmChange(null);
    } else {
      onMaxDistanceFilterKmChange(v);
    }
  };

  return (
    <div className={styles.wrap} role="search" aria-label="Filtros de paseadores">
      <div className={styles.radiusBlock}>
        <div className={styles.radiusBlockText}>
          <p className={styles.kicker}>Busqueda en el mapa</p>
          <h3 className={styles.title}>Radio de cobertura (km)</h3>
          <p className={styles.radiusHint}>
            Distancia maxima desde tu punto para traer paseadores del servidor (entre{" "}
            {minSearchRadiusKm} y {maxSearchRadiusKm} km).
          </p>
        </div>
        <div className={styles.radiusControls}>
          <label className={styles.radiusLabel} htmlFor="tutor-search-radius">
            Kilometros
          </label>
          <div className={styles.radiusRow}>
            <input
              id="tutor-search-radius"
              type="number"
              className={styles.radiusInput}
              min={minSearchRadiusKm}
              max={maxSearchRadiusKm}
              step={1}
              value={radiusDraft}
              onChange={(e) => setRadiusDraft(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  applyRadiusFromDraft();
                }
              }}
            />
            <button
              type="button"
              className={styles.radiusApply}
              onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
                applyRadiusFromDraft();
              }}
            >
              Aplicar radio
            </button>
          </div>
          <div className={styles.radiusPresets} role="group" aria-label="Radios rapidos">
            {[5, 10, 15, 25, 50]
              .filter((km) => km >= minSearchRadiusKm && km <= maxSearchRadiusKm)
              .map((km) => (
              <button
                key={km}
                type="button"
                className={searchRadiusKm === km ? styles.chipActive : styles.chip}
                onClick={() => {
                  onApplySearchRadiusKm(km);
                  setRadiusDraft(String(km));
                }}
              >
                {km} km
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className={styles.head}>
        <div>
          <p className={styles.kicker}>Refina tu busqueda</p>
          <h3 className={styles.title}>Filtros y orden</h3>
        </div>
        <div className={styles.counts} aria-live="polite">
          <span className={styles.countBadge}>
            <strong>{filteredCount}</strong>
            <span>de {totalFromApi}</span>
          </span>
          {hasActiveFilters ? (
            <button type="button" className={styles.resetGhost} onClick={onResetFilters}>
              Limpiar filtros
            </button>
          ) : null}
        </div>
      </div>

      <div className={styles.searchRow}>
        <span className={styles.searchIcon} aria-hidden>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path
              d="M11 19a8 8 0 1 1 0-16 8 8 0 0 1 0 16Zm9 2-4.35-4.35"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </span>
        <input
          type="search"
          className={styles.searchInput}
          placeholder="Buscar por nombre o palabras en la biografia..."
          value={queryText}
          onChange={(e) => onQueryTextChange(e.target.value)}
          autoComplete="off"
          spellCheck={false}
        />
      </div>

      <div className={styles.controls}>
        <label className={styles.field}>
          <span className={styles.label}>Fecha</span>
          <input
            type="date"
            className={styles.input}
            value={availabilityDate}
            onChange={(e) => onAvailabilityDateChange(e.target.value)}
          />
        </label>

        <label className={styles.field}>
          <span className={styles.label}>Hora de Inicio</span>
          <input
            type="time"
            className={styles.input}
            value={availabilityStartTime}
            onChange={(e) => onAvailabilityStartTimeChange(e.target.value)}
          />
        </label>

        <label className={styles.field}>
          <span className={styles.label}>Hora de Termino</span>
          <input
            type="time"
            className={styles.input}
            value={availabilityEndTime}
            onChange={(e) => onAvailabilityEndTimeChange(e.target.value)}
          />
        </label>

        <label className={styles.field}>
          <span className={styles.label}>Ordenar por</span>
          <select
            className={styles.select}
            value={sortMode}
            onChange={(e) => onSortModeChange(e.target.value as PaseadoresSortMode)}
          >
            {SORT_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </label>

        <div className={styles.fieldGrow}>
          <div className={styles.sliderTop}>
            <span className={styles.label}>Distancia maxima en lista</span>
            <span className={styles.sliderValue}>
              {sliderValue >= sliderMax ? `Hasta ${sliderMax} km (radio actual)` : `Hasta ${sliderValue} km`}
            </span>
          </div>
          <input
            type="range"
            className={styles.range}
            min={1}
            max={sliderMax}
            step={1}
            value={sliderValue}
            onChange={(e) => onSliderInput(Number.parseInt(e.target.value, 10))}
          />
          <div className={styles.chips} role="group" aria-label="Atajos de distancia">
            {[3, 5, 10]
              .filter((km) => km < sliderMax)
              .map((km) => (
                <button
                  key={km}
                  type="button"
                  className={maxDistanceFilterKm === km ? styles.chipActive : styles.chip}
                  onClick={() => onMaxDistanceFilterKmChange(km)}
                >
                  Hasta {km} km
                </button>
              ))}
            <button
              type="button"
              className={maxDistanceFilterKm == null ? styles.chipActive : styles.chip}
              onClick={() => onMaxDistanceFilterKmChange(null)}
            >
              Todo el radio
            </button>
          </div>
        </div>
      </div>

      {availabilityFilterError ? (
        <p className={styles.validationText} role="alert">
          {availabilityFilterError}
        </p>
      ) : null}
    </div>
  );
}
