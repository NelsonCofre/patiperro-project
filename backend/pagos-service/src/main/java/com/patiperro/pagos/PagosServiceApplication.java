package com.patiperro.pagos;

import com.patiperro.pagos.config.ComisionPlataformaProperties;
import com.patiperro.pagos.config.MercadoPagoCheckoutProperties;
import com.patiperro.pagos.config.MercadoPagoRetryProperties;
import com.patiperro.pagos.config.PagosWebhookProperties;
import com.patiperro.pagos.config.RetiroProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.LinkedHashMap;
import java.util.Map;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties({
        MercadoPagoRetryProperties.class,
        MercadoPagoCheckoutProperties.class,
        ComisionPlataformaProperties.class,
        PagosWebhookProperties.class,
        RetiroProperties.class
})
public class PagosServiceApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(PagosServiceApplication.class);
		app.setDefaultProperties(defaultDevProperties());
		app.run(args);

		System.out.println("----------------------------------------------");
		System.out.println("¡Microservicio de PAGOS iniciado con éxito!");
		System.out.println("Puerto: 8087");
		System.out.println("----------------------------------------------");
	}

	/**
	 * Valores mínimos si el IDE arranca sin copiar {@code application.properties} al classpath
	 * (p. ej. perfil {@code default} y puerto 8080).
	 */
	private static Map<String, Object> defaultDevProperties() {
		Map<String, Object> props = new LinkedHashMap<>();
		props.put(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, "dev");
		props.put("server.port", "8087");
		props.put("jwt.secret", "patiperro-dev-secret-key-should-be-long-and-random-2026");
		return props;
	}

}
