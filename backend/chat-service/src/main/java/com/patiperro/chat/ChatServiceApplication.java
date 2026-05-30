package com.patiperro.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatServiceApplication.class, args);

		System.out.println("----------------------------------------------");
        System.out.println("¡Microservicio de CHAT iniciado con éxito!");
        System.out.println("Puerto: 8089");
        System.out.println("----------------------------------------------");
	}

}
