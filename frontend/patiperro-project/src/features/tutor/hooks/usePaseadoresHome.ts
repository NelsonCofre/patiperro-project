import { useCallback, useEffect, useMemo, useState } from "react";
import { resolveApiUrl, TUTOR_ID_SESSION_KEY } from "../../../config/api";
import type { PaseadorHome } from "../types/paseadorHome.types";
import { resenaApi } from "../services/resenaApi"; // IMPORTANTE: Importar el servicio de reseñas
import {
  fetchPaseadoresCercanos,
  fetchTutorPorId,
  type PaseadorCercanoApi
} from "../services/tutorCercanosApi";

const PAGE_SIZE = 6;
const CERCANOS_LIMITE = 50;
const MIN_SEARCH_RADIUS_KM = 1;
const MAX_SEARCH_RADIUS_KM = 120;
const ID_ESTADO_BLOQUE_DISPONIBLE = 1;

function clampSearchRadiusKm(km: number): number {
  const n = Math.round(Number(km));
  if (!Number.isFinite(n)) return 10;
  return Math.min(MAX_SEARCH_RADIUS_KM, Math.max(MIN_SEARCH_RADIUS_KM, n));
}

export type PaseadoresSortMode = "distance" | "name" | "coverage";

type LocationStatus = "idle" | "requesting" | "granted" | "denied" | "unsupported" | "error";

type TutorCoordinates = {
  latitude: number;
  longitude: number;
};

type ProfileCoords = { lat: number; lon: number };

function parseTutorIdFromSession(): number | null {
  const raw = sessionStorage.getItem(TUTOR_ID_SESSION_KEY);
  if (!raw) return null;
  const n = Number.parseInt(raw, 10);
  return Number.isFinite(n) && n > 0 ? n : null;
}

function parseRadioKm(value: PaseadorCercanoApi["radioCoberturaKm"]): number {
  if (value == null) return 0;
  const n = typeof value === "number" ? value : Number(value);
  return Number.isFinite(n) ? n : 0;
}

function mapCercanoToHome(dto: PaseadorCercanoApi): PaseadorHome {
  const radio = parseRadioKm(dto.radioCoberturaKm);
  
  return {
    id: String(dto.idPaseador),
    nombre: dto.nombreCompleto ?? "",
    fotoUrl: resolveApiUrl(dto.fotoPerfil ?? ""),
    distanciaKm: dto.distanciaKm,
    // Nota: calificacionPromedio se inicializa con lo que traiga la API o 0
    calificacionPromedio: dto.calificacionPromedio ?? 0, 
    precioBase: 0,
    disponible: true,
    perfilCompleto: true,
    tieneBloqueDisponibleFuturo: true,
    tieneTarifaConfigurada: true,
    radioCoberturaKm: radio,
    proximoBloque: "-",
    bio: (dto.biografia ?? "").trim() || "Sin biografia por ahora.",
    latitud: dto.latitud,
    longitud: dto.longitud
  };
}

