package com.patiperro.notification_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificacionEventoRequest {

    @NotBlank(message = "El correo de destino es obligatorio")
    @Email(message = "El formato del correo no es válido")
    private String emailDestino;

    @NotBlank(message = "El tipo de evento es obligatorio")
    private String tipoEvento; // Ej: "RESERVA_ACEPTADA", "RESERVA_RECHAZADA", "BIENVENIDA_TUTOR"

    @NotNull(message = "Las variables de la plantilla no pueden ser nulas")
    private Map<String, Object> variables; // Diccionario de datos para Brevo
}