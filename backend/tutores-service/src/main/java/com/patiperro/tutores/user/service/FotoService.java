package com.patiperro.tutores.user.service;

import com.patiperro.tutores.user.model.Foto;
import com.patiperro.tutores.user.model.Tutor;
import com.patiperro.tutores.user.repository.FotoRepository;
import com.patiperro.tutores.user.repository.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FotoService {

    private final FotoRepository fotoRepository;
    private final TutorRepository tutorRepository;

    public List<Foto> findByTutorId(Long tutorId) {
        return fotoRepository.findByTutor_Id(tutorId);
    }

    public Foto crear(Long tutorId, String url) {
        Tutor tutor = tutorRepository.findById(tutorId)
                .orElseThrow(() -> new IllegalArgumentException("Tutor no encontrado"));
        Foto foto = Foto.builder().url(url).tutor(tutor).build();
        return fotoRepository.save(foto);
    }

    public void eliminar(Long id) {
        fotoRepository.deleteById(id);
    }
}
