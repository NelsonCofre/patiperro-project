package com.patiperro.chat.repository;

import com.patiperro.chat.model.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversacionRepository extends JpaRepository<Conversacion, Integer> {

	@Query("SELECT c FROM Conversacion c JOIN FETCH c.estadoChat")
	List<Conversacion> findAllWithEstadoChat();

	@Query("SELECT c FROM Conversacion c JOIN FETCH c.estadoChat WHERE c.id = :id")
	Optional<Conversacion> findByIdWithEstadoChat(@Param("id") Integer id);

	@Query("SELECT c FROM Conversacion c JOIN FETCH c.estadoChat WHERE c.idReserva = :idReserva")
	List<Conversacion> findByIdReservaWithEstadoChat(@Param("idReserva") Integer idReserva);

	@Query("SELECT c FROM Conversacion c JOIN FETCH c.estadoChat WHERE c.idReserva = :idReserva ORDER BY c.fechaCreacion ASC")
	List<Conversacion> findAllByIdReservaOrderByFechaCreacionAsc(@Param("idReserva") Integer idReserva);

	default Optional<Conversacion> findFirstByIdReservaOrderByFechaCreacionAsc(Integer idReserva) {
		return findAllByIdReservaOrderByFechaCreacionAsc(idReserva).stream().findFirst();
	}
}
