import { useCallback, useEffect, useMemo, useState } from "react";
import {
  cancelarReservaTutor,
  fetchReservasDetalleTutor,
  readTutorIdFromSession
} from "../services/reservaTutorApi";
import type { ReservaTutorDetalleDTO } from "../types/reservaTutor.types";
import { getReservaEstadoMeta } from "../utils/reservaEstadoUtils";
import { dispararNotificacion } from "../services/notificacionesApi"; // o la ruta correcta

const REFRESH_MS = 15000;

function getSortDate(reserva: ReservaTutorDetalleDTO): number {
  const raw = reserva.horaInicio ?? reserva.fechaSolicitud;
  if (!raw) return Number.MAX_SAFE_INTEGER;
  const time = new Date(raw).getTime();
  return Number.isNaN(time) ? Number.MAX_SAFE_INTEGER : time;
}

export function useTutorReservas() {
  const [reservas, setReservas] = useState<ReservaTutorDetalleDTO[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);

  const loadReservas = useCallback(async (mode: "initial" | "refresh" = "initial") => {
    if (mode === "initial") {
      setIsLoading(true);
    } else {
      setIsRefreshing(true);
    }

    try {
      const idTutor = readTutorIdFromSession();
      const data = await fetchReservasDetalleTutor(idTutor);
      setReservas([...data].sort((a, b) => getSortDate(a) - getSortDate(b)));
      setLastUpdated(new Date());
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "No se pudieron cargar tus reservas.");
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, []);

  useEffect(() => {
    void loadReservas("initial");
    const timer = window.setInterval(() => void loadReservas("refresh"), REFRESH_MS);
    return () => window.clearInterval(timer);
  }, [loadReservas]);

  const cancelarReserva = useCallback(
    async (reserva: ReservaTutorDetalleDTO) => {
      const meta = getReservaEstadoMeta(reserva);
      if (meta.key !== "solicitada") {
        setNotice("Solo puedes cancelar solicitudes que aun estan en estado Solicitada.");
        return;
      }

      try {
        await cancelarReservaTutor(reserva.idReserva);
        setNotice("Solicitud cancelada correctamente.");
        await loadReservas("refresh");
      } catch (e) {
        setNotice(e instanceof Error ? e.message : "No se pudo cancelar la solicitud.");
      }
    },
    [loadReservas]
  );

  const stats = useMemo(() => {
    return reservas.reduce(
      (acc, reserva) => {
        const key = getReservaEstadoMeta(reserva).key;
        acc.total += 1;
        if (key === "solicitada") acc.solicitadas += 1;
        if (key === "aceptada") acc.aceptadas += 1;
        if (key === "en_curso") acc.enCurso += 1;
        if (key === "finalizada") acc.finalizadas += 1;
        return acc;
      },
      { total: 0, solicitadas: 0, aceptadas: 0, enCurso: 0, finalizadas: 0 }
    );
  }, [reservas]);

  const lastUpdatedLabel = lastUpdated
    ? lastUpdated.toLocaleTimeString("es-CL", { hour: "2-digit", minute: "2-digit" })
    : "pendiente";

  return {
    reservas,
    isLoading,
    isRefreshing,
    error,
    notice,
    setNotice,
    lastUpdatedLabel,
    stats,
    reload: () => loadReservas("refresh"),
    cancelarReserva
  };
}
