package com.patiperro.notification_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.patiperro.notification_service.dto.CorreoAceptacionRequest;
import com.patiperro.notification_service.dto.NotificacionEventoRequest;
import com.patiperro.notification_service.model.LogEnvio;
import com.patiperro.notification_service.model.PlantillaCorreo;
import com.patiperro.notification_service.repository.LogEnvioRepository;
import com.patiperro.notification_service.repository.PlantillaRepository;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final PlantillaRepository plantillaRepository;
    private final LogEnvioRepository logEnvioRepository;
    private final JavaMailSender mailSender;

    // =========================================================================
    // GESTIÓN DE PLANTILLAS (Configuración del Sistema)
    // =========================================================================

    @Transactional
    public PlantillaCorreo registrarPlantilla(@NonNull PlantillaCorreo plantilla) {
        plantilla.setId_plantilla(null); // Vigilancia: Evita sobrescribir configuraciones existentes
        validarEstructuraPlantilla(plantilla); // Integridad: Asegura que el HTML y asunto no estén vacíos
        return plantillaRepository.save(plantilla);
    }

    public List<PlantillaCorreo> listarPlantillas() {
        return plantillaRepository.findAll(); // Recuperada: Listado de catálogo para administración
    }

    public PlantillaCorreo obtenerPlantilla(Integer idPlantilla) {
        return plantillaRepository.findById(idPlantilla)
                .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada con ID: " + idPlantilla));
    }

    public PlantillaCorreo obtenerPlantillaPorNombre(String nombreEvento) {
        // Recuperada: Busca la plantilla por nombre lógico para gatillar correos
        return plantillaRepository.findByNombreEvento(nombreEvento)
                .orElseThrow(() -> new IllegalArgumentException("Error de Configuración: La plantilla '" + nombreEvento + "' no existe"));
    }

    @Transactional
    public PlantillaCorreo actualizarPlantilla(Integer idPlantilla, @NonNull PlantillaCorreo body) {
        PlantillaCorreo existente = obtenerPlantilla(idPlantilla);
        
        existente.setNombreEvento(body.getNombreEvento());
        existente.setAsunto(body.getAsunto());
        existente.setCuerpo_html(body.getCuerpo_html());

        validarEstructuraPlantilla(existente);
        return plantillaRepository.save(existente);
    }

    @Transactional
    public void eliminarPlantilla(Integer idPlantilla) {
        PlantillaCorreo p = obtenerPlantilla(idPlantilla);
        plantillaRepository.delete(p);
    }

    // =========================================================================
    // LÓGICA DE PROCESAMIENTO Y ENVÍO (Integración Microservicios)
    // =========================================================================

    @Transactional
    public LogEnvio procesarYEnviarCorreo(CorreoAceptacionRequest datos) {
        // 1. Vigilancia: Buscar la plantilla base configurada
        PlantillaCorreo plantilla = obtenerPlantillaPorNombre("RESERVA_ACEPTADA");

        // 2. Integridad: Reemplazar variables en el HTML con los datos del DTO
        String htmlFinal = plantilla.getCuerpo_html()
            .replace("{{nombre_paseador}}", datos.nombrePaseador())
            .replace("{{nombre_mascota}}", datos.nombreMascota())
            .replace("{{monto}}", datos.montoTotal().toString());

        String estadoEnvio = "EXITOSO";

        try {
            // 3. Acción: Ejecutar el envío físico de correo HTML
            enviarEmailFisico(datos.correoTutor(), plantilla.getAsunto(), htmlFinal); 
        } catch (Exception e) {
            estadoEnvio = "FALLIDO";
        }

        // 4. Auditoría: Registrar el resultado en la tabla log_envio local
        return registrarLogTrasEnvio(datos.idUsuario(), plantilla.getId_plantilla(), estadoEnvio);
    }

    // =========================================================================
    // ACCIÓN: ENVÍO FÍSICO DE CORREO (Integración con JavaMailSender)
    // =========================================================================

    private void enviarEmailFisico(String destinatario, String asunto, String contenidoHtml) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom("no-reply@patiperro.cl"); // Remitente oficial [cite: 45, 98]
        helper.setTo(destinatario);
        helper.setSubject(asunto);
        helper.setText(contenidoHtml, true); // Habilita renderizado HTML [cite: 583, 594]

        mailSender.send(message);
    }

    // =========================================================================
    // GESTIÓN DE LOGS DE ENVÍO (Auditoría y Trazabilidad)
    // =========================================================================

    @Transactional
    public LogEnvio registrarLogTrasEnvio(@NonNull Integer idUsuario, @NonNull Integer idPlantilla, @NonNull String estado) {
        PlantillaCorreo plantilla = obtenerPlantilla(idPlantilla);
        
        LogEnvio log = new LogEnvio();
        log.setId_log(null);
        log.setIdUsuario(idUsuario);
        log.setEstado(estado.toUpperCase());
        log.setPlantilla(plantilla);
        
        return logEnvioRepository.save(log);
    }

    public List<LogEnvio> listarLogsPorUsuario(Integer idUsuario) {
        return logEnvioRepository.findByIdUsuario(idUsuario); 
    }

    public List<LogEnvio> listarHistorialCompleto() {
        return logEnvioRepository.findAll();
    }

    private void validarEstructuraPlantilla(PlantillaCorreo p) {
        if (p.getNombreEvento() == null || p.getNombreEvento().isBlank()) {
            throw new IllegalArgumentException("El nombre del evento es obligatorio");
        }
        if (p.getAsunto() == null || p.getAsunto().isBlank()) {
            throw new IllegalArgumentException("La plantilla debe tener un asunto");
        }
        if (p.getCuerpo_html() == null || p.getCuerpo_html().length() < 10) {
            throw new IllegalArgumentException("El cuerpo del correo es inválido");
        }
    }

    @Transactional
