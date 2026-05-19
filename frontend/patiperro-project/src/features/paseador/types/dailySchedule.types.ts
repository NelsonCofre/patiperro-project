export type DailyScheduleItem = {
  idReserva: number;
  idAgendaBloque: number;
  mascotaNombre: string;
  fechaAgenda: string;
  horaInicio: string;
  horaFin: string;
  inicioProgramado: string;
  finProgramado: string;
  comuna: string;
  direccionReferencia: string;
  idEstadoReserva: number | null;
  nombreEstado: string | null;
};
