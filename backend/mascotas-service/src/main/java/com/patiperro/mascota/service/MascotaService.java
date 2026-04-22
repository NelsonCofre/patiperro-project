package com.patiperro.mascota.service;

import com.patiperro.mascota.dto.PortadaIntegracion;
import com.patiperro.mascota.dto.MascotaInternoDetalleResponse;
import com.patiperro.mascota.exception.ForbiddenOperationException;
import com.patiperro.mascota.model.Foto;
import com.patiperro.mascota.model.Mascota;
import com.patiperro.mascota.model.Raza;
import com.patiperro.mascota.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MascotaService {

    private final MascotaRepository mascotaRepository;
    private final EspecieRepository especieRepository;
    private final RazaRepository razaRepository;
    private final TamanoRepository tamanoRepository;
    private final FotoRepository fotoRepository;

    // =========================================================================
    // GESTIÓN DE REGISTRO Y LISTADO (Vigilancia de Propiedad)
    // =========================================================================

    @Transactional
    public Mascota registrarMascota(@NonNull Mascota mascota, @NonNull Long idTutorSesion) {
        mascota.setIdMascota(null); // Vigilancia: Evita que el usuario fuerce un ID existente //
        mascota.setIdTutor(idTutorSesion); // Seguridad: Vincula la mascota al tutor logueado automáticamente //
        mascota.getFotos().clear(); // Integridad: El registro inicial no debe traer fotos previas //
        enlazarCatalogo(mascota); // Validación: Cruza datos con las tablas maestras //
        return mascotaRepository.save(mascota);
    }

    public List<Mascota> listarPorTutorAutorizado(Long idTutorEnUrl, @NonNull Long idTutorSesion) {
        if (!idTutorEnUrl.equals(idTutorSesion)) { // Bloqueo de seguridad: Evita ver mascotas de otros tutores //
            throw new ForbiddenOperationException("Solo puede consultar sus propias mascotas");
        }
        return mascotaRepository.findByIdTutor(idTutorSesion);
    }

    public List<Mascota> listarMisMascotas(@NonNull Long idTutorSesion) {
        return mascotaRepository.findByIdTutor(idTutorSesion); // Recuperada: Obtiene la lista directa de la sesión //
    }

    public Mascota obtenerPerfil(Long idMascota, @NonNull Long idTutorSesion) {
        Mascota m = mascotaRepository.findById(idMascota)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        asegurarPropietario(m, idTutorSesion); // Vigilancia: Valida que el solicitante sea el dueño real //
        return m;
    }

    // =========================================================================
    // ACTUALIZACIÓN Y ELIMINACIÓN (CRUD Avanzado y Selectivo)
    // =========================================================================

    @Transactional
    public Mascota actualizarMascota(Long idMascota, @NonNull Mascota body, @NonNull Long idTutorSesion) {
        Mascota existente = mascotaRepository.findById(idMascota)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        asegurarPropietario(existente, idTutorSesion); // Vigilancia de integridad //

        // Mapeo selectivo: El backend controla qué campos pueden ser modificados //
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

        enlazarCatalogoParaActualizar(existente, body); // Re-validación de integridad de catálogos //
        return mascotaRepository.save(existente);
    }

    @Transactional
    public void eliminarMascota(Long idMascota, @NonNull Long idTutorSesion) {
        Mascota m = mascotaRepository.findById(idMascota)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        asegurarPropietario(m, idTutorSesion); // Vigilancia previa al borrado físico //
        mascotaRepository.delete(m);
    }

    // =========================================================================
    // GESTIÓN DE GALERÍA DE FOTOS
    // =========================================================================

    @Transactional
    public Foto agregarFoto(Long idMascota, String url, @NonNull Long idTutorSesion) {
        Mascota mascota = obtenerPerfil(idMascota, idTutorSesion);
        Foto foto = new Foto();
        foto.setUrl(url);
        foto.setMascota(mascota);
        mascota.getFotos().add(foto); // Relación bidireccional gestionada por el service //
        mascotaRepository.save(mascota);
        return foto;
    }

    public List<Foto> listarFotos(Long idMascota, @NonNull Long idTutorSesion) {
        obtenerPerfil(idMascota, idTutorSesion); // Validación de permiso de lectura //
        return fotoRepository.findByMascota_IdMascota(idMascota);
    }

    @Transactional
    public void quitarFoto(Long idMascota, Long idFoto, @NonNull Long idTutorSesion) {
        Mascota m = obtenerPerfil(idMascota, idTutorSesion);
        Foto foto = fotoRepository.findById(idFoto)
                .orElseThrow(() -> new IllegalArgumentException("Foto no encontrada"));
        if (!foto.getMascota().getIdMascota().equals(idMascota)) { // Vigilancia: Evita borrar fotos de otro perro //
            throw new IllegalArgumentException("Foto no asociada a la mascota");
        }
        m.getFotos().remove(foto);
        mascotaRepository.save(m);
    }

    // =========================================================================
    // MÉTODOS DE VIGILANCIA Y APOYO TÉCNICO
    // =========================================================================

    private void asegurarPropietario(Mascota m, Long idTutorSesion) {
        if (!m.getIdTutor().equals(idTutorSesion)) { // Bloqueo de seguridad preventivo //
            throw new ForbiddenOperationException("Acceso denegado: No tiene permisos sobre esta mascota");
        }
    }

    private void enlazarCatalogo(Mascota m) {
        asegurarRazaPerteneceAEspecie(m.getEspecie().getIdEspecie(), m.getRaza().getIdRaza());
        // Uso de idRequerido para validación de presencia //
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

    private void asegurarRazaPerteneceAEspecie(Long idEspecie, Long idRaza) {
        Raza raza = razaRepository.findById(idRequerido(idRaza))
                .orElseThrow(() -> new IllegalArgumentException("Raza no encontrada"));
        if (!raza.getEspecie().getIdEspecie().equals(idEspecie)) { // Validación de lógica de negocio cruzada //
            throw new IllegalArgumentException("Inconsistencia: La raza no pertenece a la especie");
        }
    }

    private Long idRequerido(Long id) {
        if (id == null) { // Recuperada: Valida que los identificadores obligatorios no sean nulos //
            throw new IllegalArgumentException("Identificador de catálogo obligatorio");
        }
        return id;
    }

    /**
     * Solo para integración server-to-server (cabecera secreta en {@link com.patiperro.mascota.controller.MascotaInternoController}).
     * Prioridad URL: {@code foto_perfil}; si no, primera URL de la galería {@code foto}.
     */
    @Transactional(readOnly = true)
    public PortadaIntegracion obtenerPortadaParaIntegracion(Long idMascota) {
        Optional<Mascota> opt = mascotaRepository.findById(idMascota);
        if (opt.isEmpty()) {
            return null;
        }
        Mascota m = opt.get();
        String nombre = m.getNombre() != null ? m.getNombre().trim() : "";
        if (m.getFotoPerfil() != null && !m.getFotoPerfil().isBlank()) {
            return new PortadaIntegracion(m.getFotoPerfil().trim(), nombre);
        }
        String url = fotoRepository.findByMascota_IdMascota(idMascota).stream()
                .map(Foto::getUrl)
                .filter(u -> u != null && !u.isBlank())
                .map(String::trim)
                .findFirst()
                .orElse(null);
        return new PortadaIntegracion(url, nombre);
    }

    /**
     * Detalle interno para el panel del paseador (reserva-service).
     */
    @Transactional(readOnly = true)
    public MascotaInternoDetalleResponse obtenerDetalleParaIntegracion(Long idMascota) {
        Optional<Mascota> opt = mascotaRepository.findById(idMascota);
        if (opt.isEmpty()) {
            return null;
        }
        Mascota m = opt.get();
        String foto = m.getFotoPerfil();
        if (foto == null || foto.isBlank()) {
            foto = fotoRepository.findByMascota_IdMascota(idMascota).stream()
                    .map(Foto::getUrl)
                    .filter(u -> u != null && !u.isBlank())
                    .map(String::trim)
                    .findFirst()
                    .orElse(null);
        }
        String peso = m.getPeso() != null ? m.getPeso().stripTrailingZeros().toPlainString() + " kg" : "";
        return new MascotaInternoDetalleResponse(
                m.getIdMascota(),
                m.getNombre(),
                foto,
                m.getRaza() != null ? m.getRaza().getNombre() : "",
                m.getTamano() != null ? m.getTamano().getNombre() : "",
                m.getEdadFormateada(),
                peso,
                m.getSexo(),
                m.getComportamiento(),
                m.getCuidadosEspeciales()
        );
    }
}