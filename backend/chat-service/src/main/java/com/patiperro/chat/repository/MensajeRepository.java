package com.patiperro.chat.repository;

import com.patiperro.chat.model.Mensaje;
import com.patiperro.chat.model.TipoMensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MensajeRepository extends JpaRepository<Mensaje, Integer> {

	@Query("SELECT m FROM Mensaje m JOIN FETCH m.estadoMensaje JOIN FETCH m.conversacion "
			+ "WHERE m.conversacion.id = :idConversacion ORDER BY m.fechaEnvio ASC")
	List<Mensaje> findByConversacionIdWithCatalogos(@Param("idConversacion") Integer idConversacion);

	@Query("SELECT m FROM Mensaje m JOIN FETCH m.estadoMensaje JOIN FETCH m.conversacion "
			+ "WHERE m.id = :idMensaje AND m.conversacion.id = :idConversacion")
	Optional<Mensaje> findByIdAndConversacionIdWithCatalogos(
			@Param("idMensaje") Integer idMensaje,
			@Param("idConversacion") Integer idConversacion);

	@Query("SELECT m FROM Mensaje m WHERE m.id = :idMensaje AND m.conversacion.id = :idConversacion")
	Optional<Mensaje> findByIdAndConversacionId(
			@Param("idMensaje") Integer idMensaje,
			@Param("idConversacion") Integer idConversacion);

	@Query("SELECT m FROM Mensaje m JOIN FETCH m.estadoMensaje JOIN FETCH m.conversacion c "
			+ "WHERE c.idReserva = :idReserva AND m.tipo = :tipo ORDER BY m.fechaEnvio ASC")
	List<Mensaje> findByReservaIdAndTipoWithCatalogos(
			@Param("idReserva") Integer idReserva,
			@Param("tipo") TipoMensaje tipo);
}
