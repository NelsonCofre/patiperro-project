// Cliente HTTP del microservicio mascotas (siempre vía gateway + cookie de sesión tutor).
import { API_ENDPOINTS } from "../../../config/api";
import { resolveApiUrl } from "../../../config/api";
import { bearerAuthHeaders } from "../../../config/authHeaders";
import type {
  MascotaEditorData,
  MascotaForm,
  MascotaListItem
} from "../types/mascota.types";

type ApiErrorBody = { message?: string; mensaje?: string };

function readApiErrorMessage(data: unknown, fallback: string): string {
  if (data && typeof data === "object") {
    const o = data as ApiErrorBody;
    if (typeof o.message === "string" && o.message.trim()) return o.message;
    if (typeof o.mensaje === "string" && o.mensaje.trim()) return o.mensaje;
  }
  return fallback;
}

export type EspecieDTO = {
  idEspecie: number;
  nombre: string;
};

export type RazaDTO = {
  idRaza: number;
  nombre: string;
};

export type TamanoDTO = {
  idTamano: number;
  nombre: string;
  descripcion?: string | null;
};

export type MascotaCreatedDTO = {
  idMascota: number;
  nombre: string;
  fotoPerfil?: string | null;
};

type MascotaApiDTO = {
  idMascota?: number;
  nombre?: string;
  peso?: number | string | null;
  fechaNacimiento?: string | null;
  sexo?: string | null;
  comportamiento?: string | null;
  descripcion?: string | null;
  cuidadosEspeciales?: string | null;
  esterilizado?: boolean | null;
  numeroChip?: string | null;
  fotoPerfil?: string | null;
  edadFormateada?: string | null;
  especie?: { idEspecie?: number; nombre?: string | null } | null;
  raza?: { idRaza?: number; nombre?: string | null } | null;
  tamano?: { idTamano?: number; nombre?: string | null } | null;
};

