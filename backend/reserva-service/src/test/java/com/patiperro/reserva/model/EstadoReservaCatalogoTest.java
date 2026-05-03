package com.patiperro.reserva.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoReservaCatalogoTest {

    @Test
    void estadoAdmiteCheckoutOReintentoMercadoPago_soloSolicitadaYPendientePago() {
        assertThat(EstadoReservaCatalogo.estadoAdmiteCheckoutOReintentoMercadoPago(null)).isFalse();
        assertThat(EstadoReservaCatalogo.estadoAdmiteCheckoutOReintentoMercadoPago(EstadoReservaCatalogo.ID_SOLICITADA))
                .isTrue();
        assertThat(EstadoReservaCatalogo.estadoAdmiteCheckoutOReintentoMercadoPago(EstadoReservaCatalogo.ID_PENDIENTE_PAGO))
                .isTrue();
        assertThat(EstadoReservaCatalogo.estadoAdmiteCheckoutOReintentoMercadoPago(EstadoReservaCatalogo.ID_PAGADA))
                .isFalse();
        assertThat(EstadoReservaCatalogo.estadoAdmiteCheckoutOReintentoMercadoPago(EstadoReservaCatalogo.ID_ACEPTADA))
                .isFalse();
    }

    @Test
    void idsEstadoReembolsoMercadoPago_incluyeRechazoExpiracionCancelacion() {
        assertThat(EstadoReservaCatalogo.IDS_ESTADO_REEMBOLSO_MERCADOPAGO)
                .containsExactly(
                        EstadoReservaCatalogo.ID_RECHAZADA,
                        EstadoReservaCatalogo.ID_EXPIRADA,
                        EstadoReservaCatalogo.ID_CANCELADA);
    }

    @Test
    void estadoAdmiteMarcarPagadaPorCobroMercadoPago_soloSolicitadaPendienteYAceptada() {
        assertThat(EstadoReservaCatalogo.estadoAdmiteMarcarPagadaPorCobroMercadoPago(null)).isFalse();
        assertThat(EstadoReservaCatalogo.estadoAdmiteMarcarPagadaPorCobroMercadoPago(EstadoReservaCatalogo.ID_SOLICITADA))
                .isTrue();
        assertThat(EstadoReservaCatalogo.estadoAdmiteMarcarPagadaPorCobroMercadoPago(EstadoReservaCatalogo.ID_PENDIENTE_PAGO))
                .isTrue();
        assertThat(EstadoReservaCatalogo.estadoAdmiteMarcarPagadaPorCobroMercadoPago(EstadoReservaCatalogo.ID_ACEPTADA))
                .isTrue();
        assertThat(EstadoReservaCatalogo.estadoAdmiteMarcarPagadaPorCobroMercadoPago(EstadoReservaCatalogo.ID_PAGADA))
                .isFalse();
        assertThat(EstadoReservaCatalogo.estadoAdmiteMarcarPagadaPorCobroMercadoPago(EstadoReservaCatalogo.ID_EXPIRADA))
                .isFalse();
        assertThat(EstadoReservaCatalogo.estadoAdmiteMarcarPagadaPorCobroMercadoPago(EstadoReservaCatalogo.ID_CANCELADA))
                .isFalse();
    }
}
