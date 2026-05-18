package com.patiperro.tutores.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDTO {
    private String message;
    private String correo;
    /** PK en tutores-service (tabla tutor.id_tutor); también va en el JWT como claim tutorId. */
    private Long idTutor;
    /**
     * Mismo JWT que la cookie HttpOnly {@code access_token}. Lo expone el body para SPAs
     * (Vite en otro puerto) que no envían la cookie en peticiones cross-origin; el front debe
     * usar {@code Authorization: Bearer ...} hacia el gateway.
     */
    private String accessToken;
    private String nombreTutor;
}
