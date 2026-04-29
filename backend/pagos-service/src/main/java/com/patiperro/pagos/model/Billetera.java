package com.patiperro.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "billetera")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Billetera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_billetera")
    private Long idBilletera;

    @Column(name = "id_usuario", nullable = false, unique = true)
    private Long idUsuario;

    @Builder.Default
    @Column(name = "saldo_actual", nullable = false, precision = 14, scale = 2)
    private BigDecimal saldoActual = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "saldo_retenido", nullable = false, precision = 14, scale = 2)
    private BigDecimal saldoRetenido = BigDecimal.ZERO;
}
