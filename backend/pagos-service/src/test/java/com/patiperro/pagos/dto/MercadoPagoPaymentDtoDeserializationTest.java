package com.patiperro.pagos.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class MercadoPagoPaymentDtoDeserializationTest {

    @Test
    void jsonSample_mapeaSnakeCaseYOmiteCamposDesconocidos() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        try (InputStream in = getClass().getResourceAsStream("/mercadopago/payment-approved-sample.json")) {
            assertThat(in).isNotNull();
            MercadoPagoPaymentDto dto = objectMapper.readValue(in, MercadoPagoPaymentDto.class);
            assertThat(dto.id()).isEqualTo(123456789L);
            assertThat(dto.status()).isEqualTo("approved");
            assertThat(dto.externalReference()).isEqualTo("42");
            assertThat(dto.transactionAmount()).isEqualByComparingTo("10000");
            assertThat(dto.currencyId()).isEqualTo("CLP");
            assertThat(dto.dateApproved()).contains("2026-05-04");
            assertThat(dto.paymentTypeId()).isEqualTo("credit_card");
            assertThat(dto.refunds()).isEmpty();
        }
    }

    @Test
    void jsonMinimo_sinCamposExtra_deserializa() throws Exception {
        String minimal = "{\"id\":1,\"status\":\"pending\",\"refunds\":[]}";
        MercadoPagoPaymentDto dto = new ObjectMapper().readValue(minimal, MercadoPagoPaymentDto.class);
        assertThat(dto.status()).isEqualTo("pending");
        assertThat(dto.currencyId()).isNull();
        assertThat(dto.transactionAmount()).isNull();
    }
}
