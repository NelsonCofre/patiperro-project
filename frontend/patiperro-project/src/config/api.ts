const DEFAULT_GATEWAY_LOCAL = "http://localhost:8080";

function readEnvTrim(name: string): string {
  try {
    return String((import.meta as { env?: Record<string, string | undefined> }).env?.[name] ?? "").trim();
  } catch {
    return "";
  }
}

function isDev(): boolean {
  return typeof import.meta !== "undefined" && import.meta.env.DEV;
}

/**
 * Base del gateway.
 * - Dev con {@code VITE_API_BASE_URL=proxy}: cadena vacía → URLs `/api/...` (mismo host que Vite/Cloudflare;
 *   el proxy en {@code vite.config.ts} evita CORS con túneles).
 * - Dev con URL absoluta (ngrok, etc.): hay que permitir el origen del front en el gateway (CORS).
 * - Prod: {@code VITE_API_BASE_URL} o fallback local.
 */
function resolveApiBaseUrl(): string {
  const raw = readEnvTrim("VITE_API_BASE_URL");
  const legacyNgrok = readEnvTrim("VITE_DEV_NGROK_GATEWAY_BASE");
  if (raw.toLowerCase() === "proxy" || raw === "/") {
    return "";
  }
  if (raw) {
    return raw;
  }
  if (legacyNgrok) {
    return legacyNgrok;
  }
  if (isDev()) {
    return "";
  }
  return DEFAULT_GATEWAY_LOCAL;
}

export const API_BASE_URL = resolveApiBaseUrl();

/**
 * Checkout Bricks / comprobante / sincronizar Checkout Pro.
 * En dev, por defecto mismo origen que el resto (`""` → `/api/...` vía proxy Vite → gateway → pagos).
 * Así el retorno de MP (mismo u otro puerto / túnel) no dispara CORS contra :8087.
 * Override explícito: {@code VITE_PAGOS_CHECKOUT_API_BASE} (p. ej. llamada directa a pagos en pruebas aisladas).
 */
function resolvePagosCheckoutApiBase(): string {
  const override = readEnvTrim("VITE_PAGOS_CHECKOUT_API_BASE");
  if (override) return override;
  if (!isDev()) {
    return API_BASE_URL;
  }
  if (!API_BASE_URL) {
    return "";
  }
  const gatewayRemoto =
    API_BASE_URL !== DEFAULT_GATEWAY_LOCAL && API_BASE_URL !== "http://127.0.0.1:8080";
  if (gatewayRemoto) {
    return API_BASE_URL;
  }
  // Gateway local: no usar :8087 directo (CORS en PagosCorsConfiguration solo listaba 5173/5174).
  // El gateway reenvía el JWT vía X-Patiperro-Authorization (DuplicateAuthorizationForDownstreamFilter).
  return "";
}

export const PAGOS_CHECKOUT_API_BASE = resolvePagosCheckoutApiBase();

/** Backend MVP aislado de Checkout Pro (pago-service). */
export const PAGO_SERVICE_API_BASE =
  isDev() && API_BASE_URL ? "http://localhost:8088" : API_BASE_URL || DEFAULT_GATEWAY_LOCAL;

/** Tras login paseador se guarda idPaseador para llamadas /api/agenda/bloques/usuario/{id}. */
export const PASEADOR_ID_SESSION_KEY = "patiperro_paseador_id";

/** Tras login/registro tutor se guarda id para GET /api/tutores/{id} (coordenadas de direccion). */
export const TUTOR_ID_SESSION_KEY = "patiperro_tutor_id";

/**
 * JWT devuelto en el body de login/registro (mismo valor que cookie access_token).
 * Necesario para Authorization: Bearer hacia el gateway desde el dev server (origen distinto).
 */
export const ACCESS_TOKEN_SESSION_KEY = "patiperro_access_token";
/** Endpoint WS STOMP expuesto por reserva-service para eventos de encuentro. */
export const RESERVA_WS_URL = "ws://localhost:8090/ws/reservas";

/**
 * WebSocket STOMP del chat (mismo origen que Vite/Cloudflare → proxy /ws/chat → chat-service).
 * Override: {@code VITE_CHAT_WS_URL} (ej. ws://127.0.0.1:8089/ws/chat).
 */
export function resolveChatWsBrokerUrl(): string {
  const override = readEnvTrim("VITE_CHAT_WS_URL");
  if (override) {
    return override.replace(/\/$/, "");
  }
  if (typeof window !== "undefined") {
    const proto = window.location.protocol === "https:" ? "wss:" : "ws:";
    return `${proto}//${window.location.host}/ws/chat`;
  }
  return "ws://127.0.0.1:5173/ws/chat";
}

