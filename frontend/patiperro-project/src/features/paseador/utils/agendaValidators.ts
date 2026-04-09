// Validaciones del formulario de bloques y bloqueos de agenda.
// Cubren coherencia horaria, conflicto local y restricciones de fechas futuras.
import type {
  AgendaBlock,
  AgendaBlockForm,
  AgendaBlockFormErrors,
  AgendaBlockRangeErrors,
  AgendaBlockRangeForm,
  AgendaBlockedRange
} from "../types/agenda.types";

function timeToMinutes(value: string): number {
  const [hours, minutes] = value.split(":").map(Number);
  return hours * 60 + minutes;
}

function normalizeDate(date: string): string {
  return new Date(`${date}T00:00:00`).toISOString().split("T")[0];
}

export function getTodayIsoDate(): string {
  return new Date().toISOString().split("T")[0];
}

export function hasOverlappingAgendaBlock(
  blocks: AgendaBlock[],
  candidate: AgendaBlockForm,
  excludeId?: string
): boolean {
  const candidateStart = timeToMinutes(candidate.startTime);
  const candidateEnd = timeToMinutes(candidate.endTime);

  return blocks.some((block) => {
    if (excludeId && block.id === excludeId) {
      return false;
    }

    if (block.fecha !== candidate.fecha) {
      return false;
    }

    const blockStart = timeToMinutes(block.startTime);
    const blockEnd = timeToMinutes(block.endTime);

    return candidateStart < blockEnd && candidateEnd > blockStart;
  });
}

export function validateAgendaBlockForm(
  form: AgendaBlockForm
): AgendaBlockFormErrors {
  const errors: AgendaBlockFormErrors = {};

  if (!form.fecha) {
    errors.fecha = "Selecciona una fecha para el bloque.";
  }

  if (!form.startTime) {
    errors.startTime = "Selecciona una hora de inicio.";
  }

  if (!form.endTime) {
    errors.endTime = "Selecciona una hora de termino.";
  }

  if (form.fecha && normalizeDate(form.fecha) < getTodayIsoDate()) {
    errors.fecha = "No puedes bloquear fechas pasadas";
  }

  if (form.startTime && form.endTime) {
    if (timeToMinutes(form.endTime) <= timeToMinutes(form.startTime)) {
      errors.endTime = "La hora de termino debe ser posterior a la hora de inicio";
    }
  }

  return errors;
}

export function validateBlockRangeForm(
  form: AgendaBlockRangeForm
): AgendaBlockRangeErrors {
  const errors: AgendaBlockRangeErrors = {};

  if (!form.fecha_inicio) {
    errors.fecha_inicio = "Selecciona la fecha de inicio.";
  }

  if (!form.fecha_fin) {
    errors.fecha_fin = "Selecciona la fecha de termino.";
  }

  if (form.fecha_inicio && normalizeDate(form.fecha_inicio) < getTodayIsoDate()) {
    errors.fecha_inicio = "No puedes bloquear fechas pasadas";
  }

  if (form.fecha_fin && normalizeDate(form.fecha_fin) < getTodayIsoDate()) {
    errors.fecha_fin = "No puedes bloquear fechas pasadas";
  }

  if (
    form.fecha_inicio &&
    form.fecha_fin &&
    normalizeDate(form.fecha_fin) < normalizeDate(form.fecha_inicio)
  ) {
    errors.fecha_fin = "La fecha de termino debe ser igual o posterior a la fecha de inicio.";
  }

  return errors;
}

export function isDateInBlockedRanges(
  date: string,
  ranges: AgendaBlockedRange[]
): boolean {
  const target = normalizeDate(date);

  return ranges.some((range) => {
    const start = normalizeDate(range.fecha_inicio);
    const end = normalizeDate(range.fecha_fin);
    return target >= start && target <= end;
  });
}

export function hasBookedBlockInRange(
  blocks: Array<Pick<AgendaBlock, "fecha" | "status">>,
  range: AgendaBlockRangeForm
): boolean {
  if (!range.fecha_inicio || !range.fecha_fin) {
    return false;
  }

  const start = normalizeDate(range.fecha_inicio);
  const end = normalizeDate(range.fecha_fin);

  return blocks.some((block) => {
    if (block.status !== "booked") {
      return false;
    }

    const blockDate = normalizeDate(block.fecha);
    return blockDate >= start && blockDate <= end;
  });
}
