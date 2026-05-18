import { API_ENDPOINTS } from "../../../config/api";
import { bearerAuthHeaders } from "../../../config/authHeaders";

export type BilleteraBucketKey = "retenido" | "verificacion" | "disponible";

export type BilleteraReservaItem = {
  idReserva: number;
  mascotaNombre: string;
  tutorNombre: string;
  fecha: string;
  horaInicio: string;
  montoBruto: number;
  comision: number;
  montoNeto: number;
  estado: string;
  fechaLiberacionEstimada?: string | null;
};

export type BilleteraBucket = {
  key: BilleteraBucketKey;
  title: string;
  helper: string;
  amount: number;
  grossAmount: number;
  commissionAmount: number;
  reservas: BilleteraReservaItem[];
};

/** Saldo en verificación agrupado por día en que el neto puede pasar a disponible (regla N+2). */
export type BilleteraProyeccionLiberacionGrupo = {
  fechaDisponibleDesde: string;
  totalNeto: number;
  liberacionPausadaPorDisputa: boolean;
  reservas: BilleteraReservaItem[];
};

export type BilleteraPaseadorData = {
  retenido: BilleteraBucket;
  verificacion: BilleteraBucket;
  disponible: BilleteraBucket;
  proyeccionLiberacionesPorDia: BilleteraProyeccionLiberacionGrupo[];
  updatedAt: string;
};

export type RetiroPaseadorResult = {
  idTransaccion: number;
  montoRetirado: number;
  saldoDisponibleTrasRetiro: number;
  mensaje: string;
};

export type RetiroHistorialItem = {
  idRetiroFondos: number;
  idTransaccion: number;
  operationId: string;
  monto: number;
  estadoPago: string;
  estadoEtiqueta: string;
  solicitadoEn: string;
  cuentaDestinoResumen: string | null;
};

export type CuentaBancariaPaseadorApi = {
  id?: string | null;
  bankName?: string | null;
  accountType?: string | null;
  accountNumberMasked?: string | null;
  holderName?: string | null;
};

export type CuentaBancariaPaseador = {
  id: string;
  bankName: string;
  accountType: string;
  accountNumberMasked: string;
  holderName: string;
};

/** Cuerpo para POST alta/actualización de cuenta bancaria del paseador (FK a catálogo). */
export type RegistroCuentaBancariaBody = {
  bancoId: number;
  tipoCuentaId: number;
  numeroCuenta: string;
};

export type BancoCatalogoItem = {
  id: number;
  nombre: string;
};

export type TipoCuentaCatalogoItem = {
  id: number;
  nombre: string;
};

export type CatalogoRegistroCuenta = {
  bancos: BancoCatalogoItem[];
  tiposCuenta: TipoCuentaCatalogoItem[];
};

function getSortTime(fecha: string, horaInicio: string): number {
  const iso = `${fecha}T${horaInicio || "00:00:00"}`;
  const value = new Date(iso).getTime();
  return Number.isFinite(value) ? value : 0;
}

function parseAmount(value: unknown): number {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string") {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) return parsed;
  }
  return 0;
}

type DesgloseComisionApi = {
  montoBruto?: number | string | null;
  comision?: number | string | null;
  montoNeto?: number | string | null;
};

type BilleteraReservaItemApi = {
  idReserva?: number | null;
  desgloseComision?: DesgloseComisionApi | null;
  montoBruto?: number | string | null;
  comision?: number | string | null;
  montoNeto?: number | string | null;
  estadoEtiqueta?: string | null;
  mascotaNombre?: string | null;
  tutorNombre?: string | null;
  fechaAgenda?: string | null;
  horaInicio?: string | null;
  nombreEstadoReserva?: string | null;
};

type BilleteraBucketApi = {
  key?: string | null;
  title?: string | null;
  helper?: string | null;
  amount?: number | string | null;
  grossAmount?: number | string | null;
  commissionAmount?: number | string | null;
  reservas?: BilleteraReservaItemApi[] | null;
};

type BilleteraProyeccionGrupoApi = {
  fechaDisponibleDesde?: string | null;
  totalNeto?: number | string | null;
  liberacionPausadaPorDisputa?: boolean | null;
  reservas?: BilleteraReservaItemApi[] | null;
};

