package com.patiperro.mascota.user.dto; // CORREGIDO: se agrega .user

import lombok.Data;

@Data
public class RazaDTO {
    private Long idRaza;
    private String nombre;
}