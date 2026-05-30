package com.patiperro.paseador.model;

import com.patiperro.paseador.repository.PaseadorRepository;
import com.patiperro.paseador.user.service.PaseadorVerificacionService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Persistencia real (H2) de {@code es_verificado} y hooks JPA de {@link Paseador}.
 * Complementa tests unitarios con Mockito; no ejecuta Flyway (igual que el resto del módulo).
 */
@SpringBootTest
@Transactional
class PaseadorEsVerificadoPersistenceTest {

    @Autowired
    private PaseadorRepository paseadorRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persist_estadoAprobado_sincronizaEsVerificadoEnBd() {
        Paseador paseador = Paseador.builder()
                .correo("aprobado-persist@test.cl")
                .contrasena("hash")
                .estadoVerificacionIdentidad(EstadoVerificacionIdentidad.APROBADO)
                .esVerificado(false)
                .build();

        Paseador guardado = paseadorRepository.saveAndFlush(paseador);
        entityManager.clear();

        Paseador recargado = paseadorRepository.findById(guardado.getId()).orElseThrow();
        assertTrue(recargado.isEsVerificado());
        assertTrue(PaseadorVerificacionService.esVerificadoPublicamente(recargado));
    }

    @Test
    void persist_estadoSinEnviar_persisteEsVerificadoFalse() {
        Paseador paseador = Paseador.builder()
                .correo("sin-enviar-persist@test.cl")
                .contrasena("hash")
                .estadoVerificacionIdentidad(EstadoVerificacionIdentidad.SIN_ENVIAR)
                .build();

        Paseador guardado = paseadorRepository.saveAndFlush(paseador);
        entityManager.clear();

        Paseador recargado = paseadorRepository.findById(guardado.getId()).orElseThrow();
        assertFalse(recargado.isEsVerificado());
    }

    @Test
    void postLoad_alineaEsVerificadoCuandoEnumEsAprobado() {
        Paseador guardado = paseadorRepository.saveAndFlush(Paseador.builder()
                .correo("postload@test.cl")
                .contrasena("hash")
                .estadoVerificacionIdentidad(EstadoVerificacionIdentidad.APROBADO)
                .build());

        entityManager.createNativeQuery(
                        "UPDATE paseador SET es_verificado = false WHERE id_paseador = :id")
                .setParameter("id", guardado.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        Paseador recargado = paseadorRepository.findById(guardado.getId()).orElseThrow();
        assertTrue(recargado.isEsVerificado());
    }
}