type BilleteraResumenApi = {
  retenido?: BilleteraBucketApi | null;
  verificacion?: BilleteraBucketApi | null;
  disponible?: BilleteraBucketApi | null;
  historialLiberacionesDisponible?: BilleteraReservaItemApi[] | null;
  proyeccionLiberacionesPorDia?: BilleteraProyeccionGrupoApi[] | null;
  updatedAt?: string | null;
};

function readDesgloseMontos(item: BilleteraReservaItemApi): {
  montoBruto: number;
  comision: number;
  montoNeto: number;
} {
  const nested = item.desgloseComision;
  return {
    montoBruto: parseAmount(nested?.montoBruto ?? item.montoBruto),
    comision: parseAmount(nested?.comision ?? item.comision),
    montoNeto: parseAmount(nested?.montoNeto ?? item.montoNeto)
  };
}

function mapReservaItem(item: BilleteraReservaItemApi): BilleteraReservaItem {
  const montos = readDesgloseMontos(item);
  return {
    idReserva: Number(item.idReserva ?? 0),
    mascotaNombre: (item.mascotaNombre ?? "").trim() || "Mascota no disponible",
    tutorNombre: (item.tutorNombre ?? "").trim() || "Tutor no disponible",
    fecha: (item.fechaAgenda ?? "").trim(),
    horaInicio: (item.horaInicio ?? "").trim(),
    montoBruto: montos.montoBruto,
    comision: montos.comision,
    montoNeto: montos.montoNeto,
    estado: (item.nombreEstadoReserva ?? item.estadoEtiqueta ?? "").trim() || "Sin estado"
  };
}

function mapProyeccionGrupo(grupo: BilleteraProyeccionGrupoApi): BilleteraProyeccionLiberacionGrupo {
  const reservas =
    grupo.reservas?.map(mapReservaItem).filter((item) => item.idReserva > 0) ?? [];
  return {
    fechaDisponibleDesde: (grupo.fechaDisponibleDesde ?? "").trim(),
    totalNeto: parseAmount(grupo.totalNeto),
    liberacionPausadaPorDisputa: Boolean(grupo.liberacionPausadaPorDisputa),
    reservas
  };
}

function buildBucket(
  key: BilleteraBucketKey,
  title: string,
  helper: string,
  reservas: BilleteraReservaItem[]
): BilleteraBucket {
  return {
    key,
    title,
    helper,
    amount: 0,
    grossAmount: 0,
    commissionAmount: 0,
    reservas: [...reservas].sort(
      (a, b) => getSortTime(b.fecha, b.horaInicio) - getSortTime(a.fecha, a.horaInicio)
    )
  };
}

function mapBucket(
  key: BilleteraBucketKey,
  fallbackTitle: string,
  fallbackHelper: string,
  bucket: BilleteraBucketApi | null | undefined,
  defaultReservas: BilleteraReservaItem[] = []
): BilleteraBucket {
  const mapped = bucket?.reservas?.map(mapReservaItem).filter((item) => item.idReserva > 0) ?? [];
  // El backend envía disponible.reservas vacío y el detalle en historialLiberacionesDisponible;
  // `[]` no activa `??`, así que si no hay filas mapeadas usamos el fallback (historial).
  const reservas = mapped.length > 0 ? mapped : defaultReservas;
  const next = buildBucket(
    key,
    (bucket?.title ?? "").trim() || fallbackTitle,
    (bucket?.helper ?? "").trim() || fallbackHelper,
    reservas
  );
  let grossAmount = parseAmount(bucket?.grossAmount);
  let commissionAmount = parseAmount(bucket?.commissionAmount);
  if (reservas.length > 0 && grossAmount === 0 && commissionAmount === 0) {
    grossAmount = reservas.reduce((sum, item) => sum + item.montoBruto, 0);
    commissionAmount = reservas.reduce((sum, item) => sum + item.comision, 0);
  }
  return {
    ...next,
    amount: parseAmount(bucket?.amount),
    grossAmount,
    commissionAmount
  };
}

