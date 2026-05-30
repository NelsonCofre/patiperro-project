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
  estado:
    | "Solicitada"
    | "Aceptada"
    | "En Curso"
    | "Rechazada"
    | "Finalizada"
    | "Pagada"
    | "Disponible";
  codigoEncuentro?: number | null;
  comentarioTutor?: string;
  fechaSolicitud: string;
  // 👇 AGREGA ESTAS DOS LÍNEAS AQUÍ 👇
  latitud?: number;  // El '?' es por si alguna reserva vieja no las tiene
  longitud?: number;
  fechaInicioReal?: string | null;
  trackingActivo?: boolean;
  chatActivo?: boolean;
};

export type DecisionSolicitudPayload = {
  decision: DecisionSolicitud;
  motivoRechazo?: MotivoRechazo;
  detalleRechazo?: string;
};
