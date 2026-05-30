// Verificacion de identidad del paseador (PDF unico, aprobacion automatica).
import { API_ENDPOINTS } from "../../../config/api";

export type EstadoVerificacionIdentidad =
  | "SIN_ENVIAR"
  | "EN_PROCESO"
  | "APROBADO"
  | "RECHAZADO";

export type VerificacionIdentidadDTO = {
  estado: EstadoVerificacionIdentidad;
  estadoEtiqueta: string;
  puedeSubir: boolean;
  enviadoEn: string | null;
  revisadoEn: string | null;
  motivoRechazo: string | null;
  tieneDocumento: boolean;
  tieneFrontal: boolean;
  tieneReverso: boolean;
};

export const MAX_VERIFICACION_PDF_BYTES = 5 * 1024 * 1024;

type UploadProgressOptions = {
  onProgress?: (percent: number) => void;
};

function readErrorMessage(data: unknown, fallback: string): string {
  if (data && typeof data === "object") {
    const o = data as { message?: string; error?: string; mensaje?: string };
    const m = o.message ?? o.mensaje ?? o.error;
    if (typeof m === "string" && m.trim()) return m;
  }
  return fallback;
}

export function validarPdfVerificacion(file: File): string | null {
  if (!file) return "Selecciona un archivo PDF.";
  if (file.size > MAX_VERIFICACION_PDF_BYTES) {
    return "El archivo supera el tamano maximo permitido (5 MB).";
  }
  const name = file.name.toLowerCase();
  if (!name.endsWith(".pdf")) {
    return "Solo se permiten archivos PDF.";
  }
  const mime = file.type.toLowerCase().split(";")[0].trim();
  if (mime && mime !== "application/pdf") {
    return "Tipo de archivo no permitido (solo PDF).";
  }
  return null;
}

export async function fetchVerificacionEstado(): Promise<VerificacionIdentidadDTO> {
  const res = await fetch(API_ENDPOINTS.auth.paseadores.meVerificacion, {
    method: "GET",
    credentials: "include"
  });
  let data: unknown = null;
  try {
    data = await res.json();
  } catch {
    data = null;
  }
  if (res.status === 401 || res.status === 403) {
    throw new Error("Sesion requerida: inicia sesion como paseador.");
  }
  if (!res.ok) {
    throw new Error(readErrorMessage(data, "No se pudo cargar el estado de verificacion."));
  }
  return data as VerificacionIdentidadDTO;
}

export async function subirDocumentoVerificacion(
  file: File,
  options: UploadProgressOptions = {}
): Promise<VerificacionIdentidadDTO> {
  const validationError = validarPdfVerificacion(file);
  if (validationError) {
    throw new Error(validationError);
  }

  const body = new FormData();
  body.append("documento", file);

  return new Promise<VerificacionIdentidadDTO>((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("POST", API_ENDPOINTS.auth.paseadores.meVerificacionDocumento);
    xhr.withCredentials = true;

    xhr.upload.onprogress = (event) => {
      if (!event.lengthComputable) return;
      const percent = Math.min(100, Math.round((event.loaded / event.total) * 100));
      options.onProgress?.(percent);
    };

    xhr.onerror = () => {
      reject(new Error("No se pudo subir el documento."));
    };

    xhr.onload = () => {
      let data: unknown = null;
      try {
        data = xhr.responseText ? JSON.parse(xhr.responseText) : null;
      } catch {
        data = null;
      }

      if (xhr.status === 401 || xhr.status === 403) {
        reject(new Error("Sesion expirada. Vuelve a iniciar sesion como paseador."));
        return;
      }
      if (xhr.status < 200 || xhr.status >= 300) {
        reject(new Error(readErrorMessage(data, "No se pudo subir el documento.")));
        return;
      }

      options.onProgress?.(100);
      resolve(data as VerificacionIdentidadDTO);
    };

    xhr.send(body);
  });
}

export async function descargarDocumentoVerificacion(): Promise<Blob> {
  const res = await fetch(API_ENDPOINTS.auth.paseadores.meVerificacionDocumento, {
    method: "GET",
    credentials: "include"
  });
  if (res.status === 401 || res.status === 403) {
    throw new Error("Sesion requerida para descargar el documento.");
  }
  if (!res.ok) {
    let data: unknown = null;
    try {
      data = await res.json();
    } catch {
      data = null;
    }
    throw new Error(readErrorMessage(data, "No se pudo descargar el documento."));
  }
  return res.blob();
}
