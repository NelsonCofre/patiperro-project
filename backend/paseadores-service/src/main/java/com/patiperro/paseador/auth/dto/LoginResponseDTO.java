package com.patiperro.paseador.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDTO {
    private String message;
    private String correo;
    /** PK en paseadores-service (tabla paseador.id_paseador); el front la usa para /api/agenda/bloques/usuario/{id}. */
    private Long idPaseador;
    /** Mismo JWT que cookie {@code access_token}; para SPAs que envían {@code Authorization: Bearer}. */
    private String accessToken;
}
