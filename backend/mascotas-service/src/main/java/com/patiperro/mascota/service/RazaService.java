package com.patiperro.mascota.service;

import com.patiperro.mascota.model.Raza;
import com.patiperro.mascota.repository.RazaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RazaService {

    private final RazaRepository razaRepository;

    public List<Raza> listarTodas() {
        return razaRepository.findAll();
    }

    public Raza crear(Raza raza) {
        return razaRepository.save(raza);
    }

    @Transactional
    public Raza actualizar(Long id, Raza body) {
        Raza r = razaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Raza no encontrada"));
        r.setNombre(body.getNombre());
        return r;
    }

    @Transactional
    public void eliminar(Long id) {
        if (!razaRepository.existsById(id)) {
            throw new IllegalArgumentException("Raza no encontrada");
        }
        razaRepository.deleteById(id);
    }
}
