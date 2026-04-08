// Agenda del paseador integrada con el microservicio: semana navegable + CRUD de bloques.
import { useCallback, useEffect, useMemo, useState } from "react";
import type { AgendaToast } from "../types/agenda.types";
import {
  crearBloque,
  crearSerieMensualBloques,
  eliminarBloque,
  fetchBloquesPorUsuario,
  fetchDiasSemana,
  fetchEstadosBloque,
  readStoredPaseadorId,
  type AgendaBloqueDTO,
  type DiaSemanaDTO,
  type EstadoBloqueDTO
} from "../services/agendaService";
import {
  addDays,
  buildWeekDays,
  MAX_WEEKS_AHEAD,
  normalizeDayName,
  startOfWeekMonday,
  toISODateLocal,
  weekdayLabelFromISODate,
  weeksBetween,
  WEEKDAY_LABELS
} from "../utils/agendaWeekUtils";

const HOUR_SLOTS = [
  "07:00",
  "08:00",
  "09:00",
  "10:00",
  "11:00",
  "12:00",
  "13:00",
  "14:00",
  "15:00",
  "16:00",
  "17:00",
  "18:00",
  "19:00",
  "20:00",
  "21:00"
];

export type AgendaApiForm = {
  fecha: string;
  startTime: string;
  endTime: string;
};

export type AgendaApiFormErrors = {
  fecha?: string;
  startTime?: string;
  endTime?: string;
};

function timeToMinutes(value: string): number {
  const [hours, minutes] = value.split(":").map(Number);
  return hours * 60 + minutes;
}

function formatDuration(startTime: string, endTime: string): string {
  const durationMinutes = timeToMinutes(endTime) - timeToMinutes(startTime);
  const hours = Math.floor(durationMinutes / 60);
  const minutes = durationMinutes % 60;

  if (hours > 0 && minutes > 0) {
    return `${hours} h ${minutes} min`;
  }
  if (hours > 0) {
    return `${hours} h`;
  }
  return `${minutes} min`;
}

function parseISOToMinutes(iso: string): number {
  const d = new Date(iso);
  return d.getHours() * 60 + d.getMinutes();
}

function validateForm(form: AgendaApiForm): AgendaApiFormErrors {
  const errors: AgendaApiFormErrors = {};
  if (!form.fecha) {
    errors.fecha = "Indica una fecha.";
  }
  if (!form.startTime) {
    errors.startTime = "Selecciona hora de inicio.";
  }
  if (!form.endTime) {
    errors.endTime = "Selecciona hora de término.";
  }
  if (form.startTime && form.endTime && timeToMinutes(form.endTime) <= timeToMinutes(form.startTime)) {
    errors.endTime = "La hora de término debe ser posterior a la de inicio.";
  }
  return errors;
}

function overlapsApiBlocks(
  bloques: AgendaBloqueDTO[],
  fecha: string,
  startTime: string,
  endTime: string,
  excludeIdAgenda?: number
): boolean {
  const s = timeToMinutes(startTime);
  const e = timeToMinutes(endTime);
  return bloques.some((b) => {
    if (b.fecha !== fecha) return false;
    if (excludeIdAgenda != null && b.idAgenda === excludeIdAgenda) return false;
    const bs = parseISOToMinutes(b.horaInicio);
    const be = parseISOToMinutes(b.horaFinal);
    return s < be && e > bs;
  });
}

function findDiaId(dias: DiaSemanaDTO[], fechaISO: string): number | null {
  const d = new Date(`${fechaISO}T12:00:00`);
  const name = WEEKDAY_LABELS[d.getDay()];
  const target = normalizeDayName(name);
  const found = dias.find((x) => normalizeDayName(x.nombre) === target);
  return found?.idDia ?? null;
}

function pickEstadoDisponible(estados: EstadoBloqueDTO[]): number | null {
  if (!estados.length) return null;
  const notReservado = estados.find(
    (e) => !e.nombre.trim().toLowerCase().includes("reservad")
  );
  const pick = notReservado ?? estados[0];
  const id = Number(pick.idEstado);
  return Number.isFinite(id) ? id : null;
}

