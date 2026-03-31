// Validaciones y helpers del formulario de mascota.
// Mantienen separadas las reglas de negocio del componente visual.
import type { MascotaForm } from "../types/mascota.types";

export function keepOnlyDigits(value: string): string {
  return value.replace(/\D/g, "");
}

export function keepDecimalWeight(value: string): string {
  const cleaned = value.replace(/[^0-9.,]/g, "").replace(",", ".");
  const [integerPart, decimalPart] = cleaned.split(".");

  if (decimalPart === undefined) {
    return integerPart;
  }

  return `${integerPart}.${decimalPart.slice(0, 2)}`;
}

export function getTodayDate(): string {
  return new Date().toISOString().split("T")[0];
}

export function validateMascotaField(
  name: keyof MascotaForm,
  value: string | File | null,
  form: MascotaForm
): string | undefined {
  const stringValue = typeof value === "string" ? value.trim() : "";

  if (name === "nombre" && !stringValue) return "El nombre es obligatorio";
  if (name === "especie" && !stringValue) return "Selecciona una especie";
  if (name === "raza" && !stringValue) return "Selecciona una raza";
  if (name === "sexo" && !stringValue) return "Selecciona el sexo";
  if (name === "fecha_nacimiento" && !stringValue) {
    return "La fecha de nacimiento es obligatoria";
  }

  if (name === "peso") {
    if (!stringValue) return "El peso es obligatorio";
    if (!/^\d+(\.\d{1,2})?$/.test(stringValue)) return "Ingresa un peso valido";
  }

  if (name === "tamano" && !stringValue) return "Selecciona un tamano";
  if (name === "comportamiento" && !stringValue) {
    return "Describe el comportamiento principal";
  }
  if (name === "descripcion" && !stringValue) return "La descripcion es obligatoria";
  if (name === "cuidados_especiales" && !stringValue) {
    return "Indica si requiere cuidados especiales";
  }
  if (name === "esterilizado" && !stringValue) {
    return "Indica si esta esterilizado";
  }

  if (name === "numero_chip") {
    if (!stringValue) return "El numero de chip es obligatorio";
    if (!/^\d+$/.test(stringValue)) {
      return "El numero de chip solo acepta numeros";
    }
  }

  if (name === "foto" && !form.foto) return "Debes subir una foto de tu mascota";

  return undefined;
}
