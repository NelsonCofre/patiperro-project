package com.patiperro.mascota.user.service; // CORREGIDO: Se agregó ".user"

import com.patiperro.mascota.model.Raza;
import com.patiperro.mascota.repository.RazaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RazaService {

    @Autowired
    private RazaRepository razaRepository;

    public List<Raza> listarTodas() {
        return razaRepository.findAll();
    }
}