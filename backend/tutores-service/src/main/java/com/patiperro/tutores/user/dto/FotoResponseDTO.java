package com.patiperro.tutores.user.dto;

import com.patiperro.tutores.user.model.Foto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FotoResponseDTO {
    private Long id;
    private String url;

    public static FotoResponseDTO fromEntity(Foto foto) {
        return FotoResponseDTO.builder()
                .id(foto.getId())
                .url(foto.getUrl())
                .build();
    }
}
