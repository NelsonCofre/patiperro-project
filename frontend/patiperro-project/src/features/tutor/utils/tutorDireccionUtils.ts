import type { TutorDireccionApi } from "../services/tutorCercanosApi";
import type { ReservaTutorDetalleDTO } from "../types/reservaTutor.types";
import { tieneDireccionEncuentro } from "./reservaEstadoUtils";

export function direccionDesdePerfilTutor(direccion?: TutorDireccionApi | null): {
  comuna: string | null;
  direccionReferencia: string | null;
} {
  if (!direccion) {
    return { comuna: null, direccionReferencia: null };
  }

  const comuna = direccion.comuna?.trim() || null;
  const partes: string[] = [];

  if (direccion.calle?.trim()) {
    partes.push(direccion.calle.trim());
  }
  if (direccion.numeracion != null && Number.isFinite(direccion.numeracion)) {
    partes.push(String(direccion.numeracion));
  }
  if (direccion.casaDepartamento?.trim()) {
    partes.push(direccion.casaDepartamento.trim());
  }
  if (direccion.ciudad?.trim()) {
    partes.push(direccion.ciudad.trim());
  }

  let direccionReferencia = partes.length > 0 ? partes.join(", ") : null;

  if (!direccionReferencia) {
    const lat = direccion.latitud;
    const lon = direccion.longitud;
    if (typeof lat === "number" && Number.isFinite(lat) && typeof lon === "number" && Number.isFinite(lon)) {
      direccionReferencia = `Coordenadas: ${lat.toFixed(5)}, ${lon.toFixed(5)}`;
    }
  }

  return { comuna, direccionReferencia };
}

export function aplicarDireccionTutorAReserva(
  reserva: ReservaTutorDetalleDTO,
  direccion?: TutorDireccionApi | null
): ReservaTutorDetalleDTO {
  if (tieneDireccionEncuentro(reserva.comuna, reserva.direccionReferencia)) {
    return reserva;
  }

  const parsed = direccionDesdePerfilTutor(direccion);
  if (!parsed.comuna && !parsed.direccionReferencia) {
    return reserva;
  }

  return {
    ...reserva,
    comuna: reserva.comuna?.trim() || parsed.comuna,
    direccionReferencia: reserva.direccionReferencia?.trim() || parsed.direccionReferencia
  };
}
