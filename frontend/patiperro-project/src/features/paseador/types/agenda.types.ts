// Tipos base de la agenda del paseador.
// Dejan preparada la vista para futuros bloques creados desde backend o formulario.
export type AgendaDay =
  | "Lunes"
  | "Martes"
  | "Miercoles"
  | "Jueves"
  | "Viernes"
  | "Sabado"
  | "Domingo";

export type AgendaBlockStatus = "available" | "booked";

export type AgendaBlock = {
  id: string;
  day: AgendaDay;
  startTime: string;
  endTime: string;
  title: string;
  status: AgendaBlockStatus;
};

export type AgendaToast = {
  type: "success" | "error" | "info";
  title: string;
  message: string;
};

export type AgendaBlockForm = {
  day: AgendaDay;
  startTime: string;
  endTime: string;
};

export type AgendaBlockFormErrors = {
  day?: string;
  startTime?: string;
  endTime?: string;
};
