package com.patiperro.reserva;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Arranque completo con PostgreSQL gestionado por Testcontainers (Flyway + Hibernate validate).
 * Sin Docker activo el test se omite ({@code disabledWithoutDocker}).
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("dev")
class ReservaServicePostgresIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("reserva_db")
            .withUsername("postgres")
            .withPassword("testpass");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("jwt.secret", () -> "test-jwt-secret-key-for-context-load-only-min-chars-2026xxxx");
        r.add("patiperro.reserva.interno.secret", () -> "test-reserva-interno-secret");
        r.add("patiperro.reserva.solicitud.expiracion.scheduler.enabled", () -> "false");
        r.add("patiperro.reserva.reembolso.reconciliacion.scheduler.enabled", () -> "false");
        r.add("patiperro.reserva.notificacion-reembolso.scheduler.enabled", () -> "false");
    }

    @Test
    void contextLoadsWithFlywayOnManagedPostgres() {
    }
}
