import { buildNominatimSearchUrl } from "../../../config/nominatimSearchUrl";

export type GeocodeResult = {
  lat: number;
  lng: number;
};

type GeocodeInput = {
  direccionReferencia?: string | null;
  comuna?: string | null;
};

const DASH = "—";

/** Quita complementos que suelen romper Nominatim (depto, casa, block, etc.). */
function simplificarDireccionParaGeocodificar(value: string): string {
  return value
    .replace(/,\s*(departamento|depto\.?|casa|block|torre|piso)\s+[^,]+/gi, "")
    .replace(/\s{2,}/g, " ")
    .replace(/,\s*,/g, ",")
    .trim()
    .replace(/,\s*$/, "");
}

function buildGeocodeQueries(input: GeocodeInput): string[] {
  const comuna = input.comuna?.trim();
  const rawDir = input.direccionReferencia?.trim();
  const queries: string[] = [];

  if (rawDir && rawDir !== DASH) {
    const base = /chile/i.test(rawDir) ? rawDir : `${rawDir}, Chile`;
    queries.push(base);

    const simplified = simplificarDireccionParaGeocodificar(rawDir);
    if (simplified && simplified !== rawDir) {
      queries.push(/chile/i.test(simplified) ? simplified : `${simplified}, Chile`);
    }

    if (comuna && comuna !== DASH && !new RegExp(comuna, "i").test(rawDir)) {
      queries.push(`${rawDir}, ${comuna}, Chile`);
    }
  }

  if (comuna && comuna !== DASH) {
    queries.push(`${comuna}, Santiago, Región Metropolitana, Chile`);
  }

  return [...new Set(queries.filter(Boolean))];
}

async function nominatimSearch(query: string): Promise<GeocodeResult | null> {
  const encoded = encodeURIComponent(query);
  const response = await fetch(buildNominatimSearchUrl(encoded));
  if (!response.ok) {
    throw new Error(`Geocodificación respondió HTTP ${response.status}`);
  }

  const data = (await response.json()) as Array<{ lat?: string; lon?: string }>;
  if (!Array.isArray(data) || data.length === 0) {
    return null;
  }

  const lat = Number.parseFloat(data[0].lat ?? "");
  const lng = Number.parseFloat(data[0].lon ?? "");
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
    return null;
  }

  return { lat, lng };
}

/** Resuelve coordenadas con varias consultas de respaldo (Chile / Nominatim). */
export async function geocodeEncuentroAddress(input: GeocodeInput): Promise<GeocodeResult> {
  const queries = buildGeocodeQueries(input);
  if (queries.length === 0) {
    throw new Error("No hay dirección suficiente para ubicar en el mapa.");
  }

  let lastError: unknown = null;
  for (const query of queries) {
    try {
      const hit = await nominatimSearch(query);
      if (hit) {
        return hit;
      }
    } catch (error) {
      lastError = error;
    }
  }

  if (lastError instanceof Error) {
    throw lastError;
  }
  throw new Error("No se pudo encontrar la ubicación en el mapa.");
}
