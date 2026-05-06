package com.patiperro.reserva.scheduler;

import com.patiperro.reserva.model.EstadoReservaCatalogo;
import com.patiperro.reserva.repository.ReservaRepository;
import com.patiperro.reserva.service.ReservaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 1) Regenera el PIN de reservas ACEPTADAS sin inicio de paseo cuya ventana de PIN venció. 2) Rellena
 * {@code codigoEncuentroExpiraEn} si faltaba.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "patiperro.reserva.aceptada-mantenimiento.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ReservaAceptadaMantenimientoScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservaAceptadaMantenimientoScheduler.class);

    private final ReservaRepository reservaRepository;
    private final ReservaService reservaService;
    private final Clock clock;

    @Scheduled(
            initialDelayString = "${patiperro.reserva.aceptada-mantenimiento.initial-delay-ms:120000}",
            fixedDelayString = "${patiperro.reserva.aceptada-mantenimiento.fixed-delay-ms:300000}")
    public void ejecutar() {
        LocalDateTime ahora = LocalDateTime.now(clock);
        int idAceptada = EstadoReservaCatalogo.ID_ACEPTADA;

        List<Integer> regenerar = reservaRepository.findIdReservasAceptadaParaRegenerarCodigoPorEncuentroVencido(idAceptada, ahora);
        for (Integer id : regenerar) {
            try {
                reservaService.regenerarCodigoEncuentroPorExpiracionJobItem(id);
            } catch (Exception e) {
                log.warn("Regeneración automática de PIN fallida, idReserva={}", id, e);
            }
        }

        List<Integer> rellenar = reservaRepository.findIdReservasAceptadaConCodigoSinExpiracion(idAceptada);
        for (Integer id : rellenar) {
            try {
                reservaService.rellenarCodigoEncuentroExpiraJobItem(id);
            } catch (Exception e) {
                log.warn("Relleno de expiración de PIN omitido o fallido, idReserva={}", id, e);
            }
        }
    }
}
