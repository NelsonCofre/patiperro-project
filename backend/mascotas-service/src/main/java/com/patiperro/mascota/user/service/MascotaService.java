package com.patiperro.mascota.user.service;

import com.patiperro.mascota.model.Mascota;
import com.patiperro.mascota.repository.MascotaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull; // Importa esto
import java.util.List;

@Service
public class MascotaService {

    @Autowired
    private MascotaRepository mascotaRepository;

    // Escenario: Registro exitoso de la mascota
    // Agregamos @NonNull al parámetro
    public Mascota registrarMascota(@NonNull Mascota mascota) {
        return mascotaRepository.save(mascota);
    }

    public List<Mascota> listarPorTutor(Long idTutor) {
        return mascotaRepository.findByIdTutor(idTutor);
    }
}