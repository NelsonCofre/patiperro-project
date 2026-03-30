package com.patiperro.mascota.repository;

import com.patiperro.mascota.model.Raza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RazaRepository extends JpaRepository<Raza, Long> {
    // Al extender de JpaRepository, ya tenemos métodos como findAll() 
    // para listar todas las razas de la base de datos automáticamente.
}