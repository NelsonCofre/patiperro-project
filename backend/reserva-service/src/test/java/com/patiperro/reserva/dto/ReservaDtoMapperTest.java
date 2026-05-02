package com.patiperro.reserva.dto;

import com.patiperro.reserva.dto.integracion.TutorReservaClientDTO;
import com.patiperro.reserva.model.EstadoReserva;
import com.patiperro.reserva.model.Reserva;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReservaDtoMapperTest {

    @Test
    void toTutorDetalleResponse_mapsEntityAndFlags() {
        EstadoReserva estado = new EstadoReserva();
        estado.setIdEstadoReserva(2);
        estado.setNombreEstado("PENDIENTE_PAGO");

        Reserva r = new Reserva();
        r.setIdReserva(10);
        r.setIdTutorUsuario(20);
        r.setIdMascota(30);
        r.setIdAgendaBloque(40);
        r.setMontoTotal(new BigDecimal("15000.00"));
        r.setMercadopagoPaymentId("mp-1");
        r.setEstadoReserva(estado);
        r.setFechaSolicitud(LocalDateTime.parse("2026-05-01T10:00:00"));
        r.setCodigoEncuentro(1234);
        r.setMercadopagoUltimoEstado("rejected");
        r.setMercadopagoUltimoEstadoDetalle("cc_rejected");
        r.setMercadopagoUltimoEstadoEn(LocalDateTime.parse("2026-05-01T12:00:00"));

        AgendaBloqueResumenDTO bloque = new AgendaBloqueResumenDTO();
        bloque.setIdUsuario(99);
        bloque.setFecha(LocalDate.parse("2026-05-02"));
        bloque.setHoraInicio(LocalDateTime.parse("2026-05-02T15:00:00"));
        bloque.setHoraFinal(LocalDateTime.parse("2026-05-02T16:00:00"));

        MascotaResumenDTO mascota = new MascotaResumenDTO();
        mascota.setNombre("Firulais");

        PaseadorResumenDTO paseador = new PaseadorResumenDTO();
        paseador.setNombreCompleto("Ana P.");

        TutorReservaClientDTO tutor = new TutorReservaClientDTO();
        tutor.setPrimerNombre("Pepe");
        tutor.setApellidoPaterno("Grillo");
        tutor.setCorreo("pepe@example.com");

        ReservaTutorDetalleResponseDTO dto = ReservaDtoMapper.toTutorDetalleResponse(
                r, bloque, mascota, paseador, tutor, true);

        assertThat(dto.getIdReserva()).isEqualTo(10);
        assertThat(dto.getNombreEstado()).isEqualTo("PENDIENTE_PAGO");
        assertThat(dto.getTutorNombre()).isEqualTo("Pepe Grillo");
        assertThat(dto.getTutorCorreo()).isEqualTo("pepe@example.com");
        assertThat(dto.getMascotaNombre()).isEqualTo("Firulais");
        assertThat(dto.getPaseadorNombre()).isEqualTo("Ana P.");
        assertThat(dto.getMercadopagoUltimoEstado()).isEqualTo("rejected");
        assertThat(dto.getPuedeReintentarPago()).isTrue();
    }

    @Test
    void toTutorDetalleResponse_whenTutorNull_usesFallbackLabel() {
        Reserva r = new Reserva();
        r.setIdReserva(1);
        r.setIdTutorUsuario(55);
        r.setIdMascota(2);
        r.setIdAgendaBloque(3);
        r.setMontoTotal(BigDecimal.ONE);
        r.setEstadoReserva(null);

        ReservaTutorDetalleResponseDTO dto = ReservaDtoMapper.toTutorDetalleResponse(
                r, null, null, null, null, false);

        assertThat(dto.getTutorNombre()).isEqualTo("Tutor #55");
        assertThat(dto.getTutorCorreo()).isEqualTo("sin-correo@patiperro.cl");
        assertThat(dto.getMascotaNombre()).isEqualTo("Mascota #2");
        assertThat(dto.getPaseadorNombre()).isEqualTo("Paseador");
        assertThat(dto.getPuedeReintentarPago()).isFalse();
    }
}
