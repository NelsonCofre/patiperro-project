package com.patiperro.tutores.auth.dto;

import lombok.Data;

@Data
public class LoginRequestDTO {
    private String correo;  // email del dueño (tutor)
    private String contrasena;
}
