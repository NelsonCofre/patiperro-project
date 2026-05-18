package com.patiperro.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "retiro_fondos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetiroFondo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_retiro_fondos")
    private Long idRetiroFondos;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_transaccion", nullable = false, unique = true)
    private Transaccion transaccion;

    @Column(name = "id_usuario_paseador", nullable = false)
    private Long idUsuarioPaseador;

    @Column(name = "monto", nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;
}
