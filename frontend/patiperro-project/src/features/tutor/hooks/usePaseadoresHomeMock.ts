import { useEffect, useMemo, useState } from "react";
import type { PaseadorHome } from "../types/paseadorHome.types";

const PAGE_SIZE = 6;
const REFRESH_INTERVAL_MS = 30_000;

type LocationStatus = "idle" | "requesting" | "granted" | "denied" | "unsupported" | "error";

type TutorCoordinates = {
  latitude: number;
  longitude: number;
};

const MOCK_PASEADORES: PaseadorHome[] = [
  {
    id: "paseador-1",
    nombre: "Valentina Rojas",
    fotoUrl:
      "https://images.pexels.com/photos/4064739/pexels-photo-4064739.jpeg?auto=compress&cs=tinysrgb&w=800",
    distanciaKm: 1.2,
    calificacionPromedio: 4.9,
    precioBase: 8500,
    disponible: true,
    perfilCompleto: true,
    tieneBloqueDisponibleFuturo: true,
    tieneTarifaConfigurada: true,
    radioCoberturaKm: 4,
    proximoBloque: "Hoy, 17:30",
    bio: "Paseos tranquilos, reportes claros y cuidado especial para perros pequenos y medianos."
  },
  {
    id: "paseador-2",
    nombre: "Diego Morales",
    fotoUrl:
      "https://images.pexels.com/photos/7210701/pexels-photo-7210701.jpeg?auto=compress&cs=tinysrgb&w=800",
    distanciaKm: 2.4,
    calificacionPromedio: 4.8,
    precioBase: 9000,
    disponible: true,
    perfilCompleto: true,
    tieneBloqueDisponibleFuturo: true,
    tieneTarifaConfigurada: true,
    radioCoberturaKm: 6,
    proximoBloque: "Manana, 10:00",
    bio: "Experiencia con perros activos y paseos de energia media en zonas residenciales."
  },
  {
    id: "paseador-3",
    nombre: "Camila Torres",
    fotoUrl:
      "https://images.pexels.com/photos/6131150/pexels-photo-6131150.jpeg?auto=compress&cs=tinysrgb&w=800",
    distanciaKm: 3.1,
    calificacionPromedio: 5,
    precioBase: 9500,
    disponible: true,
    perfilCompleto: true,
    tieneBloqueDisponibleFuturo: true,
    tieneTarifaConfigurada: true,
    radioCoberturaKm: 5,
    proximoBloque: "Viernes, 09:00",
    bio: "Acompanamiento cercano, caminatas seguras y mucha paciencia para perros nerviosos."
  },
  {
    id: "paseador-4",
    nombre: "Matias Herrera",
    fotoUrl:
      "https://images.pexels.com/photos/8498831/pexels-photo-8498831.jpeg?auto=compress&cs=tinysrgb&w=800",
    distanciaKm: 4.6,
    calificacionPromedio: 4.7,
    precioBase: 8000,
    disponible: true,
    perfilCompleto: true,
    tieneBloqueDisponibleFuturo: true,
    tieneTarifaConfigurada: true,
    radioCoberturaKm: 6,
    proximoBloque: "Sabado, 12:00",
    bio: "Paseos por parques cercanos y seguimiento del bienestar despues de cada salida."
  },
  {
    id: "paseador-5",
    nombre: "Fernanda Silva",
    fotoUrl:
      "https://images.pexels.com/photos/6131167/pexels-photo-6131167.jpeg?auto=compress&cs=tinysrgb&w=800",
    distanciaKm: 5.3,
    calificacionPromedio: 4.9,
    precioBase: 10000,
    disponible: true,
    perfilCompleto: true,
    tieneBloqueDisponibleFuturo: true,
    tieneTarifaConfigurada: true,
    radioCoberturaKm: 8,
    proximoBloque: "Domingo, 16:00",
    bio: "Rutinas personalizadas para mascotas mayores o con necesidades de paseo mas pausado."
  },
  {
    id: "paseador-6",
    nombre: "Ignacio Perez",
    fotoUrl:
      "https://images.pexels.com/photos/7210754/pexels-photo-7210754.jpeg?auto=compress&cs=tinysrgb&w=800",
    distanciaKm: 6.8,
    calificacionPromedio: 4.6,
    precioBase: 7800,
    disponible: true,
    perfilCompleto: true,
    tieneBloqueDisponibleFuturo: true,
    tieneTarifaConfigurada: true,
    radioCoberturaKm: 9,
    proximoBloque: "Lunes, 08:30",
    bio: "Ideal para paseos cortos de rutina y perros que necesitan constancia semanal."
  },
  {
    id: "paseador-7",
    nombre: "Antonia Fuentes",
    fotoUrl:
      "https://images.pexels.com/photos/6898858/pexels-photo-6898858.jpeg?auto=compress&cs=tinysrgb&w=800",
    distanciaKm: 8.2,
    calificacionPromedio: 4.8,
    precioBase: 9200,
    disponible: true,
    perfilCompleto: true,
    tieneBloqueDisponibleFuturo: true,
    tieneTarifaConfigurada: true,
    radioCoberturaKm: 10,
    proximoBloque: "Martes, 18:00",
    bio: "Paseos con enfoque en socializacion tranquila y reporte simple para tutores."
  },
  {
    id: "paseador-8",
    nombre: "Sebastian Luna",
    fotoUrl:
      "https://images.pexels.com/photos/7210278/pexels-photo-7210278.jpeg?auto=compress&cs=tinysrgb&w=800",
    distanciaKm: 9.4,
    calificacionPromedio: 4.5,
    precioBase: 8700,
    disponible: true,
    perfilCompleto: true,
    tieneBloqueDisponibleFuturo: true,
    tieneTarifaConfigurada: true,
    radioCoberturaKm: 10,
    proximoBloque: "Miercoles, 11:30",
    bio: "Disponible para perros medianos y grandes con caminatas controladas."
  },
  {
    id: "paseador-9",
    nombre: "Rocío Navarro",
    fotoUrl:
      "https://images.pexels.com/photos/6131163/pexels-photo-6131163.jpeg?auto=compress&cs=tinysrgb&w=800",
    distanciaKm: 2.9,
    calificacionPromedio: 4.7,
    precioBase: 0,
    disponible: true,
    perfilCompleto: false,
    tieneBloqueDisponibleFuturo: true,
    tieneTarifaConfigurada: false,
    radioCoberturaKm: 6,
    proximoBloque: "Hoy, 19:00",
    bio: "Este perfil mock no aparece porque falta completar tarifa y datos del perfil."
  },
  {
    id: "paseador-10",
    nombre: "Pedro Campos",
    fotoUrl:
      "https://images.pexels.com/photos/7210758/pexels-photo-7210758.jpeg?auto=compress&cs=tinysrgb&w=800",
    distanciaKm: 3.7,
    calificacionPromedio: 4.4,
    precioBase: 8200,
    disponible: false,
    perfilCompleto: true,
    tieneBloqueDisponibleFuturo: false,
    tieneTarifaConfigurada: true,
    radioCoberturaKm: 7,
    proximoBloque: "Sin bloques disponibles",
    bio: "Este perfil mock no aparece porque no tiene bloques disponibles futuros."
  },
  {
    id: "paseador-11",
    nombre: "María Paz Soto",
    fotoUrl:
      "https://images.pexels.com/photos/7210304/pexels-photo-7210304.jpeg?auto=compress&cs=tinysrgb&w=800",
    distanciaKm: 7.6,
    calificacionPromedio: 4.9,
    precioBase: 9800,
    disponible: true,
    perfilCompleto: true,
    tieneBloqueDisponibleFuturo: true,
    tieneTarifaConfigurada: true,
    radioCoberturaKm: 5,
    proximoBloque: "Jueves, 15:00",
    bio: "Este perfil mock no aparece porque el tutor queda fuera de su radio de cobertura."
  }
];

