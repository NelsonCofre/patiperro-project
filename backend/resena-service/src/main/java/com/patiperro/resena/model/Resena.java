package com.patiperro.resena.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "resenas")
@Data
public class Resena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer idReserva; // Vinculación obligatoria (Criterio 4)

    private Long idPaseador; // Para actualizar su promedio después

    private Integer estrellas; // Validación obligatoria (Criterio 3)

    private String comentario; // Opcional (Criterio 3)

    private Long idTutor;
}