// Base comun del backend/gateway consumido por el frontend.
export const API_BASE_URL = "http://localhost:8080";

/**
 * En desarrollo, checkout con Bricks debe ir directo a pagos-service: el gateway WebMVC no reenvía
 * {@code Authorization} a los microservicios. En build de producción se usa el gateway.
 */
export const PAGOS_CHECKOUT_API_BASE =
  typeof import.meta !== "undefined" && import.meta.env.DEV
    ? "http://localhost:8087"
    : API_BASE_URL;

/** Backend MVP aislado de Checkout Pro (pago-service). */
export const PAGO_SERVICE_API_BASE =
  typeof import.meta !== "undefined" && import.meta.env.DEV
    ? "http://localhost:8088"
    : API_BASE_URL;

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
    /** Checkout Pro autenticado para tutor (crea preferencia con access token en backend). */
    checkoutProPreferenciaTutor: `${PAGOS_CHECKOUT_API_BASE}/api/pagos/checkout/pro/preferencia`,
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

/** Prefijo para resolver rutas relativas guardadas en el backend (ej. foto de perfil). */
export function resolveApiUrl(pathOrUrl: string): string {
  // Evita errores si el backend devuelve una ruta vacia.
  if (!pathOrUrl) return "";
  // Si ya es una URL completa, no necesita transformacion.
  if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
    return pathOrUrl;
  }
  // Completa rutas relativas usando la base del gateway.
  return `${API_BASE_URL.replace(/\/$/, "")}${pathOrUrl.startsWith("/") ? "" : "/"}${pathOrUrl}`;
}
