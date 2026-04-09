// Tipos base de la agenda del paseador.
// Se alinean con agenda_bloque y permiten trabajar con fechas concretas en frontend.
export type AgendaDay =
  | "Lunes"
  | "Martes"
  | "Miercoles"
  | "Jueves"
  | "Viernes"
  | "Sabado"
  | "Domingo";

export type AgendaBlockStatus = "available" | "booked" | "blocked";

export type AgendaBlock = {
  id: string;
  fecha: string;
  day: AgendaDay;
  startTime: string;
  endTime: string;
  title: string;
  status: AgendaBlockStatus;
  estadoBloqueId?: number | null;
  diaSemanaId?: number | null;
};

export type AgendaDateOption = {
  isoDate: string;
  dayLabel: AgendaDay;
  dayNumber: number;
  monthLabel: string;
};

export type AgendaBlockForm = {
  fecha: string;
  startTime: string;
  endTime: string;
};

export type AgendaBlockFormErrors = {
  fecha?: string;
  startTime?: string;
  endTime?: string;
};

export type AgendaBlockRangeForm = {
  fecha_inicio: string;
  fecha_fin: string;
};

export type AgendaBlockRangeErrors = {
  fecha_inicio?: string;
  fecha_fin?: string;
};

export type AgendaBlockedRange = {
  id: string;
  fecha_inicio: string;
  fecha_fin: string;
  estadoBloqueId?: number | null;
};

export type AgendaToast = {
  type: "success" | "error" | "info";
  title: string;
  message: string;
};
