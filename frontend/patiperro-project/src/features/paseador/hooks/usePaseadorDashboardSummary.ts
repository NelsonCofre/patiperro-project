import { useCallback, useEffect, useMemo, useState } from "react";
import { fetchPaseadorMiPerfil } from "../../auth/services/perfilCuentaService";
import { resenaApi } from "../../tutor/services/resenaApi";
import {
  fetchBloquesPorUsuario,
  readStoredPaseadorId,
  type AgendaBloqueDTO
} from "../services/agendaService";
import { fetchBilleteraPaseador } from "../services/billeteraPaseadorService";
import { getMyConfiguracion } from "../services/paseadorConfigService";
import { fetchSolicitudesPendientesPaseador } from "../services/solicitudesPaseadorService";
import { fetchDailySchedulePanel } from "../services/dailyScheduleService";
import { fetchVerificacionEstado } from "../services/verificacionPaseadorService";
import type { DailyScheduleItem } from "../types/dailySchedule.types";
import { esSolicitudPorResponder } from "../utils/solicitudEstadoUtils";

export type ProfileChecklistItem = {
  step: string;
  text: string;
  done: boolean;
  route: string;
};

export type DashboardMetric = {
  label: string;
  value: string;
  helper: string;
};

const WEEKDAY_PILLS = [
  { short: "Lun", prefixes: ["lun"] },
  { short: "Mar", prefixes: ["mar"] },
  { short: "Mié", prefixes: ["mie", "mié"] },
  { short: "Jue", prefixes: ["jue"] },
  { short: "Vie", prefixes: ["vie"] },
  { short: "Sáb", prefixes: ["sab", "sáb"] },
  { short: "Dom", prefixes: ["dom"] }
] as const;

function formatMoneyClp(value: number): string {
  return new Intl.NumberFormat("es-CL", {
    style: "currency",
    currency: "CLP",
    maximumFractionDigits: 0
  }).format(value);
}

function normalizeDayName(value: string): string {
  return value
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "");
}

function isFutureOrTodayBlock(fecha: string): boolean {
  if (!fecha) return false;
  const blockDate = new Date(`${fecha}T12:00:00`);
  if (Number.isNaN(blockDate.getTime())) return false;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  blockDate.setHours(0, 0, 0, 0);
  return blockDate >= today;
}

function countActiveBlocks(bloques: AgendaBloqueDTO[]): number {
  return bloques.filter((block) => {
    if (!isFutureOrTodayBlock(block.fecha)) return false;
    return block.estadoBloque.nombre.trim().toLowerCase() !== "reservado";
  }).length;
}

function buildActiveWeekdaySet(bloques: AgendaBloqueDTO[]): Set<string> {
  const active = new Set<string>();
  for (const pill of WEEKDAY_PILLS) {
    const hasBlock = bloques.some((block) => {
      if (!isFutureOrTodayBlock(block.fecha)) return false;
      const dayName = normalizeDayName(block.diaSemana?.nombre ?? "");
      return pill.prefixes.some((prefix) => dayName.startsWith(prefix));
    });
    if (hasBlock) {
      active.add(pill.short);
    }
  }
  return active;
}

function countTarifasPublicadas(tarifas: { precioPorHora: number }[]): number {
  return tarifas.filter((tarifa) => Number.isFinite(tarifa.precioPorHora) && tarifa.precioPorHora >= 1).length;
}

