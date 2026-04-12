import { useCallback, useEffect, useMemo, useState } from "react";
import { resolveApiUrl, TUTOR_ID_SESSION_KEY } from "../../../config/api";
import type { PaseadorHome } from "../types/paseadorHome.types";
import {
  fetchPaseadoresCercanos,
  fetchTutorPorId,
  type PaseadorCercanoApi
} from "../services/tutorCercanosApi";

const PAGE_SIZE = 6;
const CERCANOS_LIMITE = 50;
const MIN_SEARCH_RADIUS_KM = 1;
const MAX_SEARCH_RADIUS_KM = 120;

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
    calificacionPromedio: 0,
    precioBase: 0,
    disponible: true,
    perfilCompleto: true,
    tieneBloqueDisponibleFuturo: true,
    tieneTarifaConfigurada: true,
    radioCoberturaKm: radio,
    proximoBloque: "-",
    bio: (dto.biografia ?? "").trim() || "Sin biografia por ahora."
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
  /** Fuerza nueva peticion aunque el radio en km no cambie (mismo valor o reclamp al maximo). */
  const [cercanosFetchNonce, setCercanosFetchNonce] = useState(0);

  const [queryText, setQueryText] = useState("");
  const [maxDistanceFilterKm, setMaxDistanceFilterKm] = useState<number | null>(null);
  const [sortMode, setSortMode] = useState<PaseadoresSortMode>("distance");

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

    let cancelled = false;
    setListLoading(true);
    setListError(null);

    fetchPaseadoresCercanos({
      latitudReferencia: refLat,
      longitudReferencia: refLon,
      radioBusquedaMaxKm: searchRadiusKm,
      limite: CERCANOS_LIMITE
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
  }, [profileLoadDone, refLat, refLon, searchRadiusKm, cercanosFetchNonce]);

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
    const q = queryText.trim().toLowerCase();
    if (q) {
      list = list.filter(
        (p) =>
          p.nombre.toLowerCase().includes(q) ||
          p.bio.toLowerCase().includes(q)
      );
    }
    const capKm =
      maxDistanceFilterKm != null
        ? Math.min(maxDistanceFilterKm, searchRadiusKm)
        : searchRadiusKm;
    list = list.filter((p) => p.distanciaKm <= capKm);

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
  }, [paseadores, queryText, maxDistanceFilterKm, searchRadiusKm, sortMode]);

  useEffect(() => {
    setVisibleCount(PAGE_SIZE);
  }, [queryText, maxDistanceFilterKm, sortMode, paseadores]);

  const visiblePaseadores = useMemo(
    () => filteredPaseadores.slice(0, visibleCount),
    [filteredPaseadores, visibleCount]
  );
  const hasMore = visibleCount < filteredPaseadores.length;

  const resetFilters = useCallback(() => {
    setQueryText("");
    setMaxDistanceFilterKm(null);
    setSortMode("distance");
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
          setLocationError(
            "No pudimos acceder a tu ubicacion. Puedes ingresar tu direccion manualmente."
          );
          return;
        }

        setLocationStatus("error");
        setLocationError("No pudimos obtener tu ubicacion en este momento.");
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 60000
      }
    );
  }, []);

  const needsReferencePoint =
    profileLoadDone && (refLat == null || refLon == null);

  return {
    isLoading,
    profileLoadDone,
    needsReferencePoint,
    searchRadiusKm,
    applySearchRadiusKm,
    minSearchRadiusKm: MIN_SEARCH_RADIUS_KM,
    maxSearchRadiusKm: MAX_SEARCH_RADIUS_KM,
    lastUpdatedLabel:
      lastUpdatedAt?.toLocaleTimeString("es-CL", {
        hour: "2-digit",
        minute: "2-digit"
      }) ?? "--:--",
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
    maxDistanceFilterKm,
    setMaxDistanceFilterKm,
    sortMode,
    setSortMode,
    resetFilters,
    hasActiveFilters:
      queryText.trim().length > 0 ||
      maxDistanceFilterKm != null ||
      sortMode !== "distance"
  };
}
