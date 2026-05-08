package com.patiperro.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "comprobante_pago",
        indexes = {
                @Index(name = "idx_comprobante_pago_reserva", columnList = "id_reserva"),
                @Index(name = "idx_comprobante_pago_creado_en", columnList = "creado_en DESC")
        }
)
public class ComprobantePago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comprobante")
    private Long idComprobante;

    @Column(name = "id_reserva", nullable = false, unique = true)
    private Long idReserva;

    @Column(name = "id_tutor_usuario")
    private Long idTutorUsuario;

    @Column(name = "id_transaccion_pagos")
    private Long idTransaccionPagos;

    @Column(name = "id_transaccion_externa", length = 128)
    private String idTransaccionExterna;

    @Column(name = "fecha_hora_operacion")
    private LocalDateTime fechaHoraOperacion;

    @Column(name = "paseador_nombre", length = 256)
    private String paseadorNombre;

    @Column(name = "mascota_nombre", length = 256)
    private String mascotaNombre;

    @Column(name = "fecha_paseo")
    private LocalDate fechaPaseo;

    @Column(name = "hora_inicio")
    private LocalDateTime horaInicio;

    @Column(name = "hora_final")
    private LocalDateTime horaFinal;

    @Column(name = "duracion_minutos")
    private Long duracionMinutos;

    @Column(name = "moneda", nullable = false, length = 8)
    private String moneda = "CLP";

    @Column(name = "monto_total", precision = 14, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "comision_app", precision = 14, scale = 2)
    private BigDecimal comisionApp;

    @Column(name = "monto_neto", precision = 14, scale = 2)
    private BigDecimal montoNeto;

    @Column(name = "estado_fondos", length = 512)
    private String estadoFondos;

    @Column(name = "tipo_documento", nullable = false, length = 64)
    private String tipoDocumento = "RESUMEN_TRANSACCION";

    @Column(name = "disclaimer_legal", length = 512)
    private String disclaimerLegal;

    @Column(name = "html_resumen", columnDefinition = "TEXT")
    private String htmlResumen;

    @Column(name = "json_snapshot", columnDefinition = "TEXT")
    private String jsonSnapshot;

    @Column(name = "email_destino", length = 320)
    private String emailDestino;

    @Column(name = "email_enviado_en")
    private LocalDateTime emailEnviadoEn;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    public Long getIdComprobante() {
        return idComprobante;
    }

    public void setIdComprobante(Long idComprobante) {
        this.idComprobante = idComprobante;
    }

    public Long getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(Long idReserva) {
        this.idReserva = idReserva;
    }

    public Long getIdTutorUsuario() {
        return idTutorUsuario;
    }

    public void setIdTutorUsuario(Long idTutorUsuario) {
        this.idTutorUsuario = idTutorUsuario;
    }

    public Long getIdTransaccionPagos() {
        return idTransaccionPagos;
    }

    public void setIdTransaccionPagos(Long idTransaccionPagos) {
        this.idTransaccionPagos = idTransaccionPagos;
    }

    public String getIdTransaccionExterna() {
        return idTransaccionExterna;
    }

    public void setIdTransaccionExterna(String idTransaccionExterna) {
        this.idTransaccionExterna = idTransaccionExterna;
    }

    public LocalDateTime getFechaHoraOperacion() {
        return fechaHoraOperacion;
    }

    public void setFechaHoraOperacion(LocalDateTime fechaHoraOperacion) {
        this.fechaHoraOperacion = fechaHoraOperacion;
    }

    public String getPaseadorNombre() {
        return paseadorNombre;
    }

    public void setPaseadorNombre(String paseadorNombre) {
        this.paseadorNombre = paseadorNombre;
    }

    public String getMascotaNombre() {
        return mascotaNombre;
    }

    public void setMascotaNombre(String mascotaNombre) {
        this.mascotaNombre = mascotaNombre;
    }

    public LocalDate getFechaPaseo() {
        return fechaPaseo;
    }

    public void setFechaPaseo(LocalDate fechaPaseo) {
        this.fechaPaseo = fechaPaseo;
    }

    public LocalDateTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalDateTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalDateTime getHoraFinal() {
        return horaFinal;
    }

    public void setHoraFinal(LocalDateTime horaFinal) {
        this.horaFinal = horaFinal;
    }

    public Long getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(Long duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public BigDecimal getComisionApp() {
        return comisionApp;
    }

    public void setComisionApp(BigDecimal comisionApp) {
        this.comisionApp = comisionApp;
    }

    public BigDecimal getMontoNeto() {
        return montoNeto;
    }

    public void setMontoNeto(BigDecimal montoNeto) {
        this.montoNeto = montoNeto;
    }

    public String getEstadoFondos() {
        return estadoFondos;
    }

    public void setEstadoFondos(String estadoFondos) {
        this.estadoFondos = estadoFondos;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getDisclaimerLegal() {
        return disclaimerLegal;
    }

    public void setDisclaimerLegal(String disclaimerLegal) {
        this.disclaimerLegal = disclaimerLegal;
    }

    public String getHtmlResumen() {
        return htmlResumen;
    }

    public void setHtmlResumen(String htmlResumen) {
        this.htmlResumen = htmlResumen;
    }

    public String getJsonSnapshot() {
        return jsonSnapshot;
    }

    public void setJsonSnapshot(String jsonSnapshot) {
        this.jsonSnapshot = jsonSnapshot;
    }

    public String getEmailDestino() {
        return emailDestino;
    }

    public void setEmailDestino(String emailDestino) {
        this.emailDestino = emailDestino;
    }

    public LocalDateTime getEmailEnviadoEn() {
        return emailEnviadoEn;
    }

    public void setEmailEnviadoEn(LocalDateTime emailEnviadoEn) {
        this.emailEnviadoEn = emailEnviadoEn;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }
}

