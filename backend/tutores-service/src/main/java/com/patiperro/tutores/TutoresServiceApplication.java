package com.patiperro.tutores;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/** API de tutores = dueños de mascotas (perfil, auth, direccion, galeria). */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class TutoresServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TutoresServiceApplication.class, args);
    }
}
