// src/features/tutor/services/resenaApi.ts
import { API_BASE_URL } from "../../../config/api";
import type {
  ResenaRequest,
  ResenaResponse,
  ResenaDetalleDTO
} from "../types/resena.types";

const API_PREFIX = API_BASE_URL ? `${API_BASE_URL.replace(/\/$/, "")}/api` : "/api";

export const resenaApi = {
  /**
   * Envía una nueva calificación al microservicio de reseñas.
   */
  crearResena: async (resena: ResenaRequest): Promise<ResenaResponse> => {
    const response = await fetch(`${API_PREFIX}/resenas`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(resena),
    });

    if (!response.ok) {
      const errorMsg = await response.text();
      console.error("Error del servidor:", errorMsg); 
      throw new Error(errorMsg || 'Error al enviar la reseña');
    }

    return response.json();
  },

  /**
   * AC #3: Obtiene el historial de reseñas detallado de un paseador.
   * Utiliza el endpoint: GET /api/resenas/paseador/{idPaseador}
   */
  obtenerResenasPorPaseador: async (idPaseador: number): Promise<ResenaDetalleDTO[]> => {
    const response = await fetch(`${API_PREFIX}/resenas/paseador/${idPaseador}`);
    
    if (!response.ok) {
      throw new Error('No se pudieron cargar las reseñas del paseador');
    }
    
    return response.json();
  },

  /**
   * AC #4: Obtiene el promedio de reputación del paseador.
   * Utiliza el endpoint: GET /api/resenas/paseador/{idPaseador}/promedio
   */
  obtenerPromedioPaseador: async (idPaseador: number): Promise<number> => {
    const response = await fetch(`${API_PREFIX}/resenas/paseador/${idPaseador}/promedio`);
    
    if (!response.ok) return 0;
    
    return response.json();
  },

  /** IDs de reserva que el tutor ya calificó. */
  obtenerReservasCalificadasPorTutor: async (idTutor: number): Promise<number[]> => {
    const response = await fetch(`${API_PREFIX}/resenas/tutor/${idTutor}/reservas-calificadas`);
    if (!response.ok) return [];
    const data = await response.json();
    return Array.isArray(data) ? data.map((id) => Number(id)).filter((id) => Number.isFinite(id)) : [];
  }
};