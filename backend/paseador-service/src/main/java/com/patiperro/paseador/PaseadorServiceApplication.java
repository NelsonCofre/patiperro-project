package com.patiperro.paseador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Microservicio de paseadores (perfil, disponibilidad, etc.). Puerto 8082; prefijo API /api/paseadores. */
@SpringBootApplication
public class PaseadorServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaseadorServiceApplication.class, args);
	}

}
