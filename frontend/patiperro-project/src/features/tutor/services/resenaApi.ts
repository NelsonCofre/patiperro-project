// src/features/tutor/services/resenaApi.ts
import type { 
  ResenaRequest, 
  ResenaResponse, 
  ResenaDetalleDTO 
} from '../types/resena.types';

const BASE_URL = import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080/api';

export const resenaApi = {
  /**
   * Envía una nueva calificación al microservicio de reseñas.
   */
  crearResena: async (resena: ResenaRequest): Promise<ResenaResponse> => {
    const response = await fetch(`${BASE_URL}/resenas`, {
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
    const response = await fetch(`${BASE_URL}/resenas/paseador/${idPaseador}`);
    
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
    const response = await fetch(`${BASE_URL}/resenas/paseador/${idPaseador}/promedio`);
    
    if (!response.ok) return 0;
    
    return response.json();
  }
};