function isPaseadorVisibleForTutor(paseador: PaseadorHome, searchRadiusKm: number) {
  return (
    paseador.disponible &&
    paseador.perfilCompleto &&
    paseador.tieneBloqueDisponibleFuturo &&
    paseador.tieneTarifaConfigurada &&
    paseador.distanciaKm <= searchRadiusKm &&
    paseador.distanciaKm <= paseador.radioCoberturaKm
  );
}

export function usePaseadoresHomeMock() {
  const [isLoading, setIsLoading] = useState(true);
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
  const [searchRadiusKm, setSearchRadiusKm] = useState(10);
  const [lastUpdatedAt, setLastUpdatedAt] = useState(() => new Date());
  const [refreshTick, setRefreshTick] = useState(0);
  const [locationStatus, setLocationStatus] = useState<LocationStatus>("idle");
  const [coordinates, setCoordinates] = useState<TutorCoordinates | null>(null);
  const [locationError, setLocationError] = useState("");

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setIsLoading(false);
    }, 850);

    return () => window.clearTimeout(timeoutId);
  }, []);

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      setRefreshTick((current) => current + 1);
      setLastUpdatedAt(new Date());
    }, REFRESH_INTERVAL_MS);

    return () => window.clearInterval(intervalId);
  }, []);

  const paseadores = useMemo(
    () =>
      MOCK_PASEADORES
        .filter((paseador) => isPaseadorVisibleForTutor(paseador, searchRadiusKm))
        .sort((a, b) => a.distanciaKm - b.distanciaKm),
    [refreshTick, searchRadiusKm]
  );

  const visiblePaseadores = paseadores.slice(0, visibleCount);
  const hasMore = visibleCount < paseadores.length;

  const loadMore = () => {
    setVisibleCount((current) => current + PAGE_SIZE);
  };

  const expandSearch = () => {
    setSearchRadiusKm((current) => current + 5);
    setVisibleCount(PAGE_SIZE);
  };

  const requestTutorLocation = () => {
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
  };

  return {
    isLoading,
    searchRadiusKm,
    lastUpdatedLabel: lastUpdatedAt.toLocaleTimeString("es-CL", {
      hour: "2-digit",
      minute: "2-digit"
    }),
    locationStatus,
    coordinates,
    locationError,
    paseadores,
    visiblePaseadores,
    hasMore,
    loadMore,
    expandSearch,
    requestTutorLocation
  };
}
