package com.patiperro.mascota.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period; // Necesario para calcular la edad
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

    // =========================================================================
    // SEGURIDAD Y VIGILANCIA
    // =========================================================================

    /** Lo fija el backend desde el JWT; el cliente no debe enviarlo en el JSON. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) // El backend solo leerá el ID del tutor desde el Token JWT, ignorando lo que el usuario envíe manualmente en ese campo. //
    @Column(name = "id_tutor", nullable = false)
    private Long idTutor;

    // =========================================================================
    // INICIO DE VALIDACIONES DE BACKEND (Criterios de Aceptación)
    // =========================================================================

    @NotBlank(message = "El nombre es obligatorio") // El nombre es obligatorio //
    @Size(min = 2, max = 60, message = "El nombre debe tener entre 2 y 60 caracteres") // El tamaño es entre 2 y 60 incluyendolos //
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$", message = "El nombre solo puede contener letras y espacios") // Validación avanzada: Evita números o símbolos en el nombre //
    @Column(nullable = false, length = 60)
    private String nombre;

    @NotNull(message = "El peso es obligatorio") // El peso es obligatorio //
    @DecimalMin(value = "0.1", message = "El peso debe ser un número válido mayor a 0 kg") // El valor comienza desde 0.1 //
    @DecimalMax(value = "150.0", message = "El peso no puede exceder los 150 kg") // Validación lógica para evitar errores de digitación masivos //
    @Digits(integer = 3, fraction = 2, message = "El peso admite hasta 3 enteros y 2 decimales") // Se limitan los números. 3 enteros con 2 decimales //
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal peso;

    @NotNull(message = "La fecha de nacimiento es obligatoria") // La fecha de nacimiento es obligatoria //
    @PastOrPresent(message = "La fecha de nacimiento no puede ser futura") // La fecha ingresada solo puede ser de hoy hacía atras //
    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @NotBlank(message = "Debe especificar el sexo") // El sexo es obligatorio //
    @Pattern(regexp = "^(Macho|Hembra)$", message = "El sexo debe ser 'Macho' o 'Hembra'") // Validación de integridad: Garantiza que no se envíen valores extraños //
    @Column(length = 10, nullable = false)
    private String sexo;

    @NotNull(message = "La especie es obligatoria") // La especie es obligatoria //
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "especie_id_especie", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "razas"})
    private Especie especie;

    @NotNull(message = "La raza es obligatoria") // La raza es obligatoria //
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "raza_id_raza", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Raza raza;

    @NotNull(message = "El tamaño es obligatorio") // El tamano es obligatoria //
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tamano_id_tamano", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Tamano tamano;

    // =========================================================================
    // CAMPOS OPCIONALES Y DE BIENESTAR
    // =========================================================================

    @Size(max = 60, message = "El comportamiento debe ser breve") // El comportamiento es opcional //
    @Column(length = 60)
    private String comportamiento;

    @Size(max = 500, message = "Los cuidados especiales no pueden exceder los 500 caracteres") // Límite de texto para descripciones largas //
    @Column(name = "cuidados_especiales", length = 500)
    private String cuidadosEspeciales;

    @Size(max = 250, message = "La descripción no puede exceder los 250 caracteres") // Límite de texto para el resumen //
    @Column(length = 250)
    private String descripcion;

    @Column(name = "esterilizado") // Campo booleano para salud //
    private Boolean esterilizado;

    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "El número de chip solo debe contener caracteres alfanuméricos") // Validación de formato de chip //
    @Column(name = "numero_chip", length = 50)
    private String numeroChip;

    /** Solo lectura en JSON; se asigna vía multipart en {@code /api/mascotas/{id}/foto-perfil}. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "foto_perfil", length = 150)
    private String fotoPerfil;

    /** Galería aparte: {@code GET /api/mascotas/{id}/fotos}. No serializar en el JSON de la mascota. */
    @OneToMany(mappedBy = "mascota", cascade = CascadeType.ALL, orphanRemoval = true) // Relación con galería de fotos //
    @JsonManagedReference
    @JsonIgnore
    private List<Foto> fotos = new ArrayList<>();

    // =========================================================================
    // LÓGICA DE NEGOCIO (Más que un CRUD genérico)
    // =========================================================================

    /**
     * Calcula la edad dinámicamente para el paseador.
     * Cumple con el criterio: "Frontend debe mostrar automáticamente la edad en años o meses".
     * Al ser @Transient, no se guarda en la base de datos (se calcula al vuelo).
     */
    @Transient
    public String getEdadFormateada() {
        if (this.fechaNacimiento == null) return "Edad no especificada";
        
        Period periodo = Period.between(this.fechaNacimiento, LocalDate.now());
        
        if (periodo.getYears() > 0) {
            return periodo.getYears() + (periodo.getYears() == 1 ? " año " : " años ") + 
                   periodo.getMonths() + " meses";
        } else {
            return periodo.getMonths() + (periodo.getMonths() == 1 ? " mes" : " meses");
        }
    }
}