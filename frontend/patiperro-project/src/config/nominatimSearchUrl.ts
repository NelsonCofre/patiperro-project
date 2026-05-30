/**
 * URL de búsqueda Nominatim. En dev usa proxy de Vite ({@code /geo/nominatim}) con User-Agent
 * adecuado; desde el navegador directo OSM suele fallar (política de uso / bloqueos).
 */
export function buildNominatimSearchUrl(encodedQuery: string): string {
  const qs = `format=json&q=${encodedQuery}&limit=1`;
  if (typeof import.meta !== "undefined" && import.meta.env.DEV) {
    return `/geo/nominatim/search?${qs}`;
  }
  return `https://nominatim.openstreetmap.org/search?${qs}`;
}
