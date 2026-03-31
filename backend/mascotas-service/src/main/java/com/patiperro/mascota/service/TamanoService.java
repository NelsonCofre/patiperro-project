package com.patiperro.mascota.service;

import com.patiperro.mascota.model.Tamano;
import com.patiperro.mascota.repository.TamanoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TamanoService {

    private final TamanoRepository tamanoRepository;

    public List<Tamano> listarTodos() {
        return tamanoRepository.findAll();
    }

    public Tamano crear(Tamano tamano) {
        return tamanoRepository.save(tamano);
    }

    @Transactional
    public Tamano actualizar(Long id, Tamano body) {
        Tamano t = tamanoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tamaño no encontrado"));
        t.setNombre(body.getNombre());
        t.setDescripcion(body.getDescripcion());
        return t;
    }

    @Transactional
    public void eliminar(Long id) {
        if (!tamanoRepository.existsById(id)) {
            throw new IllegalArgumentException("Tamaño no encontrado");
        }
        tamanoRepository.deleteById(id);
    }
}
