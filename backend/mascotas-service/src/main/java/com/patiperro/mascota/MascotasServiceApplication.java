package com.patiperro.mascota;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MascotasServiceApplication {

    public static void main(String[] args) {
        // Esta línea inicia el servidor embebido (Tomcat) en el puerto 8083
        SpringApplication.run(MascotasServiceApplication.class, args);
        
        System.out.println("----------------------------------------------");
        System.out.println("¡Microservicio de MASCOTAS iniciado con éxito!");
        System.out.println("Puerto: 8083");
        System.out.println("----------------------------------------------");
    }
}
