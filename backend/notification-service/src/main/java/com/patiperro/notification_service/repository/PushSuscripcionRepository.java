package com.patiperro.notification_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.patiperro.notification_service.model.PushSuscripcion;

public interface PushSuscripcionRepository extends JpaRepository<PushSuscripcion, Integer> {

    /** UPSERT y baja: un endpoint identifica un dispositivo/navegador (único en BD). */
    Optional<PushSuscripcion> findByEndpoint(String endpoint);

    /** Envío push: todas las suscripciones activas del destinatario (puede haber varios dispositivos). */
    List<PushSuscripcion> findByIdUsuarioAndActivaTrue(Integer idUsuario);
}
