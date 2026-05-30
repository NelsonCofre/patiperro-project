package com.patiperro.agenda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AgendaServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgendaServiceApplication.class, args);

		System.out.println("----------------------------------------------");
        System.out.println("¡Microservicio de AGENDA iniciado con éxito!");
        System.out.println("Puerto: 8084");
        System.out.println("----------------------------------------------");
	}

}
