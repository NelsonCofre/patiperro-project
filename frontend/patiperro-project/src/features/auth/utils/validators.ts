// Helpers y reglas de validacion reutilizados por los formularios de auth.
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
const LETTER_REGEX = /^[A-Za-zÁÉÍÓÚáéíóúÑñÜü\s'-]+$/;
const PASSWORD_UPPERCASE_REGEX = /[A-Z]/;
const PASSWORD_NUMBER_REGEX = /\d/;
const PASSWORD_SPECIAL_REGEX = /[^A-Za-z0-9]/;

export function isValidEmail(value: string): boolean {
  return EMAIL_REGEX.test(value.trim());
}

export function getPasswordSecurityError(value: string): string | null {
  if (!value) {
    return "La contraseña es obligatoria.";
  }

  if (
    value.length < 8 ||
    !PASSWORD_UPPERCASE_REGEX.test(value) ||
    !PASSWORD_NUMBER_REGEX.test(value) ||
    !PASSWORD_SPECIAL_REGEX.test(value)
  ) {
    return "Tu contraseña debe contener al menos 8 caracteres, incluyendo una mayúscula, un número y un carácter especial.";
  }

  return null;
}

export function hasOnlyLetters(value: string): boolean {
  return LETTER_REGEX.test(value.trim());
}

export function keepOnlyDigits(value: string): string {
  return value.replace(/\D/g, "");
}

export function normalizeRut(value: string): string {
  const cleaned = value.replace(/[^0-9kK-]/g, "").toUpperCase();
  const withoutExtraHyphens = cleaned.replace(/-/g, "");

  if (withoutExtraHyphens.length <= 1) {
    return withoutExtraHyphens;
  }

  const body = withoutExtraHyphens.slice(0, -1);
  const verifier = withoutExtraHyphens.slice(-1);
  return `${body}-${verifier}`;
}

export function isValidRut(value: string): boolean {
  const normalized = value.replace(/\./g, "").replace(/-/g, "").toUpperCase();

  if (!/^\d{7,8}[0-9K]$/.test(normalized)) {
    return false;
  }

  const body = normalized.slice(0, -1);
  const verifier = normalized.slice(-1);

  let sum = 0;
  let multiplier = 2;

  for (let index = body.length - 1; index >= 0; index -= 1) {
    sum += Number(body[index]) * multiplier;
    multiplier = multiplier === 7 ? 2 : multiplier + 1;
  }

  const remainder = 11 - (sum % 11);
  const expectedVerifier =
    remainder === 11 ? "0" : remainder === 10 ? "K" : String(remainder);

  return verifier === expectedVerifier;
}

export function getMaxBirthDate(): string {
  const today = new Date();
  const maxDate = new Date(
    today.getFullYear() - 18,
    today.getMonth(),
    today.getDate()
  );

  return maxDate.toISOString().split("T")[0];
}
