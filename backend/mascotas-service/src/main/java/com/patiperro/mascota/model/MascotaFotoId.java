package com.patiperro.mascota.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MascotaFotoId implements Serializable {

    @Column(name = "foto_id_foto", nullable = false)
    private Long fotoIdFoto;

    @Column(name = "mascota_id_mascota", nullable = false)
    private Long mascotaIdMascota;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MascotaFotoId that = (MascotaFotoId) o;
        return Objects.equals(fotoIdFoto, that.fotoIdFoto)
                && Objects.equals(mascotaIdMascota, that.mascotaIdMascota);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fotoIdFoto, mascotaIdMascota);
    }
}
