package com.patiperro.mascota.controller;

import com.patiperro.mascota.dto.FotoUrlRequest;
import com.patiperro.mascota.model.Foto;
import com.patiperro.mascota.model.Mascota;
import com.patiperro.mascota.security.TutorSecurity;
import com.patiperro.mascota.service.MascotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CRUD de mascotas (datos de perfil). La foto de perfil no se crea ni actualiza por JSON:
 * usar {@code PATCH|POST /api/mascotas/{id}/foto-perfil} ({@link com.patiperro.mascota.controller.MascotaPerfilFotoController}).
 */
@RestController
@RequestMapping("/api/mascotas")
@RequiredArgsConstructor
public class MascotaController {

    private final MascotaService mascotaService;

    /** Registro de datos; {@code fotoPerfil} se ignora en el body (multipart en /foto-perfil). */
    @PostMapping
    public ResponseEntity<Mascota> crearMascota(@Valid @RequestBody Mascota mascota) {
        long idTutor = TutorSecurity.requireTutor().tutorId();
        Mascota nuevaMascota = mascotaService.registrarMascota(mascota, idTutor);
        return new ResponseEntity<>(nuevaMascota, HttpStatus.CREATED);
    }

    /** Listado del tutor autenticado (recomendado para el front). */
    @GetMapping("/mias")
    public List<Mascota> misMascotas() {
        long idTutor = TutorSecurity.requireTutor().tutorId();
        return mascotaService.listarMisMascotas(idTutor);
    }

    @GetMapping("/tutor/{idTutor}")
    public List<Mascota> obtenerPorTutor(@PathVariable Long idTutor) {
        long idSesion = TutorSecurity.requireTutor().tutorId();
        return mascotaService.listarPorTutorAutorizado(idTutor, idSesion);
    }

    @GetMapping("/{id}")
    public Mascota obtenerPerfil(@PathVariable Long id) {
        long idTutor = TutorSecurity.requireTutor().tutorId();
        return mascotaService.obtenerPerfil(id, idTutor);
    }

    /** Actualización selectiva; {@code fotoPerfil} no se modifica por PUT. */
    @PutMapping("/{id}")
    public Mascota actualizar(@PathVariable Long id, @Valid @RequestBody Mascota body) {
        long idTutor = TutorSecurity.requireTutor().tutorId();
        return mascotaService.actualizarMascota(id, body, idTutor);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        long idTutor = TutorSecurity.requireTutor().tutorId();
        mascotaService.eliminarMascota(id, idTutor);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/fotos")
    public ResponseEntity<Foto> agregarFoto(
            @PathVariable Long id,
            @Valid @RequestBody FotoUrlRequest request) {
        long idTutor = TutorSecurity.requireTutor().tutorId();
        return new ResponseEntity<>(mascotaService.agregarFoto(id, request.url(), idTutor), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/fotos")
    public List<Foto> listarFotos(@PathVariable Long id) {
        long idTutor = TutorSecurity.requireTutor().tutorId();
        return mascotaService.listarFotos(id, idTutor);
    }

    @DeleteMapping("/{id}/fotos/{fotoId}")
    public ResponseEntity<Void> quitarFoto(@PathVariable Long id, @PathVariable Long fotoId) {
        long idTutor = TutorSecurity.requireTutor().tutorId();
        mascotaService.quitarFoto(id, fotoId, idTutor);
        return ResponseEntity.noContent().build();
    }
}
