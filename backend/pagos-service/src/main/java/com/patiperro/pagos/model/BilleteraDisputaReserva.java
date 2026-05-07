package com.patiperro.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Disputa sobre una reserva con cobro en billetera: si {@link #disputaActiva}, no se libera verificación → disponible.
 */
@Entity
@Table(name = "billetera_disputa_reserva")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BilleteraDisputaReserva {

    @Id
    @Column(name = "id_reserva", nullable = false)
    private Integer idReserva;

    @Column(name = "disputa_activa", nullable = false)
    private boolean disputaActiva;

    @Column(name = "motivo", length = 512)
    private String motivo;

    @Column(name = "abierto_en")
    private LocalDateTime abiertoEn;

    @Column(name = "cerrado_en")
    private LocalDateTime cerradoEn;
}
