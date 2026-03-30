package com.patiperro.mascota.auth.dto;
import lombok.Data;

@Data
public class RegisterRequestDTO {
    private String nombre;
    private String email;
    private String password;
}