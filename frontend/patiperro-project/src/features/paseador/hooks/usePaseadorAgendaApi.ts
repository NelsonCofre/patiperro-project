// Agenda del paseador integrada con el microservicio: semana navegable + CRUD de bloques.
// Extiende la vista con bloqueo local de dias/rangos, preparado para enviar fecha_inicio y fecha_fin.
import { useCallback, useEffect, useMemo, useState } from "react";
import type {
  AgendaDay,
  AgendaBlockedRange,
  AgendaBlockRangeErrors,
  AgendaBlockRangeForm,
  AgendaDateOption,
  AgendaToast
} from "../types/agenda.types";
import {
  crearBloqueoDia,
  crearBloque,
  crearSerieMensualBloques,
  desbloquearDiaPorUsuario,
  eliminarBloque,
  fetchBloqueosDiaPorUsuario,
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
import {
  getTodayIsoDate,
  hasBookedBlockInRange,
  isDateInBlockedRanges,
  validateBlockRangeForm
} from "../utils/agendaValidators";

const HOUR_SLOTS = [
  "06:00",
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
  "21:00",
  "22:00"
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

function buildIsoDatesInRange(fechaInicio: string, fechaFin: string): string[] {
  const out: string[] = [];
  const start = new Date(`${fechaInicio}T12:00:00`);
  const end = new Date(`${fechaFin}T12:00:00`);
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || start > end) {
    return out;
  }
  const cursor = new Date(start);
  while (cursor <= end) {
    out.push(toISODateLocal(cursor));
    cursor.setDate(cursor.getDate() + 1);
  }
  return out;
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
  if (form.fecha && form.fecha < getTodayIsoDate()) {
    errors.fecha = "No puedes bloquear fechas pasadas";
  }
  if (form.startTime && form.endTime && timeToMinutes(form.endTime) <= timeToMinutes(form.startTime)) {
    errors.endTime = "La hora de término debe ser posterior a la hora de inicio";
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
  fecha: string;
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
    fecha: dto.fecha,
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

const INITIAL_BLOCK_RANGE_FORM: AgendaBlockRangeForm = {
  fecha_inicio: toISODateLocal(new Date()),
  fecha_fin: toISODateLocal(new Date())
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
  const [isBlockDaysModalOpen, setIsBlockDaysModalOpen] = useState(false);
  const [blockRangeForm, setBlockRangeForm] = useState<AgendaBlockRangeForm>(
    INITIAL_BLOCK_RANGE_FORM
  );
  const [blockRangeErrors, setBlockRangeErrors] = useState<AgendaBlockRangeErrors>({});
  const [blockedRanges, setBlockedRanges] = useState<AgendaBlockedRange[]>([]);
  const [toast, setToast] = useState<AgendaToast | null>(null);
  const [saving, setSaving] = useState(false);
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

  const refreshBloqueos = useCallback(async () => {
    if (paseadorId == null) {
      setBlockedRanges([]);
      return;
    }
    try {
      const bloqueos = await fetchBloqueosDiaPorUsuario(paseadorId);
      setBlockedRanges(
        bloqueos.map((b) => ({
          id: `blocked-${b.idBloqueo}`,
          fecha_inicio: b.fecha,
          fecha_fin: b.fecha,
          estadoBloqueId: null
        }))
      );
    } catch (e) {
      showToast({
        type: "error",
        title: "Bloqueos",
        message: e instanceof Error ? e.message : "No se pudieron cargar los dias bloqueados."
      });
    }
  }, [paseadorId]);

  useEffect(() => {
    refreshBloques();
    refreshBloqueos();
  }, [refreshBloques, refreshBloqueos]);

  useEffect(() => {
    if (paseadorId == null) {
      setCatalogLoading(false);
      setCatalogError("Inicia sesión como paseador para continuar.");
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
          setCatalogError(e instanceof Error ? e.message : "No se pudo cargar la agenda. Intenta de nuevo.");
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
  const selectedDateBlocked = useMemo(
    () => isDateInBlockedRanges(selectedISODate, blockedRanges),
    [selectedISODate, blockedRanges]
  );

  const bloquesDelDia = useMemo(() => {
    if (selectedDateBlocked) {
      return [];
    }

    return bloques.filter((b) => b.fecha === selectedISODate);
  }, [bloques, selectedISODate, selectedDateBlocked]);

  const selectedDayBlocks = useMemo(() => bloquesDelDia.map(dtoToTimelineView), [bloquesDelDia]);

  const selectedDayLabel = useMemo(
    () => weekdayLabelFromISODate(selectedISODate),
    [selectedISODate]
  );

  const visibleWeekDays = useMemo(
    () =>
      weekDays.map(
        (cell): AgendaDateOption & { isToday: boolean; isBlocked: boolean } => ({
          isoDate: cell.iso,
          dayLabel: weekdayLabelFromISODate(cell.iso) as AgendaDay,
          dayNumber: cell.dayNum,
          monthLabel: cell.date.toLocaleDateString("es-CL", { month: "short" }),
          isToday: cell.isToday,
          isBlocked: isDateInBlockedRanges(cell.iso, blockedRanges)
        })
      ),
    [weekDays, blockedRanges]
  );

  const addBlockDisabledReason = useMemo(() => {
    if (selectedDateBlocked) {
      return "El día seleccionado está bloqueado por motivos personales.";
    }
    if (catalogLoading) return "Cargando tu agenda...";
    if (paseadorId == null) {
      return "Inicia sesión como paseador para continuar.";
    }
    if (catalogError) return catalogError;
    if (!estadosBloque.length) {
      return "No se pudo cargar la configuración de bloques. Intenta de nuevo.";
    }
    if (!diasSemana.length) {
      return "No se pudo cargar el calendario. Intenta de nuevo.";
    }
    if (idEstadoDisponible == null) {
      return "No se pudo preparar la agenda. Intenta de nuevo.";
    }
    return null;
  }, [
    selectedDateBlocked,
    catalogLoading,
    paseadorId,
    catalogError,
    estadosBloque.length,
    diasSemana.length,
    idEstadoDisponible
  ]);

  const isAddDisabled = saving || addBlockDisabledReason != null;
  const isBlockDaysDisabled =
    saving || Object.keys(validateBlockRangeForm(blockRangeForm)).length > 0;

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
    if (selectedDateBlocked) {
      showToast({
        type: "info",
        title: "Dia bloqueado",
        message: "Quita el bloqueo del día antes de crear nuevos bloques de disponibilidad."
      });
      return;
    }

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

  const openBlockDaysModal = () => {
    setBlockRangeForm({
      fecha_inicio: selectedISODate,
      fecha_fin: selectedISODate
    });
    setBlockRangeErrors({});
    setIsBlockDaysModalOpen(true);
  };

  const closeBlockDaysModal = () => {
    setBlockRangeErrors({});
    setIsBlockDaysModalOpen(false);
  };

  const updateFormField = <K extends keyof AgendaApiForm>(name: K, value: AgendaApiForm[K]) => {
    setForm((prev) => ({ ...prev, [name]: value }));
    setFormErrors((prev) => ({ ...prev, [name]: undefined }));
  };

  const updateBlockRangeField = <K extends keyof AgendaBlockRangeForm>(
    name: K,
    value: AgendaBlockRangeForm[K]
  ) => {
    setBlockRangeForm((prev) => ({ ...prev, [name]: value }));
    setBlockRangeErrors((prev) => ({ ...prev, [name]: undefined }));
  };

  const handleFieldBlur = <K extends keyof AgendaApiForm>(name: K) => {
    const errs = validateForm(form);
    setFormErrors((prev) => ({ ...prev, [name]: errs[name] }));
  };

  const handleBlockRangeBlur = <K extends keyof AgendaBlockRangeForm>(name: K) => {
    const errs = validateBlockRangeForm(blockRangeForm);
    setBlockRangeErrors((prev) => ({ ...prev, [name]: errs[name] }));
  };

  const handleAddBlock = async () => {
    const validationErrors = validateForm(form);
    setFormErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) return;

    if (selectedDateBlocked) {
      showToast({
        type: "error",
        title: "Dia no disponible",
        message: "El día seleccionado está bloqueado por motivos personales."
      });
      return;
    }

    if (paseadorId == null) {
      showToast({
        type: "error",
        title: "Sesión",
        message: "Inicia sesión de nuevo para continuar."
      });
      return;
    }
    if (idEstadoDisponible == null) {
      showToast({
        type: "error",
        title: "Agenda",
        message: "No se pudo determinar el estado disponible."
      });
      return;
    }
    let idDiaParaUnSolo: number | null = null;
    if (!repeatMismoDiaEnMes) {
      idDiaParaUnSolo = findDiaId(diasSemana, form.fecha);
      if (idDiaParaUnSolo == null) {
        showToast({
          type: "error",
          title: "Agenda",
          message: "No se pudo asignar el día de la semana."
        });
        return;
      }
      if (overlapsApiBlocks(bloques, form.fecha, form.startTime, form.endTime)) {
        showToast({
          type: "error",
          title: "Conflicto de horario",
          message: "Ya tienes un bloque de disponibilidad que coincide con este horario"
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
          title: "Bloques creados",
          message:
            res.creados > 0
              ? `Se crearon ${res.creados} bloque(s) para el resto del mes.${
                  res.omitidosPasado + res.omitidosSolape > 0
                    ? ` ${res.omitidosPasado + res.omitidosSolape} fecha(s) se omitieron por estar en el pasado o solaparse con bloques existentes.`
                    : ""
                }`
              : "No se crearon bloques nuevos; las fechas restantes del mes ya estaban ocupadas o en el pasado."
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
          message: "Tu bloque de disponibilidad quedó guardado."
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

  const handleBlockDays = async () => {
    const validationErrors = validateBlockRangeForm(blockRangeForm);
    setBlockRangeErrors(validationErrors);

    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    if (hasBookedBlockInRange(bloques.map(dtoToTimelineView), blockRangeForm)) {
      showToast({
        type: "error",
        title: "Conflicto con paseos",
        message:
          "Tienes paseos programados para este dia. Debes gestionarlos individualmente antes de bloquear la fecha completa"
      });
      return;
    }

    if (paseadorId == null) {
      showToast({
        type: "error",
        title: "Sesión",
        message: "Inicia sesión de nuevo para continuar."
      });
      return;
    }

    setSaving(true);
    try {
      const fechas = buildIsoDatesInRange(blockRangeForm.fecha_inicio, blockRangeForm.fecha_fin);
      const yaBloqueadas = new Set(
        blockedRanges.flatMap((r) => buildIsoDatesInRange(r.fecha_inicio, r.fecha_fin))
      );
      let creadas = 0;
      let omitidas = 0;
      for (const fecha of fechas) {
        if (yaBloqueadas.has(fecha)) {
          omitidas += 1;
          continue;
        }
        try {
          await crearBloqueoDia({
            idUsuario: paseadorId,
            fecha,
            motivo: "Bloqueo personal de disponibilidad"
          });
          creadas += 1;
        } catch (e) {
          const msg = e instanceof Error ? e.message : "";
          if (msg.toLowerCase().includes("ya existe")) {
            omitidas += 1;
            continue;
          }
          throw e;
        }
      }
      await refreshBloqueos();
      closeBlockDaysModal();
      showToast({
        type: "success",
        title: "Bloqueo aplicado",
        message:
          creadas > 0
            ? `Se bloquearon ${creadas} dia(s)${omitidas > 0 ? `. ${omitidas} ya estaban bloqueados.` : "."}`
            : "No se crearon nuevos bloqueos; las fechas seleccionadas ya estaban bloqueadas."
      });
    } catch (e) {
      showToast({
        type: "error",
        title: "No se pudo bloquear",
        message: e instanceof Error ? e.message : "Error al bloquear dias."
      });
    } finally {
      setSaving(false);
    }
  };

  const handleUnblockSelectedDate = async () => {
    const hadBlockedRange = isDateInBlockedRanges(selectedISODate, blockedRanges);

    if (!hadBlockedRange) {
      showToast({
        type: "info",
        title: "Sin bloqueo",
        message: "El día seleccionado no tiene un bloqueo activo."
      });
      return;
    }

    if (paseadorId == null) {
      showToast({
        type: "error",
        title: "Sesión",
        message: "Inicia sesión de nuevo para continuar."
      });
      return;
    }

    setSaving(true);
    try {
      await desbloquearDiaPorUsuario(paseadorId, selectedISODate);
      await refreshBloqueos();
      showToast({
        type: "success",
        title: "Dia habilitado",
        message: "Se quito el bloqueo y la agenda vuelve a mostrarse como disponible."
      });
    } catch (e) {
      showToast({
        type: "error",
        title: "No se pudo desbloquear",
        message: e instanceof Error ? e.message : "Error al desbloquear el dia."
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
        message:
          "No puedes modificar este bloque porque ya tienes un paseo programado. Debes gestionar la cancelación del servicio primero"
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
        message: "El bloque se elimino de tu agenda."
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
    if (bloquesLoading) return "Cargando bloques...";
    if (bloquesError) return bloquesError;
    if (bloques.length === 0) return "Sin bloques en tu agenda.";
    return `${bloques.length} bloque${bloques.length === 1 ? "" : "s"} guardados`;
  };

  return {
    paseadorId,
    weekDays: visibleWeekDays,
    weekMonday,
    goPrevWeek,
    goNextWeek,
    canGoPrevWeek,
    canGoNextWeek,
    selectedISODate,
    setSelectedISODate,
    selectedDayLabel,
    selectedDateBlocked,
    blockedRanges,
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
    isBlockDaysModalOpen,
    blockRangeForm,
    blockRangeErrors,
    isBlockDaysDisabled,
    openAddModal,
    closeAddModal,
    openBlockDaysModal,
    closeBlockDaysModal,
    dismissToast,
    updateFormField,
    updateBlockRangeField,
    handleFieldBlur,
    handleBlockRangeBlur,
    handleAddBlock,
    handleBlockDays,
    handleDeleteBlock,
    handleUnblockSelectedDate,
    textoEstadoServidor,
    catalogLoading,
    catalogError,
    refreshBloques
  };
}
