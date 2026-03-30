package com.patiperro.mascota.repository;

import com.patiperro.mascota.model.Mascota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MascotaRepository extends JpaRepository<Mascota, Long> {
    
    // Este método nos servirá para mostrarle al Tutor "sus" mascotas registradas
    List<Mascota> findByIdTutor(Long idTutor);
}