package com.patiperro.reserva.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reserva")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Integer idReserva;

    @Column(name = "id_tutor_usuario", nullable = false)
    private Integer idTutorUsuario;

    @Column(name = "id_mascota", nullable = false)
    private Integer idMascota;

    @Column(name = "id_agenda_bloque", nullable = false)
    private Integer idAgendaBloque;

    @Column(name = "id_tarifa", nullable = false)
    private Integer idTarifa;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    /** Instantánea cuando el paseador acepta (línea de tiempo tutor). */
    @Column(name = "fecha_aceptacion")
    private LocalDateTime fechaAceptacion;

    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    /**
     * Id de la fila {@code transaccion} en pagos-service ({@code transaccion.id_transaccion}).
     * Los datos de Mercado Pago viven en {@code pago_externo} / transacción en la BD de pagos.
     */
    @Column(name = "id_pago")
    private Long idPago;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_reserva_id_estado_reserva", nullable = false)
    private EstadoReserva estadoReserva;

    @Column(name = "fecha_inicio_real")
    private LocalDateTime fechaInicioReal;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "codigo_encuentro")
    private Integer codigoEncuentro;

    /**
     * Instante hasta el cual el PIN de encuentro es válido (p. ej. inicio programado del paseo + ventana de minutos).
     * {@code null} si aún no hay código o no aplica.
     */
    @Column(name = "codigo_encuentro_expira_en")
    private LocalDateTime codigoEncuentroExpiraEn;

    /** Intentos fallidos con PIN incorrecto; {@code null} equivale a 0 en negocio. */
    @Column(name = "codigo_intentos_fallidos")
    private Integer codigoIntentosFallidos;

    /** Tras superar el máximo de intentos: instante hasta el cual no se permiten nuevos intentos. */
    @Column(name = "codigo_bloqueado_hasta")
    private LocalDateTime codigoBloqueadoHasta;

    @Column(name = "motivo_rechazo", length = 120)
    private String motivoRechazo;

    @Column(name = "detalle_rechazo", length = 500)
    private String detalleRechazo;
}
