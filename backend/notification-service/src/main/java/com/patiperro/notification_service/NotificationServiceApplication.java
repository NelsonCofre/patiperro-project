package com.patiperro.notification_service;

import com.patiperro.notification_service.config.WebPushProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Microservicio de notificaciones (correo Brevo, Web Push del chat).
 * {@link WebPushProperties} se registra vía {@link EnableConfigurationProperties}
 * (mismo patrón que {@code api-gateway} / {@code pagos-service}).
 */
@SpringBootApplication
@EnableConfigurationProperties(WebPushProperties.class)
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);

        System.out.println("----------------------------------------------");
        System.out.println("¡Microservicio de NOTIFICATION iniciado con éxito!");
        System.out.println("Puerto: 8086");
        System.out.println("----------------------------------------------");
    }
}
