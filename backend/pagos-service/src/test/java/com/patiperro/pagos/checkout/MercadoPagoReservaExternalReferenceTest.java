package com.patiperro.pagos.checkout;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MercadoPagoReservaExternalReferenceTest {

    @Test
    void fromReservaId_prefijoEstable() {
        assertThat(MercadoPagoReservaExternalReference.fromReservaId(7)).isEqualTo("patiperro-reserva:7");
        assertThat(MercadoPagoReservaExternalReference.fromReservaId(7L)).isEqualTo("patiperro-reserva:7");
        assertThat(MercadoPagoReservaExternalReference.fromReservaId((Integer) null)).isNull();
    }
}
