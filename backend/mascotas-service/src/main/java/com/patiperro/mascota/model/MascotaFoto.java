package com.patiperro.mascota.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mascota_foto")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MascotaFoto {

    @EmbeddedId
    @JsonIgnore
    private MascotaFotoId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("mascotaIdMascota")
    @JoinColumn(name = "mascota_id_mascota", nullable = false)
    @JsonBackReference
    private Mascota mascota;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("fotoIdFoto")
    @JoinColumn(name = "foto_id_foto", nullable = false)
    private Foto foto;
}
