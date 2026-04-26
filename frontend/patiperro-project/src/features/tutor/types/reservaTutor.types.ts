export type ReservaTutorDetalleDTO = {
  idReserva: number;
  idTutorUsuario: number;
  idMascota: number;
  mascotaNombre: string;
  idAgendaBloque: number;
  idPaseador: number | null;
  paseadorNombre: string;
  fecha: string | null;
  horaInicio: string | null;
  horaFinal: string | null;
  montoTotal: number | null;
  idPago: number | null;
  idEstadoReserva: number | null;
  nombreEstado: string | null;
  fechaSolicitud: string | null;
  fechaAceptacion: string | null;
  fechaInicioReal: string | null;
  fechaFin: string | null;
  codigoEncuentro: number | null;
};

export type ReservaEstadoKey =
  | "solicitada"
  | "aceptada"
  | "en_curso"
  | "finalizada"
  | "rechazada"
  | "cancelada"
  | "desconocida";

export type ReservaEstadoMeta = {
  key: ReservaEstadoKey;
  label: string;
  helper: string;
  className: string;
};
