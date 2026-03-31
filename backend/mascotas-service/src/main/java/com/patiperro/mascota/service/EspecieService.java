package com.patiperro.mascota.service;

import com.patiperro.mascota.model.Especie;
import com.patiperro.mascota.repository.EspecieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EspecieService {

    private final EspecieRepository especieRepository;

    public List<Especie> listarTodas() {
        return especieRepository.findAll();
    }

    public Especie crear(Especie especie) {
        return especieRepository.save(especie);
    }

    @Transactional
    public Especie actualizar(Long id, Especie body) {
        Especie e = especieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Especie no encontrada"));
        e.setNombre(body.getNombre());
        return e;
    }

    @Transactional
    public void eliminar(Long id) {
        if (!especieRepository.existsById(id)) {
            throw new IllegalArgumentException("Especie no encontrada");
        }
        especieRepository.deleteById(id);
    }
}
