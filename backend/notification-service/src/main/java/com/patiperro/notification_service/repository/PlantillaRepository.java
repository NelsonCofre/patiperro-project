package com.patiperro.notification_service.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.patiperro.notification_service.model.PlantillaCorreo;

public interface PlantillaRepository extends JpaRepository<PlantillaCorreo, Integer> {
    // Vigilancia: Permite recuperar la configuración por su nombre lógico en lugar de ID //
    Optional<PlantillaCorreo> findByNombreEvento(String nombreEvento);
}