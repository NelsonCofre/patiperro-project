// Cliente HTTP del microservicio mascotas (siempre vía gateway + cookie de sesión tutor).
import { API_ENDPOINTS } from "../../../config/api";
import type { MascotaForm } from "../types/mascota.types";

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
  idMascota?: number;
  nombre?: string;
  fotoPerfil?: string | null;
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
    credentials: "include"
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudieron cargar las especies."));
  }
  return data as EspecieDTO[];
}

export async function fetchRazas(especieId: number): Promise<RazaDTO[]> {
  const response = await fetch(API_ENDPOINTS.mascotas.razas(especieId), {
    credentials: "include"
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudieron cargar las razas."));
  }
  return data as RazaDTO[];
}

export async function fetchTamanos(): Promise<TamanoDTO[]> {
  const response = await fetch(API_ENDPOINTS.mascotas.tamanos, {
    credentials: "include"
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
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudo registrar la mascota."));
  }
  return data as MascotaCreatedDTO;
}

