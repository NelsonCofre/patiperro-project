// Tipos del formulario de tarifas por tamano del paseador.
// Se acercan al modelo real de tarifa_paseador para facilitar la integracion con backend.
export type TarifaPaseadorFormItem = {
  tarifaId: number | null;
  configuracionId: number | null;
  tamanoId: number;
  tamanoNombre: string;
  descripcion: string;
  enabled: boolean;
  precioBase: string;
};

export type TarifasForm = {
  /** Kilómetros (decimal como texto para el input). Obligatorio en API. */
  radioCoberturaKm: string;
  tarifas: TarifaPaseadorFormItem[];
};

export type TarifasErrors = Record<number, string | undefined>;

export type TarifaPaseadorPayloadItem = {
  id_tarifa: number | null;
  configuracion_id_configuracion: number | null;
  tamano_id_tamano: number;
  precio_base: number | null;
  activo: boolean;
};
