package com.patiperro.pagos.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patiperro.pagos.dto.comprobante.ComprobantePagoResponse;
import com.patiperro.pagos.model.ComprobantePago;
import com.patiperro.pagos.model.ComprobantePagoSnapshotSchema;
import com.patiperro.pagos.repository.ComprobantePagoRepository;
import com.patiperro.pagos.reserva.ReservaConsultaClient;
import com.patiperro.pagos.reserva.dto.ReservaComprobanteDto;
import com.patiperro.pagos.support.NotificationResumenComprobanteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Generación idempotente del comprobante tras reserva PAGADA (llamada servidor-a-servidor).
 * <p>Las llamadas HTTP (reserva, notification) se ejecutan fuera de transacción JPA para no retener
 * conexiones del pool durante la latencia de red.</p>
 */
@Service
public class ComprobantePagoGeneracionService {

    public static final String MSG_NO_PAGO_APROBADO = "NO_PAGO_APROBADO";

    public static final String MSG_SNAPSHOT_JSON_ERROR = "SNAPSHOT_JSON_ERROR";

    public static final String MSG_SIN_ID_TUTOR_EN_RESERVA = "SIN_ID_TUTOR_EN_RESERVA";

    public static final String MSG_UPSERT_SIN_RESULTADO = "UPSERT_COMPROBANTE_SIN_RESULTADO";

    private static final Logger log = LoggerFactory.getLogger(ComprobantePagoGeneracionService.class);

    private final ReservaConsultaClient reservaConsultaClient;
    private final ComprobantePagoConstruccionService construccionService;
    private final ComprobantePagoRepository comprobantePagoRepository;
    private final ObjectMapper objectMapper;
    private final ComprobantePagoHtmlRenderer htmlRenderer;
    private final NotificationResumenComprobanteClient notificationResumenComprobanteClient;
    private final TransactionTemplate transactionTemplate;

    public ComprobantePagoGeneracionService(
            ReservaConsultaClient reservaConsultaClient,
            ComprobantePagoConstruccionService construccionService,
            ComprobantePagoRepository comprobantePagoRepository,
            ObjectMapper objectMapper,
            ComprobantePagoHtmlRenderer htmlRenderer,
            NotificationResumenComprobanteClient notificationResumenComprobanteClient,
            TransactionTemplate transactionTemplate) {
        this.reservaConsultaClient = reservaConsultaClient;
        this.construccionService = construccionService;
        this.comprobantePagoRepository = comprobantePagoRepository;
        this.objectMapper = objectMapper;
        this.htmlRenderer = htmlRenderer;
        this.notificationResumenComprobanteClient = notificationResumenComprobanteClient;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Persiste snapshot y envía correo al tutor (best-effort) si aplica.
     *
     * @throws IllegalArgumentException reserva no encontrada / datos inválidos vía cliente reserva
     * @throws IllegalStateException    {@link #MSG_NO_PAGO_APROBADO} sin transacción aprobada
     */
    public void generarPostPago(Integer idReserva, boolean reenviarCorreo) {
        if (idReserva == null) {
            throw new IllegalArgumentException(ReservaConsultaClient.MSG_ID_RESERVA_OBLIGATORIO);
        }
        long idReservaLong = idReserva.longValue();

        ReservaComprobanteDto r = reservaConsultaClient.obtenerComprobanteInterno(idReservaLong);

        Optional<ComprobantePagoResponse> respuesta =
                construccionService.construirSiHayPagoAprobado(r, idReservaLong);
        if (respuesta.isEmpty()) {
            throw new IllegalStateException(MSG_NO_PAGO_APROBADO);
        }

        ComprobantePagoResponse dto = respuesta.get();
        String json;
        try {
            json = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(MSG_SNAPSHOT_JSON_ERROR, e);
        }

        if (r.idTutorUsuario() == null) {
            throw new IllegalStateException(MSG_SIN_ID_TUTOR_EN_RESERVA);
        }
        long idTutor = r.idTutorUsuario().longValue();
        Long idTx = dto.idOrden();
        if (idTx == null) {
            throw new IllegalStateException(MSG_NO_PAGO_APROBADO);
        }

        ComprobantePago guardado =
                transactionTemplate.execute(status -> upsertSnapshot(idReservaLong, idTutor, idTx, json));
        if (guardado == null) {
            throw new IllegalStateException(MSG_UPSERT_SIN_RESULTADO);
        }

        boolean debeEnviar =
                StringUtils.hasText(r.tutorCorreo())
                        && notificationResumenComprobanteClient.isEnabled()
                        && (reenviarCorreo || guardado.getCorreoEnviadoEn() == null);

        if (!debeEnviar) {
            return;
        }

        String html = htmlRenderer.render(dto);
        boolean enviado =
                notificationResumenComprobanteClient.enviarResumenHtml(idReserva, r.tutorCorreo().trim(), html);
        if (enviado) {
            persistCorreoEnviadoEn(idReservaLong, LocalDateTime.now());
        } else {
            log.warn("Comprobante tutor: correo no confirmado por notification-service (idReserva={})", idReserva);
        }
    }

    /** Transacción corta: marca correo tras respuesta exitosa de notification (relee fila por id_reserva). */
    private void persistCorreoEnviadoEn(long idReserva, LocalDateTime cuando) {
        transactionTemplate.executeWithoutResult(
                status ->
                        comprobantePagoRepository
                                .findByIdReserva(idReserva)
                                .ifPresentOrElse(
                                        row -> {
                                            row.setCorreoEnviadoEn(cuando);
                                            comprobantePagoRepository.save(row);
                                        },
                                        () ->
                                                log.warn(
                                                        "Comprobante tutor: no hay fila para marcar correo (idReserva={})",
                                                        idReserva)));
    }

    private ComprobantePago upsertSnapshot(long idReserva, long idTutor, Long idTransaccion, String json) {
        Optional<ComprobantePago> existing = comprobantePagoRepository.findByIdReserva(idReserva);
        if (existing.isPresent()) {
            ComprobantePago e = existing.get();
            e.setIdTransaccion(idTransaccion);
            e.setIdTutorUsuario(idTutor);
            e.setSnapshotJson(json);
            e.setSnapshotSchemaVersion(ComprobantePagoSnapshotSchema.CURRENT);
            return comprobantePagoRepository.save(e);
        }
        ComprobantePago n = ComprobantePago.builder()
                .idReserva(idReserva)
                .idTransaccion(idTransaccion)
                .idTutorUsuario(idTutor)
                .snapshotJson(json)
                .snapshotSchemaVersion(ComprobantePagoSnapshotSchema.CURRENT)
                .build();
        try {
            return comprobantePagoRepository.save(n);
        } catch (DataIntegrityViolationException ex) {
            ComprobantePago concurrent = comprobantePagoRepository
                    .findByIdReserva(idReserva)
                    .orElseThrow(() -> ex);
            concurrent.setIdTransaccion(idTransaccion);
            concurrent.setIdTutorUsuario(idTutor);
            concurrent.setSnapshotJson(json);
            concurrent.setSnapshotSchemaVersion(ComprobantePagoSnapshotSchema.CURRENT);
            return comprobantePagoRepository.save(concurrent);
        }
    }
}
