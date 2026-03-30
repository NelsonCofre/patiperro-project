package com.patiperro.mascota.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder; // ESTA ES LA IMPORTANTE
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder // Permite usar AuthResponseDTO.builder()
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDTO {
    private String token;
}