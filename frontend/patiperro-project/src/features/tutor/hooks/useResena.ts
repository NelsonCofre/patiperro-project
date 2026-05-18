// src/features/tutor/hooks/useResena.ts
import { useState } from 'react';
import { resenaApi } from '../services/resenaApi';
import type { ResenaRequest } from '../types/resena.types';

export const useResena = () => {
  const [error, setError] = useState<string | null>(null);

  const enviarResena = async (data: ResenaRequest) => {
    setError(null);
    try {
      await resenaApi.crearResena(data);
      // Aquí podrías disparar un Toast de éxito si lo tienes configurado
    } catch (err: any) {
      // Capturamos el mensaje que viene del ResponseEntity.body() del backend
      const mensajeError = err.message || "No se pudo enviar la reseña";
      setError(mensajeError);
      throw err; // Re-lanzamos para que el Modal también sepa que falló
    }
  };

  return { enviarResena, error, setError };
};