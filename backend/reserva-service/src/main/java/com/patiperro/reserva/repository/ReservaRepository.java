package com.patiperro.reserva.repository;

import com.patiperro.reserva.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    List<Reserva> findByIdTutorUsuario(Integer idTutorUsuario);

    List<Reserva> findByIdMascota(Integer idMascota);

    List<Reserva> findByIdAgendaBloque(Integer idAgendaBloque);

    List<Reserva> findByEstadoReserva_IdEstadoReserva(Integer idEstadoReserva);

    /**
     * VIGILANCIA DE COMPROMISOS:
     * Verifica si alguno de los bloques de agenda enviados ya tiene una reserva 
     * en estados que impiden cambios (ej. Confirmada, Pagada, En Curso).
     * 
     * @param idsAgenda Lista de IDs de bloques a revisar.
     * @param idsEstadoReservaComprometidos IDs de los estados que bloquean la edición.
     * @return true si existe al menos una coincidencia (hay un compromiso previo).
     */
    boolean existsByIdAgendaBloqueInAndEstadoReserva_IdEstadoReservaIn(
            Collection<Integer> idsAgenda,
            Collection<Integer> idsEstadoReservaComprometidos);
}