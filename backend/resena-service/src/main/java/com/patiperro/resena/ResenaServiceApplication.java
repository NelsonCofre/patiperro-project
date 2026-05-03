package com.patiperro.resena;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// REVISA QUE TENGA EL .jpa AL FINAL
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration; // Importa esto

@SpringBootApplication(exclude = {
    SimpleDiscoveryClientAutoConfiguration.class // Esto mata el error de raíz
})
@EnableFeignClients
public class ResenaServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResenaServiceApplication.class, args);
    }
}