export function usePaseadorDashboardSummary() {
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [paseosRealizados, setPaseosRealizados] = useState(0);
  const [calificacionPromedio, setCalificacionPromedio] = useState(0);
  const [saldoDisponible, setSaldoDisponible] = useState(0);
  const [bloquesActivos, setBloquesActivos] = useState(0);
  const [pendingSolicitudes, setPendingSolicitudes] = useState(0);
  const [paseosHoy, setPaseosHoy] = useState(0);
  const [dailyTours, setDailyTours] = useState<DailyScheduleItem[]>([]);
  const [dailyToursError, setDailyToursError] = useState("");
  const [nombrePaseador, setNombrePaseador] = useState("");
  const [checklist, setChecklist] = useState<ProfileChecklistItem[]>([]);
  const [activeWeekdays, setActiveWeekdays] = useState<Set<string>>(new Set());

  const loadSummary = useCallback(async () => {
    setIsLoading(true);
    setLoadError("");
    const idPaseador = readStoredPaseadorId();
    if (idPaseador == null) {
      setLoadError("No se encontró tu sesión de paseador. Vuelve a iniciar sesión.");
      setIsLoading(false);
      return;
    }

    try {
      const results = await Promise.allSettled([
        fetchSolicitudesPendientesPaseador(),
        fetchBilleteraPaseador(),
        resenaApi.obtenerPromedioPaseador(idPaseador),
        fetchBloquesPorUsuario(idPaseador),
        getMyConfiguracion(),
        fetchPaseadorMiPerfil(),
        fetchVerificacionEstado(),
        fetchDailySchedulePanel()
      ]);

      const solicitudes = results[0].status === "fulfilled" ? results[0].value : [];
      const billetera = results[1].status === "fulfilled" ? results[1].value : null;
      const promedio = results[2].status === "fulfilled" ? results[2].value : 0;
      const bloques = results[3].status === "fulfilled" ? results[3].value : [];
      const config = results[4].status === "fulfilled" ? results[4].value : null;
      const perfil = results[5].status === "fulfilled" ? results[5].value : null;
      const verificacion = results[6].status === "fulfilled" ? results[6].value : null;
      const agendaHoy = results[7].status === "fulfilled" ? results[7].value : [];

      const finalizadas = solicitudes.filter((s) => s.estado === "Finalizada").length;
      const pendientes = solicitudes.filter((s) => esSolicitudPorResponder(s.estado)).length;
      const activos = countActiveBlocks(bloques);
      const tarifasPublicadas = countTarifasPublicadas(config?.tarifas ?? []);
      const tieneRadio = (config?.radioCoberturaKm ?? 0) > 0;
      const servicioConfigurado = tarifasPublicadas > 0 && tieneRadio;
      const perfilCompleto = Boolean(
        perfil?.biografia?.trim() && perfil?.fotoPerfil?.trim()
      );
      const verificacionAprobada = verificacion?.estado === "APROBADO";

      setPaseosRealizados(finalizadas);
      setCalificacionPromedio(Number.isFinite(promedio) ? promedio : 0);
      setSaldoDisponible(billetera?.disponible?.amount ?? 0);
      setBloquesActivos(activos);
      setPendingSolicitudes(pendientes);
      setPaseosHoy(agendaHoy.length);
      setDailyTours(agendaHoy);
      setDailyToursError(results[7].status === "rejected" ? "No se pudieron cargar los paseos de hoy." : "");
      setNombrePaseador(perfil?.nombreCompleto?.trim() || "");
      setActiveWeekdays(buildActiveWeekdaySet(bloques));
      setChecklist([
        {
          step: "01",
          text: "Publicar tarifas y radio de cobertura",
          done: servicioConfigurado,
          route: "/paseador/dashboard/configuracion"
        },
        {
          step: "02",
          text: "Cargar bloques de disponibilidad",
          done: activos > 0,
          route: "/paseador/dashboard/agenda"
        },
        {
          step: "03",
          text: "Completar biografía y foto de perfil",
          done: perfilCompleto,
          route: "/paseador/dashboard/perfil"
        },
        {
          step: "04",
          text: "Verificar identidad",
          done: verificacionAprobada,
          route: "/paseador/dashboard/verificacion"
        }
      ]);

      const failedCount = results.filter((result) => result.status === "rejected").length;
      if (failedCount === results.length) {
        setLoadError("No se pudieron cargar los datos del panel.");
      }
    } catch (error) {
      setLoadError(error instanceof Error ? error.message : "No se pudieron cargar los datos del panel.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadSummary();
  }, [loadSummary]);

  const profileStatus = useMemo(() => {
    if (checklist.length === 0) return "Cargando…";
    const done = checklist.filter((item) => item.done).length;
    if (done === checklist.length) return "Perfil activo";
    if (done === 0) return "Incompleto";
    return `En progreso (${done}/${checklist.length})`;
  }, [checklist]);

  const metrics: DashboardMetric[] = useMemo(
    () => [
      {
        label: "Paseos realizados",
        value: String(paseosRealizados),
        helper:
          paseosRealizados > 0
            ? "Servicios marcados como finalizados."
            : "Aparecerán aquí cuando completes tu primer paseo."
      },
      {
        label: "Calificación media",
        value: calificacionPromedio > 0 ? calificacionPromedio.toFixed(1) : "—",
        helper:
          calificacionPromedio > 0
            ? "Promedio según reseñas de tutores."
            : "Aún no tienes reseñas publicadas."
      },
      {
        label: "Saldo disponible",
        value: formatMoneyClp(saldoDisponible),
        helper:
          saldoDisponible > 0
            ? "Monto listo para retirar en tu billetera."
            : "Consulta tu billetera para ver retenido y disponible."
      }
    ],
    [calificacionPromedio, paseosRealizados, saldoDisponible]
  );

  const heroHighlights = useMemo(() => {
    const highlights: { label: string; text: string }[] = [];
    if (pendingSolicitudes > 0) {
      highlights.push({
        label: "Prioridad",
        text: `Tienes ${pendingSolicitudes} solicitud${pendingSolicitudes === 1 ? "" : "es"} por responder.`
      });
    }
    if (paseosHoy > 0) {
      highlights.push({
        label: "Hoy",
        text: `Tienes ${paseosHoy} paseo${paseosHoy === 1 ? "" : "s"} programado${paseosHoy === 1 ? "" : "s"}.`
      });
    }
    const nextStep = checklist.find((item) => !item.done);
    if (nextStep) {
      highlights.push({
        label: "Siguiente paso",
        text: nextStep.text
      });
    } else {
      highlights.push({
        label: "Estado",
        text: "Tu perfil está listo para recibir nuevas reservas."
      });
    }
    return highlights.slice(0, 2);
  }, [checklist, paseosHoy, pendingSolicitudes]);

  const agendaObjective = useMemo(() => {
    if (bloquesActivos > 0) {
      return `${bloquesActivos} bloque${bloquesActivos === 1 ? "" : "s"} disponible${bloquesActivos === 1 ? "" : "s"}`;
    }
    return "Crea tu primera disponibilidad semanal";
  }, [bloquesActivos]);

  return {
    isLoading,
    loadError,
    metrics,
    profileStatus,
    checklist,
    bloquesActivos,
    agendaObjective,
    heroHighlights,
    activeWeekdays,
    weekdayPills: WEEKDAY_PILLS,
    nombrePaseador,
    pendingSolicitudes,
    dailyTours,
    dailyToursError,
    reload: loadSummary
  };
}
