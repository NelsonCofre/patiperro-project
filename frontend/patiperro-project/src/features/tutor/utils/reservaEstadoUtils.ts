import type { ReservaEstadoMeta, ReservaTutorDetalleDTO } from "../types/reservaTutor.types";

export type ReservaEstadoKey = ReservaEstadoMeta["key"];

export type TutorReservaFilterKey =
  | "solicitadas"
  | "aceptadas"
  | "en_curso"
  | "finalizadas"
  | "cerradas";

function normalizeEstadoName(nombre?: string | null): string {
  return (nombre ?? "")
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/\s+/g, "_");
}

export function getReservaEstadoMeta(
  reserva: Pick<ReservaTutorDetalleDTO, "idEstadoReserva" | "nombreEstado">
): ReservaEstadoMeta {
  const normalized = normalizeEstadoName(reserva.nombreEstado);
  const id = reserva.idEstadoReserva;

  if (id === 7 || normalized.includes("pendiente_pago") || normalized.includes("pending_payment")) {
    return {
      key: "pendiente_pago",
      label: "Pendiente de pago",
      helper: "Completa el checkout para confirmar la reserva",
      className: "statusPendientePago"
    };
  }

  if (id === 8 || (normalized.includes("pagad") && !normalized.includes("pendiente"))) {
    return {
      key: "pagada",
      label: "Pagada",
      helper: "Esperando respuesta del paseador",
      className: "statusPagada"
    };
  }

  if (id === 1 || normalized.includes("solicit")) {
    return {
      key: "solicitada",
      label: "Solicitada",
      helper: "Esperando respuesta del paseador",
      className: "statusSolicitada"
    };
  }

  if (id === 2 || normalized.includes("acept")) {
    return {
      key: "aceptada",
      label: "Aceptada",
      helper: "Lista para el encuentro o el paseo",
      className: "statusAceptada"
    };
  }

  if (id === 4 || normalized.includes("curso")) {
    return {
      key: "en_curso",
      label: "En curso",
      helper: "Paseo en progreso ahora",
      className: "statusEnCurso"
    };
  }

  if (id === 5 || normalized.includes("final")) {
    return {
      key: "finalizada",
      label: "Finalizada",
      helper: "Servicio completado",
      className: "statusFinalizada"
    };
  }

  if (id === 3 || normalized.includes("rechaz")) {
    return {
      key: "rechazada",
      label: "Rechazada",
      helper: "El paseador no aceptó la solicitud",
      className: "statusRechazada"
    };
  }

  if (id === 6 || normalized.includes("cancel")) {
    return {
      key: "cancelada",
      label: "Cancelada",
      helper: "Solicitud retirada",
      className: "statusRechazada"
    };
  }

  if (id === 9 || normalized.includes("expir")) {
    return {
      key: "expirada",
      label: "Expirada",
      helper: "La reserva vencio sin completarse",
      className: "statusExpirada"
    };
  }

  return {
    key: "desconocida",
    label: reserva.nombreEstado ?? "Sin estado",
    helper: "Estado no reconocido",
    className: "statusDesconocida"
  };
}

export function matchesTutorReservaFilter(
  filter: TutorReservaFilterKey,
  reserva: ReservaTutorDetalleDTO
): boolean {
  const key = getReservaEstadoMeta(reserva).key;

  switch (filter) {
    case "solicitadas":
      return key === "solicitada" || key === "pendiente_pago" || key === "pagada";
    case "aceptadas":
      return key === "aceptada";
    case "en_curso":
      return key === "en_curso";
    case "finalizadas":
      return key === "finalizada";
    case "cerradas":
      return key === "rechazada" || key === "cancelada" || key === "expirada";
    default:
      return true;
  }
}

export function isReservaSolicitada(reserva: ReservaTutorDetalleDTO): boolean {
  const key = getReservaEstadoMeta(reserva).key;
  return key === "solicitada" || key === "pendiente_pago" || key === "pagada";
}

export function isReservaFinalizada(reserva: ReservaTutorDetalleDTO): boolean {
  return getReservaEstadoMeta(reserva).key === "finalizada";
}

export function formatReservaDate(value?: string | null): string {
  if (!value) return "Fecha no disponible";
  const date = new Date(value.length === 10 ? `${value}T12:00:00` : value);
  if (Number.isNaN(date.getTime())) return "Fecha no disponible";
  return date.toLocaleDateString("es-CL", {
    day: "numeric",
    month: "long",
    year: "numeric"
  });
}

export function formatReservaTime(value?: string | null): string {
  if (!value) return "Sin hora";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value.slice(0, 5);
  return date.toLocaleTimeString("es-CL", {
    hour: "2-digit",
    minute: "2-digit"
  });
}

export function formatReservaMoney(value?: number | null): string {
  if (value == null || Number.isNaN(value)) return "Monto no informado";
  return new Intl.NumberFormat("es-CL", {
    style: "currency",
    currency: "CLP",
    maximumFractionDigits: 0
  }).format(value);
}

export function formatDireccionEncuentro(
  comuna?: string | null,
  direccionReferencia?: string | null
): string {
  const comunaLabel = comuna?.trim();
  const direccion = direccionReferencia?.trim();

  if (direccion && comunaLabel) {
    return `${direccion}, ${comunaLabel}`;
  }
  return direccion || comunaLabel || "Dirección no disponible";
}

export function tieneDireccionEncuentro(
  comuna?: string | null,
  direccionReferencia?: string | null
): boolean {
  return Boolean(comuna?.trim() || direccionReferencia?.trim());
}
