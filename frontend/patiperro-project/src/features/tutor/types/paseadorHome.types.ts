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
};
