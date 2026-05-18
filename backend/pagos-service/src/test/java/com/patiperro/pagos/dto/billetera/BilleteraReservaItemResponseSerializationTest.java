package com.patiperro.pagos.dto.billetera;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class BilleteraReservaItemResponseSerializationTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Test
  void serializaDesgloseComisionSinConflictoDeGetters() throws Exception {
    BilleteraReservaItemResponse item = new BilleteraReservaItemResponse(
        27,
        new BigDecimal("1500.00"),
        new BigDecimal("75.00"),
        new BigDecimal("1425.00"),
        "LIBERADO_A_DISPONIBLE",
        "Pepe",
        "Tutor",
        "2026-05-15",
        "14:00",
        "FINALIZADA",
        99L,
        Instant.parse("2026-05-17T12:00:00Z"));

    String json = objectMapper.writeValueAsString(item);

    assertThat(json).contains("\"desgloseComision\"");
    assertThat(json).contains("\"montoBruto\":1500");
    assertThat(json).contains("\"montoNeto\":1425");
  }
}