export type TimelineBlockView = {
  id: string;
  idAgenda: number;
  startTime: string;
  endTime: string;
  startMinutes: number;
  endMinutes: number;
  title: string;
  status: "available" | "booked";
  durationLabel: string;
};

function dtoToTimelineView(dto: AgendaBloqueDTO): TimelineBlockView {
  const start = new Date(dto.horaInicio);
  const end = new Date(dto.horaFinal);
  const pad = (n: number) => String(n).padStart(2, "0");
  const startTime = `${pad(start.getHours())}:${pad(start.getMinutes())}`;
  const endTime = `${pad(end.getHours())}:${pad(end.getMinutes())}`;
  const reservado = dto.estadoBloque.nombre.trim().toLowerCase() === "reservado";
  return {
    id: String(dto.idAgenda),
    idAgenda: dto.idAgenda,
    startTime,
    endTime,
    startMinutes: start.getHours() * 60 + start.getMinutes(),
    endMinutes: end.getHours() * 60 + end.getMinutes(),
    title: dto.estadoBloque.nombre,
    status: reservado ? "booked" : "available",
    durationLabel: formatDuration(startTime, endTime)
  };
}

const INITIAL_FORM: AgendaApiForm = {
  fecha: toISODateLocal(new Date()),
  startTime: "10:00",
  endTime: "11:00"
};

