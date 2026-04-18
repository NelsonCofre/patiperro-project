// Validaciones y helpers del formulario de mascota.
// Mantienen separadas las reglas de negocio del componente visual.
import type { MascotaForm } from "../types/mascota.types";

const ALLOWED_IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];
const MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;

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

export function getMascotaAgeLabel(birthDate: string): string {
  if (!birthDate) {
    return "";
  }

  const today = new Date();
  const birth = new Date(`${birthDate}T00:00:00`);

  if (Number.isNaN(birth.getTime()) || birth > today) {
    return "";
  }

  let months =
    (today.getFullYear() - birth.getFullYear()) * 12 +
    (today.getMonth() - birth.getMonth());

  if (today.getDate() < birth.getDate()) {
    months -= 1;
  }

  if (months < 0) {
    return "";
  }

  if (months < 12) {
    const safeMonths = Math.max(months, 0);
    return `${safeMonths} ${safeMonths === 1 ? "mes" : "meses"}`;
  }

  const years = Math.floor(months / 12);
  return `${years} ${years === 1 ? "año" : "años"}`;
}

export function validateMascotaPhoto(file: File | null): string | undefined {
  if (!file) return "Debes subir una foto de tu mascota";

  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    return "La foto debe ser un archivo JPG, PNG o WEBP";
  }

  if (file.size > MAX_IMAGE_SIZE_BYTES) {
    return "La foto no puede superar los 5 MB";
  }

  return undefined;
}

export function validateMascotaField(
  name: keyof MascotaForm,
  value: string | File | null,
  form: MascotaForm
): string | undefined {
  const stringValue = typeof value === "string" ? value.trim() : "";

  if (name === "nombre" && !stringValue) return "El nombre es obligatorio";
  if (name === "especie" && !stringValue) return "Selecciona la especie";
  if (name === "raza" && !stringValue) return "Selecciona la raza";
  if (name === "tamano" && !stringValue) return "Selecciona el tamaño";
  if (name === "sexo" && !stringValue) return "Selecciona el sexo";
  if (name === "fecha_nacimiento") {
    if (!stringValue) {
      return "La fecha de nacimiento es obligatoria";
    }

    const selectedDate = new Date(`${stringValue}T00:00:00`);
    const today = new Date(`${getTodayDate()}T00:00:00`);

    if (Number.isNaN(selectedDate.getTime()) || selectedDate > today) {
      return "La fecha de nacimiento no puede ser futura";
    }
  }

  if (name === "peso") {
    if (!stringValue) return "El peso es obligatorio";

    if (!/^\d+(\.\d{1,2})?$/.test(stringValue)) {
      return "El peso debe ser un numero valido mayor a 0 kg";
    }

    if (Number.parseFloat(stringValue) <= 0) {
      return "El peso debe ser un numero valido mayor a 0 kg";
    }
  }

  if (name === "comportamiento" && !stringValue) {
    return "Describe el comportamiento principal";
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

  if (name === "foto") return validateMascotaPhoto(form.foto);

  return undefined;
}
