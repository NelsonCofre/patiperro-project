package com.patiperro.reserva.dto.integracion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Subconjunto de {@code GET /api/tutores/{id}} en tutores-service (JWT HS256 compartido).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TutorReservaClientDTO {

    private Long id;
    private String primerNombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String telefono;
    private String correo;
    private String fotoPerfil;
    private String biografia;
    private DireccionTutorClientDTO direccion;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DireccionTutorClientDTO {
        private String comuna;
        private String ciudad;
        private String calle;
        private Integer numeracion;
        private String casaDepartamento;
        private Double latitud;
        private Double longitud;
    }
}
