package com.patiperro.mascota.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FotoUrlRequest(
        @NotBlank @Size(max = 150) String url
) {
}
