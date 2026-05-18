package com.patiperro.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "comprobante_pago")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComprobantePago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comprobante")
    private Long idComprobante;

    @Column(name = "id_reserva", nullable = false, unique = true)
    private Long idReserva;

    @Column(name = "id_transaccion", nullable = false)
    private Long idTransaccion;

    @Column(name = "id_tutor_usuario", nullable = false)
    private Long idTutorUsuario;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    /** Formato de {@link #snapshotJson}; alinear con migraciones Flyway. */
    @Column(name = "snapshot_schema_version", nullable = false)
    private Integer snapshotSchemaVersion;

    @Column(name = "correo_enviado_en")
    private LocalDateTime correoEnviadoEn;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    @PrePersist
    void prePersist() {
        if (fechaGeneracion == null) {
            fechaGeneracion = LocalDateTime.now();
        }
        if (snapshotSchemaVersion == null) {
            snapshotSchemaVersion = ComprobantePagoSnapshotSchema.CURRENT;
        }
    }
}
