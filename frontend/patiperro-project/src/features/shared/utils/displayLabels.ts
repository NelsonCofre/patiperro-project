export const FALLBACK_MASCOTA = "Mascota";
export const FALLBACK_PASEADOR = "Paseador asignado";
export const FALLBACK_TUTOR = "Tutor";

const MASCOTA_PLACEHOLDER = /^Mascota\s*#\d+$/i;

/** Nombre genérico del tipo "Mascota #5" cuando falta integración con mascotas-service. */
export function isMascotaPlaceholderName(value?: string | null): boolean {
  const trimmed = (value ?? "").trim();
  if (!trimmed) return true;
  return trimmed === FALLBACK_MASCOTA || MASCOTA_PLACEHOLDER.test(trimmed);
}

/** Prioriza un nombre legible de mascota sobre placeholders del comprobante o reserva. */
export function resolveMascotaDisplayName(
  ...candidates: Array<string | null | undefined>
): string {
  for (const candidate of candidates) {
    const trimmed = (candidate ?? "").trim();
    if (trimmed && !isMascotaPlaceholderName(trimmed)) {
      return trimmed;
    }
  }
  const firstNonEmpty = candidates.map((c) => (c ?? "").trim()).find(Boolean);
  return firstNonEmpty || FALLBACK_MASCOTA;
}

export function formatFechaCorta(value?: string | null): string {
  if (!value?.trim()) return "Fecha por confirmar";
  const raw = value.trim();
  const date = new Date(raw.includes("T") ? raw : `${raw}T12:00:00`);
  if (Number.isNaN(date.getTime())) return raw;
  return new Intl.DateTimeFormat("es-CL", {
    weekday: "short",
    day: "2-digit",
    month: "short"
  }).format(date);
}

export function tituloPaseoReserva(input: {
  mascotaNombre?: string | null;
  paseadorNombre?: string | null;
}): string {
  const mascota = input.mascotaNombre?.trim() || FALLBACK_MASCOTA;
  const paseador = input.paseadorNombre?.trim() || "paseador";
  return `Paseo de ${mascota} con ${paseador}`;
}

export function etiquetaPaseoProgramado(fecha?: string | null): string {
  return `Paseo · ${formatFechaCorta(fecha)}`;
}

export function subtituloChatPaseo(input: {
  mascotaNombre?: string | null;
  counterpartName?: string | null;
}): string {
  const mascota = input.mascotaNombre?.trim() || FALLBACK_MASCOTA;
  const counterpart = input.counterpartName?.trim() || "contacto";
  return `${mascota} con ${counterpart}`;
}

export function tituloItemCheckout(input: {
  mascota?: string | null;
  paseador?: string | null;
}): string {
  const mascota = input.mascota?.trim() || FALLBACK_MASCOTA;
  const paseador = input.paseador?.trim() || "paseador";
  return `Paseo de ${mascota} con ${paseador} · Patiperro`;
}

export function referenciaTransaccionExterna(
  paymentTransactionId?: string | null,
  paymentOrderId?: string | null,
  idTransaccionExterna?: string | null
): string {
  return (
    idTransaccionExterna?.trim() ||
    paymentTransactionId?.trim() ||
    paymentOrderId?.trim() ||
    "Referencia no disponible"
  );
}

export function slugParaArchivo(value: string): string {
  return (
    value
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .replace(/[^a-zA-Z0-9]+/g, "-")
      .replace(/^-+|-+$/g, "")
      .toLowerCase() || "patiperro"
  );
}
