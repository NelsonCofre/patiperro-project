package com.patiperro.pagos;

import com.patiperro.pagos.config.ComisionPlataformaProperties;
import com.patiperro.pagos.config.PagosWebhookProperties;
import com.patiperro.pagos.config.MercadoPagoCheckoutProperties;
import com.patiperro.pagos.config.MercadoPagoRetryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties({
        MercadoPagoRetryProperties.class,
        MercadoPagoCheckoutProperties.class,
        ComisionPlataformaProperties.class,
        PagosWebhookProperties.class
})
public class PagosServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PagosServiceApplication.class, args);
	}

}
