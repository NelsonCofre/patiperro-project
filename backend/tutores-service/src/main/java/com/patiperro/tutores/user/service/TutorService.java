package com.patiperro.tutores.user.service;

import com.patiperro.tutores.user.model.Tutor;
import com.patiperro.tutores.user.repository.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TutorService {

    private final TutorRepository tutorRepository;

    public Tutor findById(Long id) {
        return tutorRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Tutor no encontrado"));
    }

    public Tutor findByCorreo(String correo) {
        return tutorRepository.findByCorreo(correo).orElseThrow(() -> new IllegalArgumentException("Tutor no encontrado"));
    }
}
