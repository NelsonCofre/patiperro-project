package com.patiperro.reserva;

import com.patiperro.reserva.config.properties.PagosBilleteraIntegracionProperties;
import com.patiperro.reserva.config.properties.PagosComprobanteIntegracionProperties;
import com.patiperro.reserva.config.properties.PagosCheckoutIntegracionProperties;
import com.patiperro.reserva.config.properties.PagosReembolsoIntegracionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties({
        PagosReembolsoIntegracionProperties.class,
        PagosBilleteraIntegracionProperties.class,
        PagosComprobanteIntegracionProperties.class,
        PagosCheckoutIntegracionProperties.class
})
public class ReservaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReservaServiceApplication.class, args);

        System.out.println("----------------------------------------------");
        System.out.println("¡Microservicio de RESERVA iniciado con éxito!");
        System.out.println("Puerto: 8090");
        System.out.println("----------------------------------------------");
    }
}