export const CHAT_WS_BROKER_URL = resolveChatWsBrokerUrl();

// Endpoints centralizados para evitar URLs repetidas en paginas y servicios.
export const API_ENDPOINTS = {
  auth: {
    tutores: {
      register: `${API_BASE_URL}/api/auth/tutores/register`,
      login: `${API_BASE_URL}/api/auth/tutores/login`,
      uploadFotoPerfil: `${API_BASE_URL}/api/tutores/auth/upload-foto-perfil`
    },
    paseadores: {
      register: `${API_BASE_URL}/api/paseadores/auth/register`,
      login: `${API_BASE_URL}/api/paseadores/auth/login`,
      uploadFotoPerfil: `${API_BASE_URL}/api/paseadores/auth/upload-foto-perfil`,
      /** Catálogo público (sin JWT). */
      publicTamanos: `${API_BASE_URL}/api/paseadores/public/tamanos`,
      /** Configuración del paseador autenticado (JWT en cookie vía gateway). */
      meConfiguracion: `${API_BASE_URL}/api/paseadores/me/configuracion`,
      /**
       * Búsqueda pública por proximidad (Haversine + radio de cobertura). Query params obligatorios:
       * latitudReferencia, longitudReferencia; opcionales: radioBusquedaMaxKm, limite, y filtro agenda
       * (fechaDisponibilidad, horaInicioDisponibilidad, horaFinDisponibilidad, idEstadoBloqueDisponible).
       */
      publicCercanos: `${API_BASE_URL}/api/paseadores/public/cercanos`,
      /** GET perfil público mínimo (solicitud paseo / tutor). */
      publicPerfil: (idPaseador: number) =>
        `${API_BASE_URL}/api/paseadores/public/${idPaseador}`,
      publicConfiguracion: (idPaseador: number) =>
        `${API_BASE_URL}/api/paseadores/public/${idPaseador}/configuracion`
    }
  },
  /** Mascotas (JWT tutor en cookie vía gateway). */
  mascotas: {
    base: `${API_BASE_URL}/api/mascotas`,
    mias: `${API_BASE_URL}/api/mascotas/mias`,
    especies: `${API_BASE_URL}/api/mascotas/especies`,
    razas: (especieId?: number) =>
      especieId != null
        ? `${API_BASE_URL}/api/mascotas/razas?especieId=${especieId}`
        : `${API_BASE_URL}/api/mascotas/razas`,
    tamanos: `${API_BASE_URL}/api/mascotas/tamanos`
  },
  /** Agenda (JWT en cookie vía gateway; mismo access_token que paseador/tutor). */
  agenda: {
    estadosBloque: `${API_BASE_URL}/api/agenda/estados-bloque`,
    diasSemana: `${API_BASE_URL}/api/agenda/dias-semana`,
    bloques: `${API_BASE_URL}/api/agenda/bloques`,
    bloqueosDia: `${API_BASE_URL}/api/agenda/bloqueos-dia`,
    bloquesSerieMes: `${API_BASE_URL}/api/agenda/bloques/serie-mes`,
    bloquesPorUsuario: (idUsuario: number) =>
      `${API_BASE_URL}/api/agenda/bloques/usuario/${idUsuario}`,
    bloqueosDiaPorUsuario: (idUsuario: number) =>
      `${API_BASE_URL}/api/agenda/bloqueos-dia/usuario/${idUsuario}`,
    desbloquearDiaPorUsuario: (idUsuario: number, fechaISO: string) =>
      `${API_BASE_URL}/api/agenda/bloqueos-dia/usuario/${idUsuario}/fecha/${fechaISO}`,
    bloquesOferta: (idUsuario: number, desde: string, hasta: string) =>
      `${API_BASE_URL}/api/agenda/bloques/usuario/${idUsuario}/oferta?desde=${desde}&hasta=${hasta}`,
    bloque: (idBloque: number) => `${API_BASE_URL}/api/agenda/bloques/${idBloque}`
  },
  /** Perfil tutor (JWT en cookie vía gateway). */
  tutores: {
    byId: (idTutor: number) => `${API_BASE_URL}/api/tutores/${idTutor}`,
    bookings: `${API_BASE_URL}/api/tutor/bookings`
  },
  bookings: {
    timeline: (idBooking: number) => `${API_BASE_URL}/api/bookings/${idBooking}/timeline`,
    iniciarCheckoutMercadoPago: (idBooking: number) => `${API_BASE_URL}/api/bookings/${idBooking}/mercadopago/checkout`
  },
  reservas: {
    validarCodigo: `${API_BASE_URL}/api/reservas/codigo/validar`,
    estadoEncuentro: (idReserva: number) => `${API_BASE_URL}/api/reservas/${idReserva}/estado-encuentro`
  },
  pagos: {
    checkoutSimulado: `${API_BASE_URL}/api/pagos/checkout/simulado`,
    /** Checkout API: token del Payment Brick (en dev contra :8087 por JWT; en prod vía gateway). */
    pagoBrick: `${PAGOS_CHECKOUT_API_BASE}/api/pagos/checkout/pago-brick`,
    /** Billetera del paseador autenticado (en dev directo a :8087 por JWT). */
    billeteraPaseador: `${PAGOS_CHECKOUT_API_BASE}/api/pagos/paseador/billetera`,
    /** Solicitud de retiro del paseador autenticado. */
    retiroPaseador: `${PAGOS_CHECKOUT_API_BASE}/api/pagos/paseador/billetera/retiros`,
    /** Resumen/comprobante de pago para tutor por reserva. */
    comprobanteByReserva: (idReserva: number) =>
      `${PAGOS_CHECKOUT_API_BASE}/api/pagos/comprobante/${idReserva}`,
    /** Checkout Pro autenticado para tutor (crea preferencia con access token en backend). */
    checkoutProPreferenciaTutor: `${PAGOS_CHECKOUT_API_BASE}/api/pagos/checkout/pro/preferencia`,
    /** Tras volver de Checkout Pro: aplica el mismo flujo que el webhook usando payment_id (útil en local). */
    checkoutProSincronizarPagoTutor: `${PAGOS_CHECKOUT_API_BASE}/api/pagos/checkout/pro/sincronizar-pago`,
    /** Cuentas bancarias registradas para retiros (paseador autenticado). */
    cuentasBancariasPaseador: `${PAGOS_CHECKOUT_API_BASE}/api/pagos/paseador/billetera/cuentas-bancarias`,
    /** Catálogo banco + tipo_cuenta para el formulario de registro (paseador autenticado). */
    catalogoRegistroCuentaPaseador: `${PAGOS_CHECKOUT_API_BASE}/api/pagos/paseador/billetera/catalogo/registro-cuenta`,
    /** Sandbox MVP: crea preferencia Checkout Pro en pagos-service (endpoint interno con secret). */
    checkoutProPreferencias: `${PAGOS_CHECKOUT_API_BASE}/api/pagos/interno/mercadopago/checkout/preferencia`
  },
  reserva: {
    base: `${API_BASE_URL}/api/reserva`,
    estados: `${API_BASE_URL}/api/reserva/estados`,
    byTutor: (idTutor: number) => `${API_BASE_URL}/api/reserva/tutor/${idTutor}`,
    byTutorDetalle: (idTutor: number) => `${API_BASE_URL}/api/reserva/tutor/${idTutor}/detalle`,
    byId: (idReserva: number) => `${API_BASE_URL}/api/reserva/${idReserva}`,
    status: (idReserva: number) => `${API_BASE_URL}/api/reserva/${idReserva}/status`,
    paseadorSolicitudesPendientes: (idPaseador: number) =>
      `${API_BASE_URL}/api/reserva/paseador/${idPaseador}/solicitudes-pendientes`
  }
};

/**
 * Resuelve rutas de recursos del gateway (fotos, etc.).
 * En dev: URL relativa bajo `/api` para que Vite la proxifique a {@code VITE_GATEWAY_PROXY_TARGET}
 * (los {@code <img>} no pueden enviar {@code ngrok-skip-browser-warning}).
 */
export function resolveApiUrl(pathOrUrl: string): string {
  if (!pathOrUrl) return "";
  const isDev = typeof import.meta !== "undefined" && import.meta.env.DEV;

  if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
    if (isDev) {
      try {
        const u = new URL(pathOrUrl);
        if (u.pathname.startsWith("/api")) {
          return `${u.pathname}${u.search}`;
        }
      } catch {
        /* ignorar */
      }
    }
    return pathOrUrl;
  }

  const path = pathOrUrl.startsWith("/") ? pathOrUrl : `/${pathOrUrl}`;
  if (isDev && path.startsWith("/api")) {
    return path;
  }
  return `${API_BASE_URL.replace(/\/$/, "")}${path}`;
}
