package com.patiperro.paseador.config;

import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * BD existente sin {@code flyway_schema_history} (creada con Hibernate): baseline en versión 0
 * para que {@code V1__*} se aplique en el primer arranque con perfil prod.
 */
@Configuration
public class FlywayBaselineConfiguration {

    @Bean
    FlywayConfigurationCustomizer paseadoresFlywayBaseline() {
        return configuration -> configuration
                .baselineOnMigrate(true)
                .baselineVersion("0");
    }
}
