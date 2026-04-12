package com.patiperro.notification_service.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.patiperro.notification_service.model.LogEnvio;

public interface LogEnvioRepository extends JpaRepository<LogEnvio, Integer> {

// Vigilancia: Declaración explícita para que Spring genere la Query SQL automáticamente //
    List<LogEnvio> findByIdUsuario(Integer idUsuario);
}