import type {
  DecisionSolicitudPayload,
  SolicitudPendientePaseador
} from "../types/solicitudPaseador.types";

const MOCK_DELAY_MS = 850;

const MOCK_SOLICITUDES: SolicitudPendientePaseador[] = [
  {
    idReserva: 101,
    tutorNombre: "Camila Rojas",
    tutorTelefono: "+56 9 6123 4578",
    tutorCorreo: "camila.rojas@mail.com",
    tutorComuna: "Providencia",
    tutorDireccion: "Cercano a Plaza Las Lilas",
    tutorFotoUrl:
      "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=700&q=80",
    tutorNotas: "Prefiere recibir actualizaciones al inicio y al finalizar el paseo.",
    mascotaNombre: "Luna",
    mascotaFotoUrl:
      "https://images.unsplash.com/photo-1587300003388-59208cc962cb?auto=format&fit=crop&w=900&q=80",
    mascotaRaza: "Mestiza",
    mascotaTamano: "Mediano",
    mascotaEdad: "3 años",
    mascotaPeso: "18 kg",
    mascotaSexo: "Hembra",
    mascotaCaracter: "Tranquila, curiosa y algo sensible al ruido.",
    mascotaCuidados: "Evitar calles con motos y mantener correa corta en cruces.",
    fecha: "2026-04-18",
    horaInicio: "10:00",
    horaFin: "11:00",
    comuna: "Providencia",
    direccionReferencia: "Cercano a Plaza Las Lilas",
    montoTotal: 9500,
    estado: "Solicitada",
    comentarioTutor: "Luna se asusta con motos, prefiere caminar por calles tranquilas.",
    fechaSolicitud: "2026-04-15T09:24:00"
  },
  {
    idReserva: 102,
    tutorNombre: "Diego Morales",
    tutorTelefono: "+56 9 7344 2091",
    tutorCorreo: "diego.morales@mail.com",
    tutorComuna: "Ñuñoa",
    tutorDireccion: "Sector Parque Juan XXIII",
    tutorFotoUrl:
      "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=700&q=80",
    tutorNotas: "Rocky tiene buena energía, pero no debe soltarse en parques abiertos.",
    mascotaNombre: "Rocky",
    mascotaFotoUrl:
      "https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&w=900&q=80",
    mascotaRaza: "Labrador",
    mascotaTamano: "Grande",
    mascotaEdad: "5 años",
    mascotaPeso: "31 kg",
    mascotaSexo: "Macho",
    mascotaCaracter: "Activo, sociable y con mucha energía.",
    mascotaCuidados: "No soltar en parques abiertos y llevar agua para el recorrido.",
    fecha: "2026-04-18",
    horaInicio: "16:00",
    horaFin: "17:30",
    comuna: "Ñuñoa",
    direccionReferencia: "Sector Parque Juan XXIII",
    montoTotal: 17250,
    estado: "Solicitada",
    comentarioTutor: "Rocky tiene mucha energía y responde bien con correa corta.",
    fechaSolicitud: "2026-04-15T11:10:00"
  },
  {
    idReserva: 103,
    tutorNombre: "Sofía Herrera",
    tutorTelefono: "+56 9 8455 6720",
    tutorCorreo: "sofia.herrera@mail.com",
    tutorComuna: "Santiago Centro",
    tutorDireccion: "Edificio con conserjería",
    tutorFotoUrl:
      "https://images.unsplash.com/photo-1531123897727-8f129e1688ce?auto=format&fit=crop&w=700&q=80",
    tutorNotas: "Milo suele caminar lento durante los primeros minutos.",
    mascotaNombre: "Milo",
    mascotaFotoUrl:
      "https://images.unsplash.com/photo-1517849845537-4d257902454a?auto=format&fit=crop&w=900&q=80",
    mascotaRaza: "Poodle",
    mascotaTamano: "Pequeño",
    mascotaEdad: "2 años",
    mascotaPeso: "7 kg",
    mascotaSexo: "Macho",
    mascotaCaracter: "Cariñoso, observador y camina a ritmo pausado.",
    mascotaCuidados: "Darle unos minutos para tomar confianza antes de caminar rápido.",
    fecha: "2026-04-19",
    horaInicio: "09:00",
    horaFin: "10:00",
    comuna: "Santiago Centro",
    direccionReferencia: "Edificio con conserjeria",
    montoTotal: 8000,
    estado: "Solicitada",
    fechaSolicitud: "2026-04-15T12:42:00"
  }
];

function wait(ms: number): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

export async function fetchSolicitudesPendientesPaseador(): Promise<SolicitudPendientePaseador[]> {
  await wait(MOCK_DELAY_MS);
  return MOCK_SOLICITUDES.map((solicitud) => ({ ...solicitud }));
}

export async function responderSolicitudPaseador(
  idReserva: number,
  payload: DecisionSolicitudPayload
): Promise<{ idReserva: number; estado: "Aceptada" | "Rechazada" }> {
  await wait(MOCK_DELAY_MS);

  if (!Number.isFinite(idReserva) || idReserva <= 0) {
    throw new Error("La solicitud seleccionada no es válida.");
  }

  if (payload.decision === "ACEPTAR") {
    return { idReserva, estado: "Aceptada" };
  }

  return { idReserva, estado: "Rechazada" };

  /*
   * Integración futura con backend:
   *
   * await fetch(API_ENDPOINTS.reserva.status(idReserva), {
   *   method: "PATCH",
   *   credentials: "include",
   *   headers: { ...bearerAuthHeaders(), "Content-Type": "application/json" },
   *   body: JSON.stringify(payload)
   * });
   *
   * Cuando backend soporte motivo de rechazo, enviar:
   * { decision: "RECHAZAR", motivoRechazo, detalleRechazo }
   */
}
