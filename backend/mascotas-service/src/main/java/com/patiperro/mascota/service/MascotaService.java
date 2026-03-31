package com.patiperro.mascota.service;

import com.patiperro.mascota.exception.ForbiddenOperationException;
import com.patiperro.mascota.model.Foto;
import com.patiperro.mascota.model.Mascota;
import com.patiperro.mascota.model.MascotaFoto;
import com.patiperro.mascota.model.MascotaFotoId;
import com.patiperro.mascota.repository.EspecieRepository;
import com.patiperro.mascota.repository.FotoRepository;
import com.patiperro.mascota.repository.MascotaFotoRepository;
import com.patiperro.mascota.repository.MascotaRepository;
import com.patiperro.mascota.repository.RazaRepository;
import com.patiperro.mascota.repository.TamanoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MascotaService {

    private final MascotaRepository mascotaRepository;
    private final EspecieRepository especieRepository;
    private final RazaRepository razaRepository;
    private final TamanoRepository tamanoRepository;
    private final FotoRepository fotoRepository;
    private final MascotaFotoRepository mascotaFotoRepository;

    @Transactional
    public Mascota registrarMascota(@NonNull Mascota mascota, @NonNull Long idTutorSesion) {
        mascota.setIdMascota(null);
        mascota.setIdTutor(idTutorSesion);
        mascota.getMascotaFotos().clear();
        enlazarCatalogo(mascota);
        return mascotaRepository.save(mascota);
    }

    public List<Mascota> listarPorTutorAutorizado(Long idTutorEnUrl, @NonNull Long idTutorSesion) {
        if (!idTutorEnUrl.equals(idTutorSesion)) {
            throw new ForbiddenOperationException("Solo puede consultar sus propias mascotas");
        }
        return mascotaRepository.findByIdTutor(idTutorSesion);
    }

    public List<Mascota> listarMisMascotas(@NonNull Long idTutorSesion) {
        return mascotaRepository.findByIdTutor(idTutorSesion);
    }

    public Mascota obtenerPerfil(Long idMascota, @NonNull Long idTutorSesion) {
        Mascota m = mascotaRepository.findPerfilById(idMascota)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        asegurarPropietario(m, idTutorSesion);
        return m;
    }

    @Transactional
    public Mascota actualizarMascota(Long idMascota, @NonNull Mascota body, @NonNull Long idTutorSesion) {
        Mascota existente = mascotaRepository.findById(idMascota)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        asegurarPropietario(existente, idTutorSesion);
        existente.setNombre(body.getNombre());
        existente.setPeso(body.getPeso());
        existente.setFechaNacimiento(body.getFechaNacimiento());
        existente.setSexo(body.getSexo());
        existente.setComportamiento(body.getComportamiento());
        existente.setDescripcion(body.getDescripcion());
        existente.setCuidadosEspeciales(body.getCuidadosEspeciales());
        existente.setEsterilizado(body.getEsterilizado());
        existente.setNumeroChip(body.getNumeroChip());
        enlazarCatalogoParaActualizar(existente, body);
        return mascotaRepository.save(existente);
    }

    @Transactional
    public void eliminarMascota(Long idMascota, @NonNull Long idTutorSesion) {
        Mascota m = mascotaRepository.findPerfilById(idMascota)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        asegurarPropietario(m, idTutorSesion);
        List<MascotaFoto> links = new ArrayList<>(m.getMascotaFotos());
        List<Long> fotoIds = links.stream().map(l -> l.getFoto().getIdFoto()).distinct().toList();
        mascotaFotoRepository.deleteAll(links);
        for (Long fotoId : fotoIds) {
            if (mascotaFotoRepository.countByFoto_IdFoto(fotoId) == 0) {
                fotoRepository.deleteById(fotoId);
            }
        }
        mascotaRepository.delete(m);
    }

    @Transactional
    public MascotaFoto agregarFoto(Long idMascota, String url, @NonNull Long idTutorSesion) {
        Mascota mascota = mascotaRepository.findById(idMascota)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        asegurarPropietario(mascota, idTutorSesion);
        Foto foto = fotoRepository.save(new Foto(null, url));
        MascotaFoto link = new MascotaFoto();
        link.setMascota(mascota);
        link.setFoto(foto);
        return mascotaFotoRepository.save(link);
    }

    public List<MascotaFoto> listarFotos(Long idMascota, @NonNull Long idTutorSesion) {
        Mascota m = mascotaRepository.findById(idMascota)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        asegurarPropietario(m, idTutorSesion);
        return mascotaFotoRepository.findByMascota_IdMascota(idMascota);
    }

    @Transactional
    public void quitarFoto(Long idMascota, Long idFoto, @NonNull Long idTutorSesion) {
        Mascota m = mascotaRepository.findById(idMascota)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        asegurarPropietario(m, idTutorSesion);
        MascotaFotoId id = new MascotaFotoId(idFoto, idMascota);
        MascotaFoto link = mascotaFotoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Foto no asociada a la mascota"));
        mascotaFotoRepository.delete(link);
        if (mascotaFotoRepository.countByFoto_IdFoto(idFoto) == 0) {
            fotoRepository.deleteById(idFoto);
        }
    }

    private void asegurarPropietario(Mascota m, Long idTutorSesion) {
        if (!m.getIdTutor().equals(idTutorSesion)) {
            throw new ForbiddenOperationException("No puede gestionar mascotas de otro tutor");
        }
    }

    private void enlazarCatalogo(Mascota m) {
        m.setEspecie(especieRepository.getReferenceById(idRequerido(m.getEspecie().getIdEspecie())));
        m.setRaza(razaRepository.getReferenceById(idRequerido(m.getRaza().getIdRaza())));
        m.setTamano(tamanoRepository.getReferenceById(idRequerido(m.getTamano().getIdTamano())));
    }

    private void enlazarCatalogoParaActualizar(Mascota destino, Mascota body) {
        destino.setEspecie(especieRepository.getReferenceById(idRequerido(body.getEspecie().getIdEspecie())));
        destino.setRaza(razaRepository.getReferenceById(idRequerido(body.getRaza().getIdRaza())));
        destino.setTamano(tamanoRepository.getReferenceById(idRequerido(body.getTamano().getIdTamano())));
    }

    private Long idRequerido(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Identificador de catálogo obligatorio");
        }
        return id;
    }
}
