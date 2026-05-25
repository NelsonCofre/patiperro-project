package com.patiperro.paseador.user.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patiperro.paseador.dto.user.PaseadorPerfilDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contrato JSON de los DTOs públicos del tutor (pasos 6–8): propiedad {@code esVerificado}.
 */
class PublicPaseadorDtosEsVerificadoJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void paseadorCercano_serializaEsVerificado_noVerificado() throws Exception {
        String json = mapper.writeValueAsString(PaseadorCercanoResponseDTO.builder()
                .idPaseador(1L)
                .nombreCompleto("Ana")
                .distanciaKm(2.5)
                .esVerificado(true)
                .build());

        JsonNode node = mapper.readTree(json);
        assertTrue(node.has("esVerificado"));
        assertTrue(node.get("esVerificado").asBoolean());
        assertFalse(node.has("verificado"));
    }

    @Test
    void paseadorResumen_serializaEsVerificado() throws Exception {
        String json = mapper.writeValueAsString(PaseadorResumenResponseDTO.builder()
                .idPaseador(2L)
                .nombreCompleto("Luis")
                .correo("luis@test.cl")
                .esVerificado(false)
                .build());

        JsonNode node = mapper.readTree(json);
        assertTrue(node.has("esVerificado"));
        assertFalse(node.get("esVerificado").asBoolean());
        assertFalse(node.has("verificado"));
    }

    @Test
    void paseadorPerfil_serializaEsVerificado() throws Exception {
        String json = mapper.writeValueAsString(PaseadorPerfilDTO.builder()
                .idUsuario(3L)
                .nombre("María Pérez")
                .correo("maria@test.cl")
                .esVerificado(true)
                .build());

        JsonNode node = mapper.readTree(json);
        assertTrue(node.has("esVerificado"));
        assertTrue(node.get("esVerificado").asBoolean());
        assertFalse(node.has("verificado"));
    }

    @Test
    void paseadorCercano_deserializaAliasVerificado() throws Exception {
        PaseadorCercanoResponseDTO dto = mapper.readValue(
                """
                        {"idPaseador":1,"nombreCompleto":"X","distanciaKm":1,"verificado":true}
                        """,
                PaseadorCercanoResponseDTO.class);

        assertTrue(dto.isEsVerificado());
    }
}
