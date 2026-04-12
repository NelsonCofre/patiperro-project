package com.patiperro.agenda.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "agenda_bloqueo_dia",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_agenda_bloqueo_dia_usuario_fecha",
                        columnNames = {"id_usuario", "fecha"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgendaBloqueoDia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bloqueo")
    private Integer idBloqueo;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "motivo", length = 120)
    private String motivo;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
    }
}
