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
}
