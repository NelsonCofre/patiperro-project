package com.patiperro.mascota.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mascota")
@Data
public class Mascota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mascota")
    private Long idMascota;

    /** Lo fija el backend desde el JWT; el cliente no debe enviarlo en el JSON. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "id_tutor", nullable = false)
    private Long idTutor;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false, length = 60)
    private String nombre;

    @NotNull(message = "El peso es obligatorio")
    @DecimalMin(value = "0.01", message = "El peso debe ser mayor a 0")
    @Digits(integer = 3, fraction = 2, message = "El peso admite hasta 5 dígitos en total con 2 decimales")
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal peso;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @PastOrPresent(message = "La fecha de nacimiento no puede ser futura")
    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(length = 10)
    private String sexo;

    @Column(length = 60)
    private String comportamiento;

    @Column(length = 250)
    private String descripcion;

    @Column(name = "cuidados_especiales", length = 500)
    private String cuidadosEspeciales;

    @Column(name = "esterilizado")
    private Boolean esterilizado;

    @Column(name = "numero_chip", length = 50)
    private String numeroChip;

    @NotNull(message = "La especie es obligatoria")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "especie_id_especie", nullable = false)
    private Especie especie;

    @NotNull(message = "La raza es obligatoria")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "raza_id_raza", nullable = false)
    private Raza raza;

    @NotNull(message = "El tamaño es obligatorio")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tamano_id_tamano", nullable = false)
    private Tamano tamano;

    @OneToMany(mappedBy = "mascota", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<MascotaFoto> mascotaFotos = new ArrayList<>();
}
