package com.patiperro.notification_service.service;

import java.util.List;
import java.util.Map;

import jakarta.mail.internet.MimeMessage;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.patiperro.notification_service.dto.CorreoAceptacionRequest;
import com.patiperro.notification_service.dto.NotificacionEventoRequest;
import com.patiperro.notification_service.exception.BrevoEnvioFallidoException;
import com.patiperro.notification_service.model.LogEnvio;
import com.patiperro.notification_service.model.PlantillaCorreo;
import com.patiperro.notification_service.repository.LogEnvioRepository;
import com.patiperro.notification_service.repository.PlantillaRepository;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final PlantillaRepository plantillaRepository;
    private final LogEnvioRepository logEnvioRepository;
    private final JavaMailSender mailSender;

    /** Template Brevo para {@link PagoNotificacionService#TIPO_EVENTO_PAGO_CONFIRMADO}. */
    @Value("${patiperro.notification.brevo.template.PAGO_CONFIRMADO:4}")
    private Long brevoTemplatePagoConfirmado;

    @Value("${patiperro.notification.brevo.template.REEMBOLSO_RESERVA:5}")
    private Long brevoTemplateReembolsoReserva;

    @Value("${patiperro.notification.brevo.template.RESUMEN_COMPROBANTE_TUTOR:6}")
    private Long brevoTemplateResumenComprobanteTutor;

    @Value("${patiperro.notification.brevo.transactional.base-url:https://api.brevo.com/v3/smtp/email}")
    private String brevoTransactionalBaseUrl;

    @Value("${patiperro.notification.brevo.transactional.api-key:}")
    private String brevoTransactionalApiKey;

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

    private String requireBrevoTransactionalApiKey() {
        if (!StringUtils.hasText(brevoTransactionalApiKey)) {
            throw new IllegalStateException(
                    "Falta patiperro.notification.brevo.transactional.api-key o NOTIFICATION_BREVO_TRANSACTIONAL_API_KEY");
        }
        return brevoTransactionalApiKey.trim();
    }

    private WebClient.Builder brevoTransactionalWebClientBuilder() {
        String base = brevoTransactionalBaseUrl != null ? brevoTransactionalBaseUrl.trim() : "";
        if (!StringUtils.hasText(base)) {
            base = "https://api.brevo.com/v3/smtp/email";
        }
        return WebClient.builder().baseUrl(base);
    }

    @Transactional
    public LogEnvio procesarYEnviarViaBrevo(CorreoAceptacionRequest datos) {
        String apiKey = requireBrevoTransactionalApiKey();
        String estadoEnvio = "EXITOSO";

        try {
            Map<String, Object> body = Map.of(
                    "templateId", datos.idPlantillaBrevo(),
                    "to", List.of(Map.of("email", datos.correoTutor())),
                    "params",
                            Map.of(
                                    "nombrePaseador", datos.nombrePaseador(),
                                    "nombreMascota", datos.nombreMascota(),
                                    "montoTotal", datos.montoTotal(),
                                    "nombreTutor", datos.nombreTutor()));

            brevoTransactionalWebClientBuilder()
                    .build()
                    .post()
                    .header("api-key", apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.debug("Brevo transactional aceptó plantilla {}", datos.idPlantillaBrevo());
        } catch (Exception e) {
            estadoEnvio = "FALLIDO";
            log.warn("Brevo transactional falló (plantilla={}): {}", datos.idPlantillaBrevo(), e.getMessage());
        }

        return registrarLogTrasEnvio(datos.idUsuario(), datos.idPlantillaBrevo(), estadoEnvio);
    }


    /**
     * Envío por plantilla Brevo. Si la API o la red fallan, persiste log {@code FALLIDO} y lanza {@link BrevoEnvioFallidoException}
     * (sin rollback de ese intento de auditoría).
     */
    @Transactional(noRollbackFor = BrevoEnvioFallidoException.class)
    public LogEnvio procesarEventoUniversal(NotificacionEventoRequest request) {
        String apiKey = requireBrevoTransactionalApiKey();
        Long brevoTemplateId = obtenerTemplateIdPorEvento(request.getTipoEvento());
        Map<String, Object> params = request.getVariables() != null ? request.getVariables() : Map.of();

        try {
            Map<String, Object> body = Map.of(
                    "templateId", brevoTemplateId,
                    "to", List.of(Map.of("email", request.getEmailDestino())),
                    "params", params);

            brevoTransactionalWebClientBuilder()
                    .build()
                    .post()
                    .header("api-key", apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.debug("Brevo aceptó evento {}", request.getTipoEvento());
        } catch (Exception e) {
            log.warn(
                    "Brevo o red falló para evento {}: {}",
                    request.getTipoEvento(),
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            try {
                registrarLogTrasEnvio(1, brevoTemplateId.intValue(), "FALLIDO");
            } catch (RuntimeException logErr) {
                log.warn(
                        "No se pudo persistir log FALLIDO tras error Brevo (evento={})",
                        request.getTipoEvento(),
                        logErr);
            }
            throw new BrevoEnvioFallidoException(request.getTipoEvento(), e);
        }
        return registrarLogTrasEnvio(1, brevoTemplateId.intValue(), "EXITOSO");
    }

    /**
     * Mapa tipo de evento → plantilla Brevo. {@code REEMBOLSO_RESERVA}: correo al tutor tras devolución MP
     * ({@code POST /internal/pagos/reembolso-tutor} y {@code POST /internal/pagos/reembolso-procesado}).
     */
    private Long obtenerTemplateIdPorEvento(String tipoEvento) {
        return switch (tipoEvento.toUpperCase()) {
            case "RESERVA_ACEPTADA" -> 1L;   // Conecta con tu plantilla "#1 Confirmacion de paseo"
            case "RESERVA_RECHAZADA" -> 2L;  // Conecta con tu plantilla "#2 Rechazo de paseo"
            case "SOLICITUD_PASEO" -> 3L;    // Conecta con tu plantilla "#3 Nueva plantilla"
            case "PAGO_CONFIRMADO" -> brevoTemplatePagoConfirmado != null ? brevoTemplatePagoConfirmado : 4L;
            case "REEMBOLSO_RESERVA" -> brevoTemplateReembolsoReserva != null ? brevoTemplateReembolsoReserva : 5L;
            case "RESUMEN_COMPROBANTE_TUTOR" ->
                    brevoTemplateResumenComprobanteTutor != null ? brevoTemplateResumenComprobanteTutor : 6L;
            default -> throw new IllegalArgumentException("Tipo de evento no soportado: " + tipoEvento);
        };
    }
}