public LogEnvio procesarYEnviarViaBrevo(CorreoAceptacionRequest datos) {
    String estadoEnvio = "EXITOSO";
    
    // Configuración de la API de Brevo
    WebClient webClient = WebClient.create("https://api.brevo.com/v3/smtp/email");
    String apiKey = "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";

    try {
        // Construimos el JSON exacto que pide Brevo para usar plantillas
        Map<String, Object> body = Map.of(
            "templateId", datos.idPlantillaBrevo(),
            "to", List.of(Map.of("email", datos.correoTutor())),
            "params", Map.of(
                "nombrePaseador", datos.nombrePaseador(),
                "nombreMascota", datos.nombreMascota(),
                "montoTotal", datos.montoTotal(),
                "nombreTutor", datos.nombreTutor()
            )
        );

        webClient.post()
            .header("api-key", apiKey)
            .bodyValue(body)
            .retrieve()
            .toBodilessEntity()
            .block(); // Esperamos la respuesta

        System.out.println("DEBUG: Brevo API aceptó la petición de plantilla");

    } catch (Exception e) {
        estadoEnvio = "FALLIDO";
        System.out.println("Error API Brevo: " + e.getMessage());
    }

    return registrarLogTrasEnvio(datos.idUsuario(), datos.idPlantillaBrevo(), estadoEnvio);
}


@Transactional
    public LogEnvio procesarEventoUniversal(NotificacionEventoRequest request) {
        String estadoEnvio = "EXITOSO";
        Long brevoTemplateId = obtenerTemplateIdPorEvento(request.getTipoEvento());

        // Configuración de la API de Brevo
        WebClient webClient = WebClient.create("https://api.brevo.com/v3/smtp/email");
        String apiKey = "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"; // Tip: ¡Mueve esto a application.properties luego!

        try {
            // Construimos el JSON dinámico para Brevo
            Map<String, Object> body = Map.of(
                "templateId", brevoTemplateId,
                "to", List.of(Map.of("email", request.getEmailDestino())),
                "params", request.getVariables() // <-- Aquí pasamos el Map completo de variables
            );

            webClient.post()
                .header("api-key", apiKey)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block(); // Esperamos la respuesta

            System.out.println("DEBUG: Brevo API aceptó el evento: " + request.getTipoEvento());

        } catch (Exception e) {
            estadoEnvio = "FALLIDO";
            System.out.println("Error API Brevo en evento " + request.getTipoEvento() + ": " + e.getMessage());
        }

        // Importante: Asumimos idUsuario = 1 temporalmente o lo recibes en el DTO si es necesario
        // En este ejemplo, pasaremos '1' como ID de usuario genérico o puedes añadir idUsuario a NotificacionEventoRequest
        return registrarLogTrasEnvio(1, brevoTemplateId.intValue(), estadoEnvio);
    }

    // Método privado que actúa como "Diccionario" de tus eventos
    private Long obtenerTemplateIdPorEvento(String tipoEvento) {
        return switch (tipoEvento.toUpperCase()) {
            case "RESERVA_ACEPTADA" -> 1L;   // Conecta con tu plantilla "#1 Confirmacion de paseo"
            case "RESERVA_RECHAZADA" -> 2L;  // Conecta con tu plantilla "#2 Rechazo de paseo"
            case "SOLICITUD_PASEO" -> 3L;    // Conecta con tu plantilla "#3 Nueva plantilla"
            default -> throw new IllegalArgumentException("Tipo de evento no soportado: " + tipoEvento);
        };
    }
}