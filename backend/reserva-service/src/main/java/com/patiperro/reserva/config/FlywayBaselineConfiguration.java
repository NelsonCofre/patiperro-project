package com.patiperro.reserva.config;

import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Permite arrancar sobre BD ya existente sin {@code flyway_schema_history} (ex Hibernate): baseline implícito
 * en versión 2 y aplicación solo de migraciones posteriores (p. ej. V3). En BD vacía Flyway ejecuta V1… desde cero.
 */
@Configuration
public class FlywayBaselineConfiguration {

    @Bean
    FlywayConfigurationCustomizer patiperroReservaFlywayBaseline() {
        return configuration -> configuration
                .baselineOnMigrate(true)
                .baselineVersion("2");
    }
}
