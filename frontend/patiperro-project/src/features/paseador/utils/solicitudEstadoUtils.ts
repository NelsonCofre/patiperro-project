import type { SolicitudPendientePaseador } from "../types/solicitudPaseador.types";

export type SolicitudEstadoMeta = {
  key: string;
  label: string;
  helper: string;
  className: string;
};

function normalizeEstado(estado: SolicitudPendientePaseador["estado"]): string {
  return estado
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/\s+/g, "_");
}

export function getSolicitudEstadoMeta(estado: SolicitudPendientePaseador["estado"]): SolicitudEstadoMeta {
  const normalized = normalizeEstado(estado);

  if (normalized.includes("solicit")) {
    return {
      key: "solicitada",
      label: "Solicitada",
      helper: "Espera tu decisión",
      className: "statusSolicitada"
    };
  }

  if (normalized.includes("pendiente") && normalized.includes("pago")) {
    return {
      key: "pendiente_pago",
      label: "Pendiente de pago",
      helper: "El tutor aún debe completar el pago",
      className: "statusSolicitada"
    };
  }

  if (normalized.includes("pagad")) {
    return {
      key: "pagada",
      label: "Pagada",
      helper: "El tutor pagó; debes aceptar o rechazar",
      className: "statusPagada"
    };
  }

  if (normalized.includes("acept")) {
    return {
      key: "aceptada",
      label: "Aceptada",
      helper: "Valida el código para iniciar",
      className: "statusAceptada"
    };
  }

  if (normalized.includes("curso")) {
    return {
      key: "en_curso",
      label: "En curso",
      helper: "Paseo activo ahora",
      className: "statusEnCurso"
    };
  }

  if (normalized.includes("final")) {
    return {
      key: "finalizada",
      label: "Finalizada",
      helper: "Servicio completado",
      className: "statusFinalizada"
    };
  }

  if (normalized.includes("rechaz")) {
    return {
      key: "rechazada",
      label: "Rechazada",
      helper: "Decidiste no tomarla",
      className: "statusRechazada"
    };
  }

  if (normalized.includes("expir")) {
    return {
      key: "expirada",
      label: "Expirada",
      helper: "Plazo de aceptación agotado",
      className: "statusRechazada"
    };
  }

  if (normalized.includes("cancel")) {
    return {
      key: "cancelada",
      label: "Cancelada",
      helper: "El tutor canceló la solicitud",
      className: "statusRechazada"
    };
  }

  return {
    key: "desconocida",
    label: estado,
    helper: "Estado de la reserva",
    className: "statusDesconocida"
  };
}

export function esSolicitudPorResponder(estado: SolicitudPendientePaseador["estado"]): boolean {
  const key = getSolicitudEstadoMeta(estado).key;
  return key === "solicitada" || key === "pagada" || key === "pendiente_pago";
}
