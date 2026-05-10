import { fetchSolicitudesPendientesPaseador } from "./solicitudesPaseadorService";
import type { SolicitudPendientePaseador } from "../types/solicitudPaseador.types";

export type BilleteraBucketKey = "retenido" | "verificacion" | "disponible";

export type BilleteraReservaItem = {
  idReserva: number;
  mascotaNombre: string;
  tutorNombre: string;
  fecha: string;
  horaInicio: string;
  montoBruto: number;
  comision: number;
  montoNeto: number;
  estado: string;
  fechaLiberacionEstimada?: string | null;
};

export type BilleteraBucket = {
  key: BilleteraBucketKey;
  title: string;
  helper: string;
  amount: number;
  grossAmount: number;
  commissionAmount: number;
  reservas: BilleteraReservaItem[];
};

export type BilleteraPaseadorData = {
  retenido: BilleteraBucket;
  verificacion: BilleteraBucket;
  disponible: BilleteraBucket;
  updatedAt: string;
};

const PLATFORM_COMMISSION_RATE = 0.05;

function normalizeState(value: string): string {
  return value
    .trim()
    .toUpperCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "");
}

function getBucketKey(estado: SolicitudPendientePaseador["estado"]): BilleteraBucketKey | null {
  const normalized = normalizeState(estado);

  if (normalized.includes("RECHAZ")) return null;
  if (normalized.includes("DISPON") || normalized.includes("LIBER")) return "disponible";
  if (normalized.includes("FINAL")) return "verificacion";
  if (
    normalized.includes("CURSO") ||
    normalized.includes("PAGAD") ||
    normalized.includes("ACEPT")
  ) {
    return "retenido";
  }
  return null;
}

function getSortTime(fecha: string, horaInicio: string): number {
  const iso = `${fecha}T${horaInicio || "00:00:00"}`;
  const value = new Date(iso).getTime();
  return Number.isFinite(value) ? value : 0;
}

function calcularFechaLiberacion(fechaBase: string): string | null {
  if (!fechaBase) return null;

  const fecha = new Date(`${fechaBase}T00:00:01`);
  if (Number.isNaN(fecha.getTime())) return null;

  fecha.setDate(fecha.getDate() + 2);
  fecha.setHours(0, 0, 1, 0);

  return fecha.toISOString();
}

function mapReservaItem(solicitud: SolicitudPendientePaseador): BilleteraReservaItem {
  const montoBruto = solicitud.montoTotal;
  const comision = Math.round(montoBruto * PLATFORM_COMMISSION_RATE);
  const montoNeto = Math.max(montoBruto - comision, 0);

  return {
    idReserva: solicitud.idReserva,
    mascotaNombre: solicitud.mascotaNombre,
    tutorNombre: solicitud.tutorNombre,
    fecha: solicitud.fecha,
    horaInicio: solicitud.horaInicio,
    montoBruto,
    comision,
    montoNeto,
    estado: solicitud.estado,
    fechaLiberacionEstimada: calcularFechaLiberacion(solicitud.fecha)
  };
}

function buildBucket(
  key: BilleteraBucketKey,
  title: string,
  helper: string,
  reservas: BilleteraReservaItem[]
): BilleteraBucket {
  const grossAmount = reservas.reduce((sum, reserva) => sum + reserva.montoBruto, 0);
  const commissionAmount = reservas.reduce((sum, reserva) => sum + reserva.comision, 0);

  return {
    key,
    title,
    helper,
    amount: reservas.reduce((sum, reserva) => sum + reserva.montoNeto, 0),
    grossAmount,
    commissionAmount,
    reservas: [...reservas].sort(
      (a, b) => getSortTime(b.fecha, b.horaInicio) - getSortTime(a.fecha, a.horaInicio)
    )
  };
}

export async function fetchBilleteraPaseador(): Promise<BilleteraPaseadorData> {
  const solicitudes = await fetchSolicitudesPendientesPaseador();

  const retenido: BilleteraReservaItem[] = [];
  const verificacion: BilleteraReservaItem[] = [];
  const disponible: BilleteraReservaItem[] = [];

  for (const solicitud of solicitudes) {
    const bucket = getBucketKey(solicitud.estado);
    if (!bucket) continue;
    const item = mapReservaItem(solicitud);
    if (bucket === "retenido") retenido.push(item);
    if (bucket === "verificacion") verificacion.push(item);
    if (bucket === "disponible") disponible.push(item);
  }

  return {
    retenido: buildBucket(
      "retenido",
      "Saldo Retenido",
      "Servicios pagados que aun no han finalizado o siguen en curso.",
      retenido
    ),
    verificacion: buildBucket(
      "verificacion",
      "Saldo en Verificacion",
      "Paseos finalizados que estan cumpliendo el periodo de liberacion N+2.",
      verificacion
    ),
    disponible: buildBucket(
      "disponible",
      "Saldo Disponible",
      "Fondos liberados y listos para retiro desde tu billetera.",
      disponible
    ),
    updatedAt: new Date().toISOString()
  };
}
