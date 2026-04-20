export type DecisionSolicitud = "ACEPTAR" | "RECHAZAR";

export type MotivoRechazo =
  | "Emergencia personal"
  | "Mascota no compatible"
  | "Horario no disponible"
  | "Otro";

export type RechazoSolicitudForm = {
  motivo: MotivoRechazo | "";
  detalle: string;
};

export type SolicitudPendientePaseador = {
  idReserva: number;
  tutorNombre: string;
  tutorTelefono: string;
  tutorCorreo: string;
  tutorComuna: string;
  tutorDireccion: string;
  tutorFotoUrl: string;
  tutorNotas?: string;
  mascotaNombre: string;
  mascotaFotoUrl: string;
  mascotaRaza: string;
  mascotaTamano: string;
  mascotaEdad: string;
  mascotaPeso: string;
  mascotaSexo: string;
  mascotaCaracter: string;
  mascotaCuidados: string;
  fecha: string;
  horaInicio: string;
  horaFin: string;
  comuna: string;
  direccionReferencia: string;
  montoTotal: number;
  estado: "Solicitada" | "Aceptada" | "Rechazada";
  codigoEncuentro?: number | null;
  comentarioTutor?: string;
  fechaSolicitud: string;
};

export type DecisionSolicitudPayload = {
  decision: DecisionSolicitud;
  motivoRechazo?: MotivoRechazo;
  detalleRechazo?: string;
};