export function usePaseadorAgendaApi() {
  const paseadorId = readStoredPaseadorId();

  const [weekMonday, setWeekMonday] = useState(() => startOfWeekMonday(new Date()));
  const [selectedISODate, setSelectedISODate] = useState(() => toISODateLocal(new Date()));

  const [bloques, setBloques] = useState<AgendaBloqueDTO[]>([]);
  const [bloquesLoading, setBloquesLoading] = useState(false);
  const [bloquesError, setBloquesError] = useState<string | null>(null);

  const [diasSemana, setDiasSemana] = useState<DiaSemanaDTO[]>([]);
  const [estadosBloque, setEstadosBloque] = useState<EstadoBloqueDTO[]>([]);
  const [catalogLoading, setCatalogLoading] = useState(true);
  const [catalogError, setCatalogError] = useState<string | null>(null);

  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [form, setForm] = useState<AgendaApiForm>(INITIAL_FORM);
  const [formErrors, setFormErrors] = useState<AgendaApiFormErrors>({});
  const [toast, setToast] = useState<AgendaToast | null>(null);
  const [saving, setSaving] = useState(false);
  /** Repetir la misma franja en todos los [día de la semana] restantes del mes (servidor omite pasado y solapes). */
  const [repeatMismoDiaEnMes, setRepeatMismoDiaEnMes] = useState(false);

  const weekDays = useMemo(() => buildWeekDays(weekMonday), [weekMonday]);

  const canGoPrevWeek = useMemo(() => {
    const cur = startOfWeekMonday(new Date());
    return weekMonday.getTime() > cur.getTime();
  }, [weekMonday]);

  const canGoNextWeek = useMemo(() => {
    const cur = startOfWeekMonday(new Date());
    const n = weeksBetween(cur, weekMonday);
    return n < MAX_WEEKS_AHEAD;
  }, [weekMonday]);

  useEffect(() => {
    const days = buildWeekDays(weekMonday);
    const today = toISODateLocal(new Date());
    const inWeek = days.find((w) => w.iso === today);
    setSelectedISODate(inWeek ? today : days[0].iso);
  }, [weekMonday]);

  const refreshBloques = useCallback(async () => {
    if (paseadorId == null) {
      setBloques([]);
      setBloquesError(
        "No hay id de paseador en sesión. Vuelve a iniciar sesión para sincronizar la agenda."
      );
      return;
    }
    setBloquesLoading(true);
    setBloquesError(null);
    try {
      const list = await fetchBloquesPorUsuario(paseadorId);
      setBloques(list);
    } catch (e) {
      setBloques([]);
      setBloquesError(e instanceof Error ? e.message : "No se pudieron cargar los bloques.");
    } finally {
      setBloquesLoading(false);
    }
  }, [paseadorId]);

  useEffect(() => {
    refreshBloques();
  }, [refreshBloques]);

  useEffect(() => {
    if (paseadorId == null) {
      setCatalogLoading(false);
      setCatalogError("Inicia sesión como paseador para cargar catálogos.");
      return;
    }
    let cancelado = false;
    (async () => {
      setCatalogLoading(true);
      setCatalogError(null);
      try {
        const [dias, estados] = await Promise.all([fetchDiasSemana(), fetchEstadosBloque()]);
        if (!cancelado) {
          setDiasSemana(dias);
          setEstadosBloque(estados);
        }
      } catch (e) {
        if (!cancelado) {
          setCatalogError(e instanceof Error ? e.message : "Error al cargar catálogos.");
        }
      } finally {
        if (!cancelado) {
          setCatalogLoading(false);
        }
      }
    })();
    return () => {
      cancelado = true;
    };
  }, [paseadorId]);

  const idEstadoDisponible = useMemo(() => pickEstadoDisponible(estadosBloque), [estadosBloque]);

  const bloquesDelDia = useMemo(
    () => bloques.filter((b) => b.fecha === selectedISODate),
    [bloques, selectedISODate]
  );

  const selectedDayBlocks = useMemo(
    () => bloquesDelDia.map(dtoToTimelineView),
    [bloquesDelDia]
  );

  const selectedDayLabel = useMemo(() => weekdayLabelFromISODate(selectedISODate), [selectedISODate]);

  /** El botón no depende de validateForm en tiempo real (evita falsos positivos); la validación corre al enviar. */
  const addBlockDisabledReason = useMemo(() => {
    if (catalogLoading) return "Cargando catálogos de agenda…";
    if (paseadorId == null) {
      return "Inicia sesión como paseador; falta el id en sesión (vuelve a entrar si ya estabas conectado).";
    }
    if (catalogError) return catalogError;
    if (!estadosBloque.length) {
      return "No hay estados de bloque en el servidor. Revisa la tabla estado_bloque o GET /api/agenda/estados-bloque.";
    }
    if (!diasSemana.length) {
      return "No hay días de la semana en el servidor. Revisa GET /api/agenda/dias-semana.";
    }
    if (idEstadoDisponible == null) {
      return "Los estados de bloque no tienen idEstado válido en la respuesta JSON.";
    }
    return null;
  }, [
    catalogLoading,
    paseadorId,
    catalogError,
    estadosBloque.length,
    diasSemana.length,
    idEstadoDisponible
  ]);

  const isAddDisabled = saving || addBlockDisabledReason != null;

  const showToast = (next: AgendaToast) => setToast(next);
  const dismissToast = () => setToast(null);

  const goPrevWeek = () => {
    if (!canGoPrevWeek) return;
    setWeekMonday((w) => addDays(w, -7));
  };

  const goNextWeek = () => {
    if (!canGoNextWeek) return;
    setWeekMonday((w) => addDays(w, 7));
  };

  const openAddModal = () => {
    setRepeatMismoDiaEnMes(false);
    setForm({
      fecha: selectedISODate,
      startTime: "10:00",
      endTime: "11:00"
    });
    setFormErrors({});
    setIsAddModalOpen(true);
  };

  const closeAddModal = () => {
    setFormErrors({});
    setIsAddModalOpen(false);
  };

  const updateFormField = <K extends keyof AgendaApiForm>(name: K, value: AgendaApiForm[K]) => {
    setForm((prev) => ({ ...prev, [name]: value }));
    setFormErrors((prev) => ({ ...prev, [name]: undefined }));
  };

  const handleFieldBlur = <K extends keyof AgendaApiForm>(name: K) => {
    const errs = validateForm(form);
    setFormErrors((prev) => ({ ...prev, [name]: errs[name] }));
  };

  const handleAddBlock = async () => {
    const validationErrors = validateForm(form);
    setFormErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) return;

    if (paseadorId == null) {
      showToast({
        type: "error",
        title: "Sesión",
        message: "No hay id de paseador. Inicia sesión de nuevo."
      });
      return;
    }
    if (idEstadoDisponible == null) {
      showToast({
        type: "error",
        title: "Catálogo",
        message: "No se pudo determinar el estado disponible. Revisa estados-bloque en el servidor."
      });
      return;
    }
    let idDiaParaUnSolo: number | null = null;
    if (!repeatMismoDiaEnMes) {
      idDiaParaUnSolo = findDiaId(diasSemana, form.fecha);
      if (idDiaParaUnSolo == null) {
        showToast({
          type: "error",
          title: "Catálogo",
          message: "No se pudo asignar el día de la semana. Revisa días-semana en el servidor."
        });
        return;
      }
      if (overlapsApiBlocks(bloques, form.fecha, form.startTime, form.endTime)) {
        showToast({
          type: "error",
          title: "Conflicto de horario",
          message: "Ya tienes un bloque que se superpone en esa fecha."
        });
        return;
      }
    }

    setSaving(true);
    try {
      if (repeatMismoDiaEnMes) {
        const res = await crearSerieMensualBloques({
          idUsuario: paseadorId,
          fechaSemilla: form.fecha,
          horaInicio: `${form.fecha}T${form.startTime}:00`,
          horaFinal: `${form.fecha}T${form.endTime}:00`,
          estadoBloque: { idEstado: idEstadoDisponible }
        });
        await refreshBloques();
        closeAddModal();
        showToast({
          type: "success",
          title: "Serie mensual",
          message: `Creados: ${res.creados}. Omitidos (fecha pasada): ${res.omitidosPasado}. Omitidos (solape): ${res.omitidosSolape}.`
        });
      } else {
        await crearBloque({
          idUsuario: paseadorId,
          fecha: form.fecha,
          horaInicio: `${form.fecha}T${form.startTime}:00`,
          horaFinal: `${form.fecha}T${form.endTime}:00`,
          estadoBloque: { idEstado: idEstadoDisponible },
          diaSemana: { idDia: idDiaParaUnSolo as number }
        });
        await refreshBloques();
        closeAddModal();
        showToast({
          type: "success",
          title: "Bloque creado",
          message: "El bloque quedó guardado en el servidor."
        });
      }
    } catch (e) {
      showToast({
        type: "error",
        title: "No se pudo crear",
        message: e instanceof Error ? e.message : "Error al crear el bloque."
      });
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteBlock = async (idAgenda: number) => {
    const target = bloques.find((b) => b.idAgenda === idAgenda);
    if (!target) return;
    if (target.estadoBloque.nombre.trim().toLowerCase() === "reservado") {
      showToast({
        type: "error",
        title: "Bloque protegido",
        message: "No puedes eliminar un bloque reservado."
      });
      return;
    }
    setSaving(true);
    try {
      await eliminarBloque(idAgenda);
      await refreshBloques();
      showToast({
        type: "success",
        title: "Bloque eliminado",
        message: "El bloque se eliminó del servidor."
      });
    } catch (e) {
      showToast({
        type: "error",
        title: "No se pudo eliminar",
        message: e instanceof Error ? e.message : "Error al eliminar."
      });
    } finally {
      setSaving(false);
    }
  };

  const textoEstadoServidor = () => {
    if (bloquesLoading) return "Cargando bloques…";
    if (bloquesError) return bloquesError;
    if (bloques.length === 0) return "Sin bloques en el servidor.";
    return `${bloques.length} bloque${bloques.length === 1 ? "" : "s"} guardados`;
  };

  return {
    paseadorId,
    weekDays,
    weekMonday,
    goPrevWeek,
    goNextWeek,
    canGoPrevWeek,
    canGoNextWeek,
    selectedISODate,
    setSelectedISODate,
    selectedDayLabel,
    hourSlots: HOUR_SLOTS,
    allBlocks: bloques,
    selectedDayBlocks,
    isAddModalOpen,
    form,
    formErrors,
    isAddDisabled,
    addBlockDisabledReason,
    repeatMismoDiaEnMes,
    setRepeatMismoDiaEnMes,
    saving,
    toast,
    openAddModal,
    closeAddModal,
    dismissToast,
    updateFormField,
    handleFieldBlur,
    handleAddBlock,
    handleDeleteBlock,
    textoEstadoServidor,
    catalogLoading,
    catalogError,
    refreshBloques
  };
}
