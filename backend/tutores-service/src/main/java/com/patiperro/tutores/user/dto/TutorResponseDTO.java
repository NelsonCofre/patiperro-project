package com.patiperro.tutores.user.dto;

import com.patiperro.tutores.user.model.Direccion;
import com.patiperro.tutores.user.model.Tutor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class TutorResponseDTO {

    private Long id;
    private String rut;
    private String primerNombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String correo;
    private String fotoPerfil;
    private String biografia;
    private Direccion direccion;

    public static TutorResponseDTO fromEntity(Tutor tutor) {
        return TutorResponseDTO.builder()
                .id(tutor.getId())
                .rut(tutor.getRut())
                .primerNombre(tutor.getPrimerNombre())
                .segundoNombre(tutor.getSegundoNombre())
                .apellidoPaterno(tutor.getApellidoPaterno())
                .apellidoMaterno(tutor.getApellidoMaterno())
                .fechaNacimiento(tutor.getFechaNacimiento())
                .telefono(tutor.getTelefono())
                .correo(tutor.getCorreo())
                .fotoPerfil(tutor.getFotoPerfil())
                .biografia(tutor.getBiografia())
                .direccion(tutor.getDireccion())
                .build();
    }
}
