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
 * 1) Cancela reservas ACEPTADAS sin inicio de paseo cuya ventana de PIN venció (libera bloque en agenda). 2) Rellena
 * {@code codigoEncuentroExpiraEn} si faltaba. La cancelación va antes para no competir con un “relleno” previo a la
 * heurística de expiración.
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

    @Scheduled(fixedDelayString = "${patiperro.reserva.aceptada-mantenimiento.fixed-delay-ms:300000}")
    public void ejecutar() {
        LocalDateTime ahora = LocalDateTime.now(clock);
        int idAceptada = EstadoReservaCatalogo.ID_ACEPTADA;

        List<Integer> cancelar = reservaRepository.findIdReservasAceptadaParaCancelarPorEncuentroVencido(idAceptada, ahora);
        for (Integer id : cancelar) {
            try {
                reservaService.cancelarAceptadaPorEncuentroVencidoJobItem(id);
            } catch (Exception e) {
                log.warn("Cancelación automática por PIN vencido fallida, idReserva={}", id, e);
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
