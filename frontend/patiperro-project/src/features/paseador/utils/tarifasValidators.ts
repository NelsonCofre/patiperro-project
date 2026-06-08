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

export function normalizeTamanoId(value: unknown): number {
  const id = Number(value);
  return Number.isFinite(id) && id > 0 ? id : 0;
}

export function parsePrecioPorHora(value: unknown): number | null {
  const n = Number(value);
  if (!Number.isFinite(n) || n < 1) return null;
  return Math.trunc(n);
}

/** Tarifa que se publicará al guardar (switch activo + precio válido). */
export function isTarifaOfrecida(tarifa: Pick<TarifaPaseadorFormItem, "enabled" | "precioBase">): boolean {
  if (!tarifa.enabled) return false;
  return parsePrecioPorHora(tarifa.precioBase.trim()) != null;
}

export function countTarifasOfrecidas(form: TarifasForm): number {
  return form.tarifas.filter(isTarifaOfrecida).length;
}

export function formatTarifaCLP(value: string | number): string {
  const n = typeof value === "number" ? value : Number.parseInt(String(value).replace(/\D/g, ""), 10);
  if (!Number.isFinite(n) || n <= 0) return "";
  return new Intl.NumberFormat("es-CL", {
    style: "currency",
    currency: "CLP",
    maximumFractionDigits: 0
  }).format(n);
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
  
  // 1. Validar que no esté vacío
  if (!t) {
    return "Indica el radio de cobertura en kilómetros.";
  }

  // Convertimos a número manejando comas por puntos (estándar chileno)
  const n = Number.parseFloat(t.replace(",", "."));

  // 2. Validar que sea un número válido
  if (Number.isNaN(n)) {
    return "Por favor, ingresa un número válido.";
  }

  // 3. Validar rango: Mínimo 1km y Máximo 50km (Según Criterios de Aceptación)
  if (n < 1 || n > 50) {
    return "El radio debe estar entre 1 y 50 kilómetros.";
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

  const activeWithPrice = form.tarifas.filter(isTarifaOfrecida);
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
    .filter(isTarifaOfrecida)
    .map((t) => ({
      tamanoId: t.tamanoId,
      precioPorHora: parsePrecioPorHora(t.precioBase.trim())!
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
