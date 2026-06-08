package com.patiperro.mascota.service;

import com.patiperro.mascota.model.Raza;
import com.patiperro.mascota.repository.EspecieRepository;
import com.patiperro.mascota.repository.RazaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RazaService {

    private final RazaRepository razaRepository;
    private final EspecieRepository especieRepository;

    @Transactional(readOnly = true)
    public List<Raza> listarTodas() {
        return razaRepository.findAll();
    }

    /** Si {@code idEspecie} es null, lista todas; si no, solo razas de esa especie. */
    @Transactional(readOnly = true)
    public List<Raza> listarPorEspecieOpcional(Long idEspecie) {
        if (idEspecie == null) {
            return razaRepository.findAll();
        }
        return razaRepository.findByEspecie_IdEspecie(idEspecie);
    }

    @Transactional
    public Raza crear(Raza raza) {
        enlazarEspecie(raza);
        return razaRepository.save(raza);
    }

    @Transactional
    public Raza actualizar(Long id, Raza body) {
        Raza r = razaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Raza no encontrada"));
        r.setNombre(body.getNombre());
        if (body.getEspecie() != null && body.getEspecie().getIdEspecie() != null) {
            r.setEspecie(especieRepository.getReferenceById(body.getEspecie().getIdEspecie()));
        }
        return r;
    }

    @Transactional
    public void eliminar(Long id) {
        if (!razaRepository.existsById(id)) {
            throw new IllegalArgumentException("Raza no encontrada");
        }
        razaRepository.deleteById(id);
    }

    private void enlazarEspecie(Raza raza) {
        if (raza.getEspecie() == null || raza.getEspecie().getIdEspecie() == null) {
            throw new IllegalArgumentException("La raza debe incluir especie con idEspecie");
        }
        raza.setEspecie(especieRepository.getReferenceById(raza.getEspecie().getIdEspecie()));
    }
}
