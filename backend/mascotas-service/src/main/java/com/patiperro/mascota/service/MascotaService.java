package com.patiperro.mascota.service;

import com.patiperro.mascota.dto.PortadaIntegracion;
import com.patiperro.mascota.dto.MascotaInternoDetalleResponse;
import com.patiperro.mascota.exception.ForbiddenOperationException;
import com.patiperro.mascota.model.Foto;
import com.patiperro.mascota.model.Mascota;
import com.patiperro.mascota.model.Raza;
import com.patiperro.mascota.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MascotaService {

    private final MascotaRepository mascotaRepository;
    private final EspecieRepository especieRepository;
    private final RazaRepository razaRepository;
    private final TamanoRepository tamanoRepository;
    private final FotoRepository fotoRepository;
    private final MascotaFotoStorageService mascotaFotoStorageService;

    // =========================================================================
    // GESTIÓN DE REGISTRO Y LISTADO (Vigilancia de Propiedad)
    // =========================================================================

    @Transactional
    public Mascota registrarMascota(Mascota mascota, Long idTutorSesion) {
        mascota.setIdMascota(null); // Vigilancia: Evita que el usuario fuerce un ID existente //
        mascota.setIdTutor(idTutorSesion); // Seguridad: Vincula la mascota al tutor logueado automáticamente //
        mascota.getFotos().clear(); // Integridad: El registro inicial no debe traer fotos previas //
        mascota.setFotoPerfil(null); // Foto solo vía PATCH/POST /foto-perfil (multipart validado) //
        enlazarCatalogo(mascota); // Validación: Cruza datos con las tablas maestras //
        Mascota guardada = mascotaRepository.save(mascota);
        return recargarPerfilParaRespuesta(guardada.getIdMascota());
    }

    @Transactional(readOnly = true)
    public List<Mascota> listarPorTutorAutorizado(Long idTutorEnUrl, Long idTutorSesion) {
        if (!idTutorEnUrl.equals(idTutorSesion)) { // Bloqueo de seguridad: Evita ver mascotas de otros tutores //
            throw new ForbiddenOperationException("Solo puede consultar sus propias mascotas");
        }
        return mascotaRepository.findByIdTutor(idTutorSesion);
    }

    @Transactional(readOnly = true)
    public List<Mascota> listarMisMascotas(Long idTutorSesion) {
        return mascotaRepository.findByIdTutor(idTutorSesion); // Recuperada: Obtiene la lista directa de la sesión //
    }

    @Transactional(readOnly = true)
    public Mascota obtenerPerfil(Long idMascota, Long idTutorSesion) {
        Mascota m = mascotaRepository.findPerfilById(idMascota)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        asegurarPropietario(m, idTutorSesion); // Vigilancia: Valida que el solicitante sea el dueño real //
        return m;
    }

    // =========================================================================
    // ACTUALIZACIÓN Y ELIMINACIÓN (CRUD Avanzado y Selectivo)
    // =========================================================================

    @Transactional
    public Mascota actualizarMascota(Long idMascota, Mascota body, Long idTutorSesion) {
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
        // fotoPerfil: no se actualiza por PUT; usar MascotaPerfilFotoController (multipart).

        enlazarCatalogoParaActualizar(existente, body); // Re-validación de integridad de catálogos //
        Mascota guardada = mascotaRepository.save(existente);
        return recargarPerfilParaRespuesta(guardada.getIdMascota());
    }

    /**
     * Sube o reemplaza la foto de perfil ({@code mascota.foto_perfil}). Persiste la URL pública y elimina
     * el archivo anterior solo si fue generado por este servicio.
     */
    @Transactional
    public Mascota actualizarFotoPerfil(Long idMascota, MultipartFile file, Long idTutorSesion)
            throws IOException {
        Mascota mascota = obtenerPerfil(idMascota, idTutorSesion);
        String urlAnterior = mascota.getFotoPerfil();

        String filenameNuevo = mascotaFotoStorageService.save(file);
        try {
            String urlNueva = mascotaFotoStorageService.buildPublicUrl(filenameNuevo);
            mascota.setFotoPerfil(urlNueva);
            Mascota guardada = mascotaRepository.save(mascota);

            registrarLimpiezaFotoTrasCommit(urlAnterior, filenameNuevo);
            return recargarPerfilParaRespuesta(guardada.getIdMascota());
        } catch (RuntimeException ex) {
            mascotaFotoStorageService.deleteQuietly(filenameNuevo);
            throw ex;
        }
    }

    @Transactional
    public void eliminarMascota(Long idMascota, Long idTutorSesion) {
        Mascota m = mascotaRepository.findById(idMascota)
                .orElseThrow(() -> new IllegalArgumentException("Mascota no encontrada"));
        asegurarPropietario(m, idTutorSesion);

        String urlPerfil = m.getFotoPerfil();
        List<String> urlsGaleria = m.getFotos().stream()
                .map(Foto::getUrl)
                .filter(u -> u != null && !u.isBlank())
                .toList();

        mascotaRepository.delete(m);

        registrarBorradoArchivosTrasCommit(urlPerfil, urlsGaleria);
    }

    // =========================================================================
    // GESTIÓN DE GALERÍA DE FOTOS
    // =========================================================================

    @Transactional
    public Foto agregarFoto(Long idMascota, String url, Long idTutorSesion) {
        Mascota mascota = obtenerPerfil(idMascota, idTutorSesion);
        Foto foto = new Foto();
        foto.setUrl(url);
        foto.setMascota(mascota);
        mascota.getFotos().add(foto); // Relación bidireccional gestionada por el service //
        mascotaRepository.save(mascota);
        return foto;
    }

    public List<Foto> listarFotos(Long idMascota, Long idTutorSesion) {
        obtenerPerfil(idMascota, idTutorSesion); // Validación de permiso de lectura //
        return fotoRepository.findByMascota_IdMascota(idMascota);
    }

    @Transactional
    public void quitarFoto(Long idMascota, Long idFoto, Long idTutorSesion) {
        Mascota m = obtenerPerfil(idMascota, idTutorSesion);
        Foto foto = fotoRepository.findById(idFoto)
                .orElseThrow(() -> new IllegalArgumentException("Foto no encontrada"));
        if (!foto.getMascota().getIdMascota().equals(idMascota)) { // Vigilancia: Evita borrar fotos de otro perro //
            throw new IllegalArgumentException("Foto no asociada a la mascota");
        }
        String urlGaleria = foto.getUrl();
        m.getFotos().remove(foto);
        mascotaRepository.save(m);
        registrarBorradoArchivosTrasCommit(null, List.of(urlGaleria));
    }

    // =========================================================================
    // MÉTODOS DE VIGILANCIA Y APOYO TÉCNICO
    // =========================================================================

    /** Carga especie/raza/tamaño antes de serializar JSON (open-in-view=false). */
    private Mascota recargarPerfilParaRespuesta(Long idMascota) {
        return mascotaRepository.findPerfilById(idMascota)
                .orElseThrow(() -> new IllegalStateException("Mascota no encontrada tras guardar"));
    }

    private void asegurarPropietario(Mascota m, Long idTutorSesion) {
        if (!Objects.equals(m.getIdTutor(), idTutorSesion)) { // Bloqueo de seguridad preventivo //
            throw new ForbiddenOperationException("Acceso denegado: No tiene permisos sobre esta mascota");
        }
    }

    /**
     * Tras commit: borra la foto de perfil anterior. Si la transacción hace rollback, borra el fichero nuevo
     * (evita huérfanos en disco sin tocar el archivo viejo).
     */
    private void registrarLimpiezaFotoTrasCommit(String urlAnterior, String filenameNuevo) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            eliminarArchivoLocalSiNuestro(urlAnterior);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eliminarArchivoLocalSiNuestro(urlAnterior);
            }

            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    mascotaFotoStorageService.deleteQuietly(filenameNuevo);
                }
            }
        });
    }

    private void registrarBorradoArchivosTrasCommit(String urlPerfil, List<String> urlsGaleria) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            eliminarArchivoLocalSiNuestro(urlPerfil);
            urlsGaleria.forEach(MascotaService.this::eliminarArchivoLocalSiNuestro);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eliminarArchivoLocalSiNuestro(urlPerfil);
                urlsGaleria.forEach(MascotaService.this::eliminarArchivoLocalSiNuestro);
            }
        });
    }

    /** Solo elimina ficheros bajo {@link MascotaFotoStorageService#PUBLIC_URL_PREFIX}; ignora URLs externas. */
    private void eliminarArchivoLocalSiNuestro(String url) {
        String filename = mascotaFotoStorageService.filenameFromPublicUrl(url);
        if (filename != null) {
            mascotaFotoStorageService.deleteQuietly(filename);
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
        Optional<Mascota> opt = mascotaRepository.findPerfilById(idMascota);
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