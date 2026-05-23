/**
 * Modelo de tarjeta en home tutor. La UI actual solo muestra nombre, foto, distancia y bio;
 * el resto sigue en el tipo para filtros mock y futura integracion con APIs.
 */
export type PaseadorHome = {
  id: string;
  nombre: string;
  fotoUrl: string;
  distanciaKm: number;
  calificacionPromedio: number;
  precioBase: number;
  disponible: boolean;
  perfilCompleto: boolean;
  tieneBloqueDisponibleFuturo: boolean;
  tieneTarifaConfigurada: boolean;
  radioCoberturaKm: number;
  proximoBloque: string;
  bio: string;
  latitud: number;
  longitud: number;
  /** Identidad verificada (cédula aprobada en paseadores-service). */
  verificado: boolean;
};

export type TamanoPerro = "Pequeno" | "Mediano" | "Grande";

export type TarifaPorTamano = {
  tamano: TamanoPerro;
  precio: number;
};

export type BloqueDisponibilidad = {
  inicio: string;
  fin: string;
};

export type DiaDisponibilidad = {
  fecha: string;
  etiqueta: string;
  bloques: BloqueDisponibilidad[];
};

export type ResenaPaseador = {
  id: string;
  tutorNombre: string;
  calificacion: number;
  comentario: string;
  fecha: string;
};

export type PaseadorPerfil = PaseadorHome & {
  nombreCompleto: string;
  totalPaseosRealizados: number;
  tarifasPorTamano: TarifaPorTamano[];
  disponibilidadProximos7Dias: DiaDisponibilidad[];
  resenas: ResenaPaseador[];
  verificado: boolean;
};
