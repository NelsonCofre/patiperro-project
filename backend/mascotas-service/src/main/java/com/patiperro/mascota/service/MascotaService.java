package com.patiperro.mascota.service;

import com.patiperro.mascota.exception.ForbiddenOperationException;
import com.patiperro.mascota.model.Foto;
import com.patiperro.mascota.model.Mascota;
import com.patiperro.mascota.model.Raza;
import com.patiperro.mascota.repository.EspecieRepository;
import com.patiperro.mascota.repository.FotoRepository;
import com.patiperro.mascota.repository.MascotaRepository;
import com.patiperro.mascota.repository.RazaRepository;
import com.patiperro.mascota.repository.TamanoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MascotaService {

    private final MascotaRepository mascotaRepository;
    private final EspecieRepository especieRepository;
    private final RazaRepository razaRepository;
    private final TamanoRepository tamanoRepository;
    private final FotoRepository fotoRepository;

    @Transactional
    public Mascota registrarMascota(@NonNull Mascota mascota, @NonNull Long idTutorSesion) {
        mascota.setIdMascota(null);
        mascota.setIdTutor(idTutorSesion);
        mascota.getFotos().clear();
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
        existente.setFotoPerfil(body.getFotoPerfil());
        enlazarCatalogoParaActualizar(existente, body);
        return mascotaRepository.save(existente);
    }

    @Transactional
    public void eliminarMascota(Long idMascota, @NonNull Long idTutorSesion) {
        Mascota m = mascotaRepository.findPerfilById(idMascota)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        asegurarPropietario(m, idTutorSesion);
        mascotaRepository.delete(m);
    }

    @Transactional
    public Foto agregarFoto(Long idMascota, String url, @NonNull Long idTutorSesion) {
        Mascota mascota = mascotaRepository.findPerfilById(idMascota)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        asegurarPropietario(mascota, idTutorSesion);
        Foto foto = new Foto();
        foto.setUrl(url);
        foto.setMascota(mascota);
        mascota.getFotos().add(foto);
        mascotaRepository.save(mascota);
        return foto;
    }

    public List<Foto> listarFotos(Long idMascota, @NonNull Long idTutorSesion) {
        Mascota m = mascotaRepository.findById(idMascota)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        asegurarPropietario(m, idTutorSesion);
        return fotoRepository.findByMascota_IdMascota(idMascota);
    }

    @Transactional
    public void quitarFoto(Long idMascota, Long idFoto, @NonNull Long idTutorSesion) {
        Mascota m = mascotaRepository.findPerfilById(idMascota)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        asegurarPropietario(m, idTutorSesion);
        Foto foto = fotoRepository.findById(idFoto)
                .orElseThrow(() -> new IllegalArgumentException("Foto no encontrada"));
        if (!foto.getMascota().getIdMascota().equals(idMascota)) {
            throw new IllegalArgumentException("Foto no asociada a la mascota");
        }
        m.getFotos().remove(foto);
        mascotaRepository.save(m);
    }

    private void asegurarPropietario(Mascota m, Long idTutorSesion) {
        if (!m.getIdTutor().equals(idTutorSesion)) {
            throw new ForbiddenOperationException("No puede gestionar mascotas de otro tutor");
        }
    }

    private void enlazarCatalogo(Mascota m) {
        asegurarRazaPerteneceAEspecie(m.getEspecie().getIdEspecie(), m.getRaza().getIdRaza());
        m.setEspecie(especieRepository.getReferenceById(idRequerido(m.getEspecie().getIdEspecie())));
        m.setRaza(razaRepository.getReferenceById(idRequerido(m.getRaza().getIdRaza())));
        m.setTamano(tamanoRepository.getReferenceById(idRequerido(m.getTamano().getIdTamano())));
    }

    private void enlazarCatalogoParaActualizar(Mascota destino, Mascota body) {
        asegurarRazaPerteneceAEspecie(body.getEspecie().getIdEspecie(), body.getRaza().getIdRaza());
        destino.setEspecie(especieRepository.getReferenceById(idRequerido(body.getEspecie().getIdEspecie())));
        destino.setRaza(razaRepository.getReferenceById(idRequerido(body.getRaza().getIdRaza())));
        destino.setTamano(tamanoRepository.getReferenceById(idRequerido(body.getTamano().getIdTamano())));
    }

    private void asegurarRazaPerteneceAEspecie(Long idEspecieMascota, Long idRaza) {
        Long idEsp = idRequerido(idEspecieMascota);
        Long idR = idRequerido(idRaza);
        Raza raza = razaRepository.findById(idR)
                .orElseThrow(() -> new IllegalArgumentException("Raza no encontrada"));
        if (!raza.getEspecie().getIdEspecie().equals(idEsp)) {
            throw new IllegalArgumentException("La raza no pertenece a la especie indicada");
        }
    }

    private Long idRequerido(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Identificador de catálogo obligatorio");
        }
        return id;
    }
}
