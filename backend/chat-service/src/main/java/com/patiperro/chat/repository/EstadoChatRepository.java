package com.patiperro.chat.repository;

import com.patiperro.chat.model.EstadoChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoChatRepository extends JpaRepository<EstadoChat, Integer> {

	Optional<EstadoChat> findByNombre(String nombre);
}
