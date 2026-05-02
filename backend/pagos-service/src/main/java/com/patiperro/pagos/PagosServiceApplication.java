package com.patiperro.pagos;

import com.patiperro.pagos.config.MercadoPagoCheckoutProperties;
import com.patiperro.pagos.config.MercadoPagoRetryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties({ MercadoPagoRetryProperties.class, MercadoPagoCheckoutProperties.class })
public class PagosServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PagosServiceApplication.class, args);
	}

}
