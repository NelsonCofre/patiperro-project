import { bearerAuthHeaders } from "../../../config/authHeaders";

// 1. Definimos cómo luce nuestro DTO en TypeScript
export interface NotificacionEventoRequest {
  emailDestino: string;
  tipoEvento: "RESERVA_ACEPTADA" | "RESERVA_RECHAZADA" | "SOLICITUD_PASEO"; // Ayuda al autocompletado
  variables: Record<string, any>; // Esto equivale al Map<String, Object> de Java
}

// 2. Creamos la función que hace el POST
export async function dispararNotificacion(datos: NotificacionEventoRequest): Promise<void> {
  const url = "http://localhost:8086/api/notificaciones/disparar-evento"; 

  console.log("DEBUG: Enviando payload a notificaciones:", JSON.stringify(datos, null, 2));

  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...bearerAuthHeaders()
    },
    body: JSON.stringify(datos)
  });

  if (!response.ok) {
    const errorText = await response.text();
    console.error("DEBUG: Error del servidor de notificaciones:", errorText);
    throw new Error("No se pudo enviar la notificación.");
  }
  console.log("DEBUG: ¡Notificación enviada con éxito!");
}
