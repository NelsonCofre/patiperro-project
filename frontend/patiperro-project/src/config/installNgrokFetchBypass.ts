const HEADER = "ngrok-skip-browser-warning";
const HEADER_VALUE = "true";

function requestUrlLooksLikeNgrok(url: string): boolean {
  const u = url.toLowerCase();
  return u.includes("ngrok-free.dev") || u.includes("ngrok-free.app") || u.includes(".ngrok.io");
}

function resolveUrl(input: RequestInfo | URL): string {
  if (typeof input === "string") return input;
  if (input instanceof URL) return input.href;
  return input.url;
}

/**
 * Evita la página intersticial de ngrok en el navegador (HTML sin CORS que dispara
 * "No 'Access-Control-Allow-Origin'"). Sin efecto en fetch a otros hosts.
 */
export function installNgrokFetchBypass(): void {
  const w = window as unknown as { __patiperroNgrokFetchPatched?: boolean };
  if (typeof window === "undefined" || w.__patiperroNgrokFetchPatched) return;
  w.__patiperroNgrokFetchPatched = true;

  const orig = window.fetch.bind(window);
  window.fetch = (input: RequestInfo | URL, init?: RequestInit) => {
    const url = resolveUrl(input);
    if (!requestUrlLooksLikeNgrok(url)) {
      return orig(input, init);
    }

    if (input instanceof Request) {
      const h = new Headers(input.headers);
      if (!h.has(HEADER)) h.set(HEADER, HEADER_VALUE);
      const req = new Request(input, { headers: h });
      return orig(req, init);
    }

    const headers = new Headers(init?.headers ?? undefined);
    if (!headers.has(HEADER)) headers.set(HEADER, HEADER_VALUE);
    return orig(input, { ...init, headers });
  };
}
