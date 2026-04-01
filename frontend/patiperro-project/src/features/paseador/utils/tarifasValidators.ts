// Validaciones del formulario de tarifas del paseador.
// Controlan montos validos y preparan el payload para backend.
import type { UpsertConfiguracionBody } from "../services/paseadorConfigService";
import type {
  TarifaPaseadorFormItem,
  TarifaPaseadorPayloadItem,
  TarifasErrors,
  TarifasForm
} from "../types/tarifas.types";

export function keepOnlyMoneyDigits(value: string): string {
  return value.replace(/\D/g, "");
}

export function validateTarifaValue(enabled: boolean, value: string): string | undefined {
  if (!enabled) {
    return undefined;
  }

  if (!value.trim()) {
    return "La tarifa debe ser un monto numerico mayor a $0";
  }

  if (!/^\d+$/.test(value.trim())) {
    return "La tarifa debe ser un monto numerico mayor a $0";
  }

  if (Number.parseInt(value, 10) <= 0) {
    return "La tarifa debe ser un monto numerico mayor a $0";
  }

  return undefined;
}

export function validateRadioCobertura(value: string): string | undefined {
  const t = value.trim();
  if (!t) {
    return "Indica el radio de cobertura en kilómetros.";
  }
  const n = Number.parseFloat(t.replace(",", "."));
  if (Number.isNaN(n) || n < 0) {
    return "El radio debe ser un número mayor o igual a 0.";
  }
  return undefined;
}

export function validateTarifasForm(form: TarifasForm): TarifasErrors {
  const perRow = form.tarifas.reduce<TarifasErrors>((acc, tarifa) => {
    acc[tarifa.tamanoId] = validateTarifaValue(tarifa.enabled, tarifa.precioBase);
    return acc;
  }, {});

  if (form.tarifas.length === 0) {
    return perRow;
  }

  const activeWithPrice = form.tarifas.filter(
    (t) => t.enabled && t.precioBase.trim() && Number.parseInt(t.precioBase, 10) >= 1
  );
  if (activeWithPrice.length === 0) {
    // Marca error en la primera fila para que el usuario vea el bloque.
    const firstId = form.tarifas[0]?.tamanoId;
    if (firstId !== undefined) {
      perRow[firstId] =
        perRow[firstId] ??
        "Activa al menos un tamaño con precio por hora (entero ≥ 1), como exige el backend.";
    }
  }

  return perRow;
}

export function validateConfigForm(form: TarifasForm): { radio?: string; tarifas: TarifasErrors } {
  return {
    radio: validateRadioCobertura(form.radioCoberturaKm),
    tarifas: validateTarifasForm(form)
  };
}

export function buildTarifaPaseadorPayload(
  form: TarifasForm
): TarifaPaseadorPayloadItem[] {
  return form.tarifas.map((tarifa) => ({
    id_tarifa: tarifa.tarifaId,
    configuracion_id_configuracion: tarifa.configuracionId,
    tamano_id_tamano: tarifa.tamanoId,
    precio_base:
      tarifa.enabled && tarifa.precioBase.trim()
        ? Number.parseInt(tarifa.precioBase, 10)
        : null,
    activo: tarifa.enabled
  }));
}

/** Cuerpo PUT /api/paseadores/me/configuracion (solo tarifas activas con precio). */
export function buildUpsertConfiguracionBody(form: TarifasForm): UpsertConfiguracionBody {
  const radio = Number.parseFloat(form.radioCoberturaKm.trim().replace(",", "."));
  const tarifas = form.tarifas
    .filter((t) => t.enabled && t.precioBase.trim())
    .map((t) => ({
      tamanoId: t.tamanoId,
      precioPorHora: Number.parseInt(t.precioBase, 10)
    }));
  return {
    radioCoberturaKm: radio,
    tarifas
  };
}

export function updateTarifaItem(
  tarifas: TarifaPaseadorFormItem[],
  tamanoId: number,
  updater: (item: TarifaPaseadorFormItem) => TarifaPaseadorFormItem
) {
  return tarifas.map((item) => (item.tamanoId === tamanoId ? updater(item) : item));
}
