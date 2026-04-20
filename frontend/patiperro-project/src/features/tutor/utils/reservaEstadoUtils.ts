import type { ReservaEstadoMeta, ReservaTutorDetalleDTO } from "../types/reservaTutor.types";

function normalizeEstadoName(nombre?: string | null): string {
  return (nombre ?? "")
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/\s+/g, "_");
}

export function getReservaEstadoMeta(reserva: Pick<ReservaTutorDetalleDTO, "idEstadoReserva" | "nombreEstado">): ReservaEstadoMeta {
  const normalized = normalizeEstadoName(reserva.nombreEstado);
  const id = reserva.idEstadoReserva;

  if (id === 1 || normalized.includes("solicit")) {
    return {
      key: "solicitada",
      label: "Solicitada",
      helper: "Esperando respuesta",
      className: "statusSolicitada"
    };
  }

  if (id === 2 || normalized.includes("acept")) {
    return {
      key: "aceptada",
      label: "Aceptada",
      helper: "Pendiente de pago o ejecucion",
      className: "statusAceptada"
    };
  }

  if (id === 4 || normalized.includes("curso")) {
    return {
      key: "en_curso",
      label: "En Curso",
      helper: "Paseo realizandose",
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
      helper: "Solicitud no aceptada",
      className: "statusRechazada"
    };
  }

  if (id === 6 || normalized.includes("cancel")) {
    return {
      key: "cancelada",
      label: "Cancelada",
      helper: "Solicitud retirada por ti",
      className: "statusRechazada"
    };
  }

  return {
    key: "desconocida",
    label: reserva.nombreEstado ?? "Sin estado",
    helper: "Estado no reconocido",
    className: "statusDesconocida"
  };
}

export function isReservaSolicitada(reserva: ReservaTutorDetalleDTO): boolean {
  return getReservaEstadoMeta(reserva).key === "solicitada";
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