async function parseJsonSafe(response: Response): Promise<unknown> {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

function parseApiErrorMessage(data: unknown): string | null {
  if (!data || typeof data !== "object") return null;
  const d = data as { message?: string; detail?: string; title?: string };
  return d.detail?.trim() || d.message?.trim() || d.title?.trim() || null;
}

function mapCuentaBancaria(item: CuentaBancariaPaseadorApi): CuentaBancariaPaseador | null {
  const id = (item.id ?? "").trim();
  if (!id) return null;
  return {
    id,
    bankName: (item.bankName ?? "").trim() || "Banco",
    accountType: (item.accountType ?? "").trim() || "Cuenta",
    accountNumberMasked: (item.accountNumberMasked ?? "").trim() || "****",
    holderName: (item.holderName ?? "").trim() || "Titular registrado"
  };
}

type CatalogoRegistroCuentaApi = {
  bancos?: { id?: number | string | null; nombre?: string | null }[] | null;
  tiposCuenta?: { id?: number | string | null; nombre?: string | null }[] | null;
};

function mapCatalogoItem(
  row: { id?: number | string | null; nombre?: string | null } | null | undefined
): { id: number; nombre: string } | null {
  if (!row || row.id == null) return null;
  const id = Number(row.id);
  if (!Number.isFinite(id) || id <= 0) return null;
  const nombre = (row.nombre ?? "").trim();
  if (!nombre) return null;
  return { id, nombre };
}

export async function fetchCatalogoRegistroCuentaPaseador(): Promise<CatalogoRegistroCuenta> {
  const response = await fetch(API_ENDPOINTS.pagos.catalogoRegistroCuentaPaseador, {
    method: "GET",
    credentials: "include",
    headers: { ...bearerAuthHeaders() }
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    const detail = parseApiErrorMessage(data);
    throw new Error(detail || `No se pudo cargar el catalogo bancario (HTTP ${response.status}).`);
  }
  const dto = (data ?? {}) as CatalogoRegistroCuentaApi;
  const bancos =
    dto.bancos?.map(mapCatalogoItem).filter((b): b is BancoCatalogoItem => b != null) ?? [];
  const tiposCuenta =
    dto.tiposCuenta?.map(mapCatalogoItem).filter((t): t is TipoCuentaCatalogoItem => t != null) ??
    [];
  return { bancos, tiposCuenta };
}

export async function fetchCuentasBancariasPaseador(): Promise<CuentaBancariaPaseador[]> {
  const response = await fetch(API_ENDPOINTS.pagos.cuentasBancariasPaseador, {
    method: "GET",
    credentials: "include",
    headers: { ...bearerAuthHeaders() }
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    const detail = parseApiErrorMessage(data);
    throw new Error(detail || `No se pudieron cargar las cuentas bancarias (HTTP ${response.status}).`);
  }
  if (!Array.isArray(data)) {
    return [];
  }
  return (data as CuentaBancariaPaseadorApi[])
    .map(mapCuentaBancaria)
    .filter((c): c is CuentaBancariaPaseador => c != null);
}

export async function registrarCuentaBancariaPaseador(
  body: RegistroCuentaBancariaBody
): Promise<CuentaBancariaPaseador> {
  const response = await fetch(API_ENDPOINTS.pagos.cuentasBancariasPaseador, {
    method: "POST",
    credentials: "include",
    headers: { ...bearerAuthHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify({
      bancoId: body.bancoId,
      tipoCuentaId: body.tipoCuentaId,
      numeroCuenta: body.numeroCuenta.replace(/\s+/g, "").trim()
    })
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    const detail = parseApiErrorMessage(data);
    throw new Error(detail || `No se pudo guardar la cuenta bancaria (HTTP ${response.status}).`);
  }
  const acc = mapCuentaBancaria(data as CuentaBancariaPaseadorApi);
  if (!acc) {
    throw new Error("Respuesta de cuenta invalida.");
  }
  return acc;
}

export async function fetchBilleteraPaseador(): Promise<BilleteraPaseadorData> {
  const response = await fetch(API_ENDPOINTS.pagos.billeteraPaseador, {
    method: "GET",
    credentials: "include",
    headers: { ...bearerAuthHeaders() }
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    const detail = parseApiErrorMessage(data);
    throw new Error(detail || `No se pudo cargar la billetera (HTTP ${response.status}).`);
  }
  const dto = (data ?? {}) as BilleteraResumenApi;
  const historialDisponible =
    dto.historialLiberacionesDisponible
      ?.map(mapReservaItem)
      .filter((item) => item.idReserva > 0) ?? [];

  const proyeccionLiberacionesPorDia =
    dto.proyeccionLiberacionesPorDia?.map(mapProyeccionGrupo) ?? [];

  return {
    retenido: mapBucket(
      "retenido",
      "Saldo Retenido",
      "Servicios pagados que aun no han finalizado o siguen en curso.",
      dto.retenido
    ),
    verificacion: mapBucket(
      "verificacion",
      "Saldo en Verificacion",
      "Paseos finalizados que estan cumpliendo el periodo de liberacion N+2.",
      dto.verificacion
    ),
    disponible: mapBucket(
      "disponible",
      "Saldo Disponible",
      "Fondos liberados y listos para retiro desde tu billetera.",
      dto.disponible,
      historialDisponible
    ),
    proyeccionLiberacionesPorDia,
    updatedAt: (dto.updatedAt ?? "").trim() || new Date().toISOString()
  };
}

function mapRetiroHistorialItem(raw: unknown): RetiroHistorialItem | null {
  if (!raw || typeof raw !== "object") return null;
  const item = raw as {
    idRetiroFondos?: number | string | null;
    idTransaccion?: number | string | null;
    operationId?: string | null;
    monto?: number | string | null;
    estadoPago?: string | null;
    estadoEtiqueta?: string | null;
    solicitadoEn?: string | null;
    cuentaDestinoResumen?: string | null;
  };
  const idRetiroFondos = Number(item.idRetiroFondos ?? 0);
  const idTransaccion = Number(item.idTransaccion ?? 0);
  const operationId = (item.operationId ?? "").trim();
  if (!operationId && idTransaccion <= 0 && idRetiroFondos <= 0) {
    return null;
  }
  return {
    idRetiroFondos: Number.isFinite(idRetiroFondos) ? idRetiroFondos : 0,
    idTransaccion: Number.isFinite(idTransaccion) ? idTransaccion : 0,
    operationId: operationId || (idTransaccion > 0 ? `RET-${idTransaccion}` : `RET-${idRetiroFondos}`),
    monto: parseAmount(item.monto),
    estadoPago: (item.estadoPago ?? "").trim() || "PENDIENTE",
    estadoEtiqueta: (item.estadoEtiqueta ?? "").trim() || "Retiro en proceso",
    solicitadoEn: (item.solicitadoEn ?? "").trim(),
    cuentaDestinoResumen: (item.cuentaDestinoResumen ?? "").trim() || null
  };
}

export async function fetchHistorialRetirosPaseador(): Promise<RetiroHistorialItem[]> {
  const response = await fetch(API_ENDPOINTS.pagos.retiroPaseador, {
    method: "GET",
    credentials: "include",
    headers: { ...bearerAuthHeaders() }
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    const detail = parseApiErrorMessage(data);
    throw new Error(detail || `No se pudo cargar el historial de retiros (HTTP ${response.status}).`);
  }
  if (!Array.isArray(data)) {
    return [];
  }
  return data
    .map(mapRetiroHistorialItem)
    .filter((item): item is RetiroHistorialItem => item != null);
}

export async function solicitarRetiroPaseador(monto: number): Promise<RetiroPaseadorResult> {
  const response = await fetch(API_ENDPOINTS.pagos.retiroPaseador, {
    method: "POST",
    credentials: "include",
    headers: { ...bearerAuthHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify({ monto })
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    const detail = parseApiErrorMessage(data);
    throw new Error(detail || `No se pudo procesar el retiro (HTTP ${response.status}).`);
  }
  const dto = (data ?? {}) as {
    idTransaccion?: number | string | null;
    montoRetirado?: number | string | null;
    saldoDisponibleTrasRetiro?: number | string | null;
    mensaje?: string | null;
  };
  const idTransaccion = Number(dto.idTransaccion ?? 0);
  return {
    idTransaccion: Number.isFinite(idTransaccion) ? idTransaccion : 0,
    montoRetirado: parseAmount(dto.montoRetirado),
    saldoDisponibleTrasRetiro: parseAmount(dto.saldoDisponibleTrasRetiro),
    mensaje: (dto.mensaje ?? "").trim() || "Solicitud de retiro registrada."
  };
}
