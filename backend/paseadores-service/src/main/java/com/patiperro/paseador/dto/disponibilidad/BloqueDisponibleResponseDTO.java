package com.patiperro.paseador.dto.disponibilidad;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Un bloque de agenda disponible dentro de un día (respuesta pública del paseador). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloqueDisponibleResponseDTO {

    private Integer idAgenda;
    private LocalDateTime horaInicio;
    private LocalDateTime horaFinal;
}
