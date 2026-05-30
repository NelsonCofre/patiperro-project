package com.patiperro.chat.config;

import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * BD existente sin {@code flyway_schema_history} (p. ej. creada con {@code chat_db-init.sql}):
 * baseline en versión 0 para que {@code V1__*} se aplique en el primer arranque.
 */
@Configuration
public class FlywayBaselineConfiguration {

	@Bean
	FlywayConfigurationCustomizer chatFlywayBaseline() {
		return configuration -> configuration
				.baselineOnMigrate(true)
				.baselineVersion("0");
	}
}