async function parseJsonSafe(response: Response): Promise<unknown> {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

export async function fetchEspecies(): Promise<EspecieDTO[]> {
  const response = await fetch(API_ENDPOINTS.mascotas.especies, {
    credentials: "include",
    headers: { ...bearerAuthHeaders() }
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudieron cargar las especies."));
  }
  return data as EspecieDTO[];
}

export async function fetchRazas(especieId: number): Promise<RazaDTO[]> {
  const response = await fetch(API_ENDPOINTS.mascotas.razas(especieId), {
    credentials: "include",
    headers: { ...bearerAuthHeaders() }
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudieron cargar las razas."));
  }
  return data as RazaDTO[];
}

export async function fetchTamanos(): Promise<TamanoDTO[]> {
  const response = await fetch(API_ENDPOINTS.mascotas.tamanos, {
    credentials: "include",
    headers: { ...bearerAuthHeaders() }
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudieron cargar los tamaños."));
  }
  return data as TamanoDTO[];
}

export type CreateMascotaPayload = {
  nombre: string;
  peso: number;
  fechaNacimiento: string;
  sexo?: string;
  comportamiento: string;
  descripcion?: string;
  cuidadosEspeciales?: string;
  esterilizado: boolean;
  numeroChip?: string;
  fotoPerfil?: string;
  especie: { idEspecie: number };
  raza: { idRaza: number };
  tamano: { idTamano: number };
};

function formatPesoValue(value: number | string | null | undefined): string {
  if (value == null) return "";
  const asNumber = Number(value);
  if (!Number.isFinite(asNumber)) {
    return String(value).trim();
  }
  return Number.isInteger(asNumber) ? String(asNumber) : String(asNumber);
}

function normalizeMascotaResponse(data: unknown): MascotaCreatedDTO {
  const row = (data ?? {}) as MascotaApiDTO;
  return {
    idMascota: Number(row.idMascota ?? 0),
    nombre: String(row.nombre ?? "Mascota"),
    fotoPerfil: row.fotoPerfil ?? null
  };
}

function mapMascotaToListItem(row: MascotaApiDTO): MascotaListItem {
  const fotoPerfilPath = String(row.fotoPerfil ?? "").trim();
  return {
    idMascota: Number(row.idMascota ?? 0),
    nombre: String(row.nombre ?? "Mascota"),
    especieNombre: String(row.especie?.nombre ?? ""),
    razaNombre: String(row.raza?.nombre ?? ""),
    tamanoNombre: String(row.tamano?.nombre ?? ""),
    sexo: String(row.sexo ?? ""),
    edadFormateada: String(row.edadFormateada ?? ""),
    fotoPerfilPath,
    fotoPerfilUrl: fotoPerfilPath ? resolveApiUrl(fotoPerfilPath) : ""
  };
}

function mapMascotaToForm(row: MascotaApiDTO): MascotaForm {
  return {
    nombre: String(row.nombre ?? ""),
    especie: row.especie?.idEspecie ? String(row.especie.idEspecie) : "",
    raza: row.raza?.idRaza ? String(row.raza.idRaza) : "",
    sexo: String(row.sexo ?? ""),
    fecha_nacimiento: String(row.fechaNacimiento ?? "").slice(0, 10),
    peso: formatPesoValue(row.peso),
    tamano: row.tamano?.idTamano ? String(row.tamano.idTamano) : "",
    comportamiento: String(row.comportamiento ?? ""),
    descripcion: String(row.descripcion ?? ""),
    cuidados_especiales: String(row.cuidadosEspeciales ?? ""),
    esterilizado: row.esterilizado ? "Si" : "No",
    numero_chip: String(row.numeroChip ?? ""),
    foto: null
  };
}

export function buildCreateMascotaPayload(
  form: MascotaForm,
  fotoPerfilPath?: string | null
): CreateMascotaPayload {
  const chip = form.numero_chip.trim();
  const esterilizado = form.esterilizado === "Si";
  return {
    nombre: form.nombre.trim(),
    peso: Number.parseFloat(form.peso),
    fechaNacimiento: form.fecha_nacimiento,
    sexo: form.sexo.trim() || undefined,
    comportamiento: form.comportamiento.trim(),
    descripcion: form.descripcion.trim() || undefined,
    cuidadosEspeciales: form.cuidados_especiales.trim() || undefined,
    esterilizado,
    numeroChip: chip || undefined,
    fotoPerfil: fotoPerfilPath?.trim() || undefined,
    especie: { idEspecie: Number(form.especie) },
    raza: { idRaza: Number(form.raza) },
    tamano: { idTamano: Number(form.tamano) }
  };
}

export async function createMascota(payload: CreateMascotaPayload): Promise<MascotaCreatedDTO> {
  const response = await fetch(API_ENDPOINTS.mascotas.base, {
    method: "POST",
    credentials: "include",
    headers: { ...bearerAuthHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudo registrar la mascota."));
  }
  return normalizeMascotaResponse(data);
}

export async function fetchMisMascotas(): Promise<MascotaListItem[]> {
  const response = await fetch(API_ENDPOINTS.mascotas.mias, {
    method: "GET",
    credentials: "include",
    headers: { ...bearerAuthHeaders() }
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudieron cargar tus mascotas."));
  }
  if (!Array.isArray(data)) return [];
  return data.map((row) => mapMascotaToListItem((row ?? {}) as MascotaApiDTO));
}

export async function fetchMascotaById(idMascota: number): Promise<MascotaEditorData> {
  const response = await fetch(API_ENDPOINTS.mascotas.byId(idMascota), {
    method: "GET",
    credentials: "include",
    headers: { ...bearerAuthHeaders() }
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudo cargar la mascota."));
  }
  const row = (data ?? {}) as MascotaApiDTO;
  const fotoPerfilPath = String(row.fotoPerfil ?? "").trim();
  return {
    idMascota: Number(row.idMascota ?? idMascota),
    form: mapMascotaToForm(row),
    especieNombre: String(row.especie?.nombre ?? ""),
    razaNombre: String(row.raza?.nombre ?? ""),
    tamanoNombre: String(row.tamano?.nombre ?? ""),
    edadFormateada: String(row.edadFormateada ?? ""),
    fotoPerfilPath,
    fotoPerfilUrl: fotoPerfilPath ? resolveApiUrl(fotoPerfilPath) : ""
  };
}

export async function updateMascota(
  idMascota: number,
  payload: CreateMascotaPayload
): Promise<MascotaCreatedDTO> {
  const response = await fetch(API_ENDPOINTS.mascotas.byId(idMascota), {
    method: "PUT",
    credentials: "include",
    headers: { ...bearerAuthHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudo actualizar la mascota."));
  }
  return normalizeMascotaResponse(data);
}
