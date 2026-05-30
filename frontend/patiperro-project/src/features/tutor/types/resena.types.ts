// src/features/tutor/types/resena.types.ts

export interface ResenaRequest {
  idReserva: number;
  idTutor: number;
  idPaseador: number;
  estrellas: number;
  comentario?: string;
}

export interface ResenaResponse extends ResenaRequest {
  id: number;
  fechaCreacion: string;
}

export interface ResenaDetalleDTO {
  id: number; // Identificador único de la reseña
  idReserva: number;
  estrellas: number; // AC #2: Calificación numérica
  nombreTutor: string; // AC #2: Nombre del Tutor (Nombre + Apellido)
  comentario?: string; // AC #2: Comentario de texto
}