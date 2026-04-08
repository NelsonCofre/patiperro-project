// Validaciones del formulario de bloques de agenda.
// En esta etapa cubren coherencia horaria y conflicto local para preparar la integracion backend.
import type {
  AgendaBlock,
  AgendaBlockForm,
  AgendaBlockFormErrors
} from "../types/agenda.types";

function timeToMinutes(value: string): number {
  const [hours, minutes] = value.split(":").map(Number);
  return hours * 60 + minutes;
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

    if (block.day !== candidate.day) {
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

  if (!form.day) {
    errors.day = "Selecciona un dia para el bloque.";
  }

  if (!form.startTime) {
    errors.startTime = "Selecciona una hora de inicio.";
  }

  if (!form.endTime) {
    errors.endTime = "Selecciona una hora de termino.";
  }

  if (form.startTime && form.endTime) {
    if (timeToMinutes(form.endTime) <= timeToMinutes(form.startTime)) {
      errors.endTime = "La hora de termino debe ser posterior a la hora de inicio";
    }
  }

  return errors;
}
