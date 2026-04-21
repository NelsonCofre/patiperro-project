package com.patiperro.notification_service.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import com.patiperro.notification_service.dto.CorreoAceptacionRequest;
import com.patiperro.notification_service.dto.NotificacionEventoRequest;
import com.patiperro.notification_service.model.LogEnvio;
import com.patiperro.notification_service.model.PlantillaCorreo;
import com.patiperro.notification_service.service.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // =========================================================================
    // GESTIÓN DE PLANTILLAS (Administración de Mensajería)
    // =========================================================================

    @PostMapping("/plantillas")
    public ResponseEntity<PlantillaCorreo> crearPlantilla(@Valid @RequestBody @NonNull PlantillaCorreo plantilla) {
        // Vigilancia: Registra una nueva configuración de correo en el catálogo //
        PlantillaCorreo nueva = notificationService.registrarPlantilla(plantilla);
        return new ResponseEntity<>(nueva, HttpStatus.CREATED);
    }

    @GetMapping("/plantillas")
    public List<PlantillaCorreo> listarPlantillas() {
        // Recuperada: Obtiene todas las configuraciones disponibles //
        return notificationService.listarPlantillas();
    }

    @GetMapping("/plantillas/{id}")
    public PlantillaCorreo obtenerPlantilla(@PathVariable Integer id) {
        // Vigilancia: Busca una configuración específica por su ID //
        return notificationService.obtenerPlantilla(id);
    }

    @PutMapping("/plantillas/{id}")
    public PlantillaCorreo actualizarPlantilla(@PathVariable Integer id, @Valid @RequestBody PlantillaCorreo body) {
        // Integridad: Actualiza campos selectivos de la configuración //
        return notificationService.actualizarPlantilla(id, body);
    }

    @DeleteMapping("/plantillas/{id}")
    public ResponseEntity<Void> eliminarPlantilla(@PathVariable Integer id) {
        // Vigilancia: Eliminación física de una configuración de mensaje //
        notificationService.eliminarPlantilla(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // GESTIÓN DE LOGS Y AUDITORÍA (Trazabilidad)
    // =========================================================================

    /** * Endpoint interno: Registra el resultado de un intento de envío.
     * En una fase posterior, este método será llamado tras la ejecución del JavaMailSender.
     */
    @PostMapping("/logs")
    public ResponseEntity<LogEnvio> registrarResultadoEnvio(
            @RequestParam Integer idUsuario,
            @RequestParam Integer idPlantilla,
            @RequestParam String estado) {
        // Seguridad: Vincula el log al usuario y plantilla correspondiente //
        LogEnvio log = notificationService.registrarLogTrasEnvio(idUsuario, idPlantilla, estado);
        return new ResponseEntity<>(log, HttpStatus.CREATED);
    }

    @GetMapping("/logs/usuario/{idUsuario}")
    public List<LogEnvio> historialPorUsuario(@PathVariable Integer idUsuario) {
        // Recuperada: Filtra el historial de notificaciones para un usuario específico //
        return notificationService.listarLogsPorUsuario(idUsuario);
    }

    @GetMapping("/logs")
    public List<LogEnvio> historialCompleto() {
        // Vigilancia: Reporte global de envíos para monitoreo del sistema //
        return notificationService.listarHistorialCompleto();
    }

    @PostMapping("/enviar-aceptacion_v1")
public ResponseEntity<LogEnvio> enviarCorreoAceptacion(@Valid @RequestBody CorreoAceptacionRequest request) {
    // Vigilancia: El Controller recibe los datos ya validados del DTO //
    LogEnvio log = notificationService.procesarYEnviarCorreo(request);
    return new ResponseEntity<>(log, HttpStatus.CREATED);
}

/**
     * Endpoint para gatillar el envío de correo usando plantillas de Brevo.
     * Vigilancia: Valida que el DTO traiga todos los datos necesarios antes de procesar.
     */
    @PostMapping("/enviar-aceptacion")
    public ResponseEntity<LogEnvio> enviarNotificacionAceptacion(@Valid @RequestBody CorreoAceptacionRequest request) {
        // Integridad: Delegamos la lógica de comunicación con Brevo al Service
        LogEnvio resultado = notificationService.procesarYEnviarViaBrevo(request);
        
        // Retornamos el log generado (Éxito o Fallido) con el status correspondiente
        return new ResponseEntity<>(resultado, HttpStatus.CREATED);
    }

    // =========================================================================
    // ENDPOINT UNIVERSAL DE EVENTOS
    // =========================================================================

    /**
     * Endpoint maestro para disparar cualquier notificación basada en un evento.
     * El Service se encargará de traducir el "tipoEvento" al ID de plantilla de Brevo correcto.
     */
    @PostMapping("/disparar-evento")
    public ResponseEntity<LogEnvio> dispararNotificacionGlobal(@Valid @RequestBody NotificacionEventoRequest request) {
        System.out.println("DEBUG: Recibida petición de notificación para: " + request.getEmailDestino());
        // Integridad: Delegamos toda la lógica de ruteo y envío al Service
        LogEnvio resultado = notificationService.procesarEventoUniversal(request);
        
        return new ResponseEntity<>(resultado, HttpStatus.CREATED);
    }


}