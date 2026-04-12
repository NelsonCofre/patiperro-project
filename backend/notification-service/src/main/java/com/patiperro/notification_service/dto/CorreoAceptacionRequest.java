package com.patiperro.notification_service.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para recibir los datos necesarios para el correo de aceptación.
 * Vigilancia: Solo transporta los datos mínimos para el envío, 
 * evitando dependencias con el microservicio de Usuarios.
 */
public record CorreoAceptacionRequest(
    Integer idUsuario,
    String correoTutor,
    Integer idPlantillaBrevo, // El ID que te dio Brevo
    String nombrePaseador,
    String nombreMascota,
    Double montoTotal,
    String nombreTutor
) {}