export function usePaseadoresHome() {
  const [profileLoadDone, setProfileLoadDone] = useState(false);
  const [profileCoords, setProfileCoords] = useState<ProfileCoords | null>(null);

  const [listLoading, setListLoading] = useState(false);
  const [paseadores, setPaseadores] = useState<PaseadorHome[]>([]);
  const [listError, setListError] = useState<string | null>(null);
  const [lastUpdatedAt, setLastUpdatedAt] = useState<Date | null>(null);

  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
  const [searchRadiusKm, setSearchRadiusKm] = useState(10);
  const [cercanosFetchNonce, setCercanosFetchNonce] = useState(0);

  const [queryText, setQueryText] = useState("");
  const [maxDistanceFilterKm, setMaxDistanceFilterKm] = useState<number | null>(null);
  const [sortMode, setSortMode] = useState<PaseadoresSortMode>("distance");
  const [availabilityDate, setAvailabilityDate] = useState("");
  const [availabilityStartTime, setAvailabilityStartTime] = useState("");
  const [availabilityEndTime, setAvailabilityEndTime] = useState("");
  
  const [minRating, setMinRating] = useState<number>(0);

  const [locationStatus, setLocationStatus] = useState<LocationStatus>("idle");
  const [coordinates, setCoordinates] = useState<TutorCoordinates | null>(null);
  const [locationError, setLocationError] = useState("");

  const refLat = useMemo(() => {
    if (locationStatus === "granted" && coordinates) {
      return coordinates.latitude;
    }
    return profileCoords?.lat ?? null;
  }, [locationStatus, coordinates, profileCoords]);

  const refLon = useMemo(() => {
    if (locationStatus === "granted" && coordinates) {
      return coordinates.longitude;
    }
    return profileCoords?.lon ?? null;
  }, [locationStatus, coordinates, profileCoords]);

  const scheduleFilterState = useMemo(() => {
    const date = availabilityDate.trim();
    const start = availabilityStartTime.trim();
    const end = availabilityEndTime.trim();
    const hasAny = Boolean(date || start || end);
    const hasAll = Boolean(date && start && end);

    if (!hasAny) {
      return { hasAny, isComplete: false, error: "", params: null };
    }

    if (!hasAll) {
      return {
        hasAny,
        isComplete: false,
        error: "Para filtrar por horario debes seleccionar fecha, hora de inicio y hora de termino.",
        params: null
      };
    }

    if (start >= end) {
      return {
        hasAny,
        isComplete: false,
        error: "La hora de termino debe ser posterior a la hora de inicio.",
        params: null
      };
    }

    return {
      hasAny,
      isComplete: true,
      error: "",
      params: {
        fechaDisponibilidad: date,
        horaInicioDisponibilidad: `${date}T${start}:00`,
        horaFinDisponibilidad: `${date}T${end}:00`,
        idEstadoBloqueDisponible: ID_ESTADO_BLOQUE_DISPONIBLE
      }
    };
  }, [availabilityDate, availabilityStartTime, availabilityEndTime]);

  useEffect(() => {
    const tutorId = parseTutorIdFromSession();
    if (tutorId == null) {
      setProfileCoords(null);
      setProfileLoadDone(true);
      return;
    }

    let cancelled = false;
    setProfileLoadDone(false);

    fetchTutorPorId(tutorId)
      .then((perfil) => {
        if (cancelled) return;
        const d = perfil.direccion;
        const lat = d?.latitud;
        const lon = d?.longitud;
        if (
          typeof lat === "number" &&
          Number.isFinite(lat) &&
          typeof lon === "number" &&
          Number.isFinite(lon)
        ) {
          setProfileCoords({ lat, lon });
        } else {
          setProfileCoords(null);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setProfileCoords(null);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setProfileLoadDone(true);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!profileLoadDone) {
      return;
    }

    if (refLat == null || refLon == null) {
      setPaseadores([]);
      setListError(null);
      setListLoading(false);
      return;
    }

    if (scheduleFilterState.hasAny && !scheduleFilterState.isComplete) {
      setPaseadores([]);
      setListError(null);
      setListLoading(false);
      return;
    }

    let cancelled = false;
    setListLoading(true);
    setListError(null);

    fetchPaseadoresCercanos({
      latitudReferencia: refLat,
      longitudReferencia: refLon,
      radioBusquedaMaxKm: searchRadiusKm,
      limite: CERCANOS_LIMITE,
      ...(scheduleFilterState.params ?? {})
    })
      .then((rows) => {
        if (cancelled) return;
        const mapped = rows.map(mapCercanoToHome).sort((a, b) => a.distanciaKm - b.distanciaKm);
        setPaseadores(mapped);
        setLastUpdatedAt(new Date());
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          setPaseadores([]);
          setListError(e instanceof Error ? e.message : "Error al cargar paseadores.");
        }
      })
      .finally(() => {
        if (!cancelled) {
          setListLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [profileLoadDone, refLat, refLon, searchRadiusKm, cercanosFetchNonce, scheduleFilterState]);

  // NUEVO: EFECTO PARA SINCRONIZAR CALIFICACIONES REALES
  useEffect(() => {
    if (paseadores.length === 0) return;

    const actualizarPromediosReales = async () => {
      const actualizaciones = await Promise.all(
        paseadores.map(async (p) => {
          try {
            // Consultamos la "fuente de verdad" en el microservicio de reseñas
            const ratingReal = await resenaApi.obtenerPromedioPaseador(Number(p.id));
            return { ...p, calificacionPromedio: ratingReal || 0 };
          } catch {
            return p;
          }
        })
      );

      const hayCambios = actualizaciones.some(
        (p, i) => p.calificacionPromedio !== paseadores[i].calificacionPromedio
      );

      if (hayCambios) {
        setPaseadores(actualizaciones);
      }
    };

    actualizarPromediosReales();
  }, [lastUpdatedAt]);

  useEffect(() => {
    if (maxDistanceFilterKm != null && maxDistanceFilterKm > searchRadiusKm) {
      setMaxDistanceFilterKm(null);
    }
  }, [searchRadiusKm, maxDistanceFilterKm]);

  const isLoading =
    !profileLoadDone ||
    (refLat != null && refLon != null && listLoading && paseadores.length === 0);

  const applySearchRadiusKm = useCallback((km: number) => {
    setSearchRadiusKm(clampSearchRadiusKm(km));
    setCercanosFetchNonce((n) => n + 1);
    setVisibleCount(PAGE_SIZE);
  }, []);

  const filteredPaseadores = useMemo(() => {
    let list = [...paseadores];

    // 1. Filtro por Texto
    const q = queryText.trim().toLowerCase();
    if (q) {
      list = list.filter(
        (p) =>
          p.nombre.toLowerCase().includes(q) ||
          p.bio.toLowerCase().includes(q)
      );
    }

    // 2. Filtro por Distancia
    const capKm =
      maxDistanceFilterKm != null
        ? Math.min(maxDistanceFilterKm, searchRadiusKm)
        : searchRadiusKm;
    list = list.filter((p) => p.distanciaKm <= capKm);

    // 3. Filtro por Calificación Mínima
    if (minRating > 0) {
      list = list.filter((p) => {
        const cumpleNota = p.calificacionPromedio >= minRating;
        const noEsNuevo = p.calificacionPromedio > 0;
        return cumpleNota && noEsNuevo;
      });
    }

    // 4. Ordenamiento
    switch (sortMode) {
      case "name":
        list.sort((a, b) => a.nombre.localeCompare(b.nombre, "es"));
        break;
      case "coverage":
        list.sort((a, b) => b.radioCoberturaKm - a.radioCoberturaKm);
        break;
      default:
        list.sort((a, b) => a.distanciaKm - b.distanciaKm);
    }

    return list;
  }, [paseadores, queryText, maxDistanceFilterKm, searchRadiusKm, sortMode, minRating]);

  useEffect(() => {
    setVisibleCount(PAGE_SIZE);
  }, [queryText, maxDistanceFilterKm, sortMode, paseadores, availabilityDate, availabilityStartTime, availabilityEndTime, minRating]);

  const visiblePaseadores = useMemo(
    () => filteredPaseadores.slice(0, visibleCount),
    [filteredPaseadores, visibleCount]
  );
  
  const hasMore = visibleCount < filteredPaseadores.length;

  const resetFilters = useCallback(() => {
    setQueryText("");
    setMaxDistanceFilterKm(null);
    setSortMode("distance");
    setAvailabilityDate("");
    setAvailabilityStartTime("");
    setAvailabilityEndTime("");
    setMinRating(0);
  }, []);

  const loadMore = useCallback(() => {
    setVisibleCount((c) => c + PAGE_SIZE);
  }, []);

  const expandSearch = useCallback(() => {
    setSearchRadiusKm((c) => clampSearchRadiusKm(c + 5));
    setCercanosFetchNonce((n) => n + 1);
    setVisibleCount(PAGE_SIZE);
  }, []);

  const requestTutorLocation = useCallback(() => {
    if (!("geolocation" in navigator)) {
      setLocationStatus("unsupported");
      setLocationError("Tu navegador no soporta geolocalizacion.");
      return;
    }

    setLocationStatus("requesting");
    setLocationError("");

    navigator.geolocation.getCurrentPosition(
      (position) => {
        setCoordinates({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude
        });
        setLocationStatus("granted");
      },
      (error) => {
        setCoordinates(null);
        if (error.code === error.PERMISSION_DENIED) {
          setLocationStatus("denied");
          setLocationError("No pudimos acceder a tu ubicacion.");
          return;
        }
        setLocationStatus("error");
        setLocationError("No pudimos obtener tu ubicacion.");
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
    );
  }, []);

  const needsReferencePoint = profileLoadDone && (refLat == null || refLon == null);

  return {
    isLoading,
    profileLoadDone,
    needsReferencePoint,
    searchRadiusKm,
    applySearchRadiusKm,
    minSearchRadiusKm: MIN_SEARCH_RADIUS_KM,
    maxSearchRadiusKm: MAX_SEARCH_RADIUS_KM,
    lastUpdatedLabel: lastUpdatedAt?.toLocaleTimeString("es-CL", { hour: "2-digit", minute: "2-digit" }) ?? "--:--",
    locationStatus,
    coordinates,
    locationError,
    listError,
    paseadores,
    filteredCount: filteredPaseadores.length,
    visiblePaseadores,
    hasMore,
    loadMore,
    expandSearch,
    requestTutorLocation,
    queryText,
    setQueryText,
    availabilityDate,
    setAvailabilityDate,
    availabilityStartTime,
    setAvailabilityStartTime,
    availabilityEndTime,
    setAvailabilityEndTime,
    availabilityFilterError: scheduleFilterState.error,
    maxDistanceFilterKm,
    setMaxDistanceFilterKm,
    sortMode,
    setSortMode,
    resetFilters,
    minRating,
    setMinRating,
    refLat,
    refLon,
    hasActiveFilters:
      queryText.trim().length > 0 ||
      maxDistanceFilterKm != null ||
      sortMode !== "distance" ||
      scheduleFilterState.hasAny ||
      minRating > 0
  };
}