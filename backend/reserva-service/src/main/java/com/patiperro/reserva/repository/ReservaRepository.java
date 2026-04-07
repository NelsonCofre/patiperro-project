package com.patiperro.reserva.repository;

import com.patiperro.reserva.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    List<Reserva> findByIdTutorUsuario(Integer idTutorUsuario);

    List<Reserva> findByIdMascota(Integer idMascota);

    List<Reserva> findByIdAgendaBloque(Integer idAgendaBloque);

    List<Reserva> findByEstadoReserva_IdEstadoReserva(Integer idEstadoReserva);
}
