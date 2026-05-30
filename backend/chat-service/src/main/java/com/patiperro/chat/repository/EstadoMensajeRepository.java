package com.patiperro.chat.repository;

import com.patiperro.chat.model.EstadoMensaje;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoMensajeRepository extends JpaRepository<EstadoMensaje, Integer> {

	Optional<EstadoMensaje> findByNombre(String nombre);
}
