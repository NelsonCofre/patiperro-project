/**
 * Fin del bloque de agenda en hora local del navegador (fecha YYYY-MM-DD + hora HH:mm o HH:mm:ss).
 */
export function parseFinPaseoLocal(fecha: string, horaFin: string): Date | null {
  const d = fecha?.trim();
  const h = horaFin?.trim();
  if (!d || !h) return null;
  const timePart = h.length <= 5 ? `${h}:00` : h;
  const parsed = new Date(`${d}T${timePart}`);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

/** {@code true} si ya pasó el horario de fin del paseo según agenda (para habilitar "Finalizar"). */
export function yaPasoHorarioFinPaseo(fecha: string, horaFin: string): boolean {
  const end = parseFinPaseoLocal(fecha, horaFin);
  if (!end) return true;
  return Date.now() >= end.getTime();
}
