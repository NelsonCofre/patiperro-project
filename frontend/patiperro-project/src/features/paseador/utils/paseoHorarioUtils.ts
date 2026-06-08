/**
 * Instante local del bloque (fecha YYYY-MM-DD + hora HH:mm o HH:mm:ss).
 */
export function parsePaseoInstanteLocal(fecha: string, hora: string): Date | null {
  const d = fecha?.trim();
  const h = hora?.trim();
  if (!d || !h) return null;
  const timePart = h.length <= 5 ? `${h}:00` : h;
  const parsed = new Date(`${d}T${timePart}`);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

/**
 * Fin del bloque de agenda en hora local del navegador (fecha YYYY-MM-DD + hora HH:mm o HH:mm:ss).
 */
export function parseFinPaseoLocal(fecha: string, horaFin: string): Date | null {
  return parsePaseoInstanteLocal(fecha, horaFin);
}

/** {@code true} si ya pasó el horario de fin del paseo según agenda (para habilitar "Finalizar"). */
export function yaPasoHorarioFinPaseo(fecha: string, horaFin: string): boolean {
  const end = parseFinPaseoLocal(fecha, horaFin);
  if (!end) return true;
  return Date.now() >= end.getTime();
}

type SolicitudHorarioRef = {
  idReserva: number;
  fecha: string;
  horaInicio: string;
  horaFin: string;
};

/**
 * Reserva cuyo horario está más cerca de ahora (en curso, próximo o el más reciente).
 */
export function findProximaSolicitudId(solicitudes: SolicitudHorarioRef[]): number | null {
  if (solicitudes.length <= 1) {
    return null;
  }

  const now = Date.now();

  const enVentana = solicitudes
    .map((solicitud) => {
      const inicio = parsePaseoInstanteLocal(solicitud.fecha, solicitud.horaInicio);
      const fin = parsePaseoInstanteLocal(solicitud.fecha, solicitud.horaFin);
      return { solicitud, inicioMs: inicio?.getTime(), finMs: fin?.getTime() };
    })
    .filter(
      ({ inicioMs, finMs }) =>
        inicioMs != null &&
        finMs != null &&
        Number.isFinite(inicioMs) &&
        Number.isFinite(finMs) &&
        inicioMs <= now &&
        now <= finMs
    )
    .sort((a, b) => (a.finMs ?? 0) - (b.finMs ?? 0));

  if (enVentana.length > 0) {
    return enVentana[0].solicitud.idReserva;
  }

  const proximas = solicitudes
    .map((solicitud) => {
      const inicio = parsePaseoInstanteLocal(solicitud.fecha, solicitud.horaInicio);
      return { solicitud, inicioMs: inicio?.getTime() };
    })
    .filter(({ inicioMs }) => inicioMs != null && Number.isFinite(inicioMs) && inicioMs >= now)
    .sort((a, b) => (a.inicioMs ?? 0) - (b.inicioMs ?? 0));

  if (proximas.length > 0) {
    return proximas[0].solicitud.idReserva;
  }

  let mejorId: number | null = null;
  let mejorDistancia = Number.POSITIVE_INFINITY;

  for (const solicitud of solicitudes) {
    const inicio = parsePaseoInstanteLocal(solicitud.fecha, solicitud.horaInicio);
    if (!inicio) continue;
    const distancia = Math.abs(inicio.getTime() - now);
    if (distancia < mejorDistancia) {
      mejorDistancia = distancia;
      mejorId = solicitud.idReserva;
    }
  }

  return mejorId;
}
