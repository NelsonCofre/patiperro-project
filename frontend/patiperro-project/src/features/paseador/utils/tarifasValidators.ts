// Validaciones del formulario de tarifas del paseador.
// Controlan montos validos y preparan el payload para backend.
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

export function validateTarifasForm(form: TarifasForm): TarifasErrors {
  return form.tarifas.reduce<TarifasErrors>((acc, tarifa) => {
    acc[tarifa.tamanoId] = validateTarifaValue(tarifa.enabled, tarifa.precioBase);
    return acc;
  }, {});
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

export function updateTarifaItem(
  tarifas: TarifaPaseadorFormItem[],
  tamanoId: number,
  updater: (item: TarifaPaseadorFormItem) => TarifaPaseadorFormItem
) {
  return tarifas.map((item) => (item.tamanoId === tamanoId ? updater(item) : item));
}
