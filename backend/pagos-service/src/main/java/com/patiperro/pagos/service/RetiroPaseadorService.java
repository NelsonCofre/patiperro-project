package com.patiperro.pagos.service;

import com.patiperro.pagos.dto.billetera.RetiroPaseadorResponse;
import com.patiperro.pagos.model.Billetera;
import com.patiperro.pagos.model.Destino;
import com.patiperro.pagos.model.EstadoPago;
import com.patiperro.pagos.model.Origen;
import com.patiperro.pagos.model.TipoTransaccion;
import com.patiperro.pagos.model.Transaccion;
import com.patiperro.pagos.repository.BilleteraRepository;
import com.patiperro.pagos.repository.TransaccionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Retiro desde saldo disponible: una transacción atómica con bloqueo pesimístico ({@code FOR UPDATE}) sobre la fila
 * {@link Billetera} para evitar retiros concurrentes por encima del saldo.
 */
@Service
@RequiredArgsConstructor
public class RetiroPaseadorService {

    private static final Logger log = LoggerFactory.getLogger(RetiroPaseadorService.class);

    private static final int SCALE = 2;

    private final BilleteraRepository billeteraRepository;
    private final TransaccionRepository transaccionRepository;

    @Transactional
    public RetiroPaseadorResponse solicitarRetiro(Long idUsuarioPaseador, BigDecimal montoSolicitado) {
        if (idUsuarioPaseador == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario no identificado");
        }
        if (montoSolicitado == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto es obligatorio");
        }
        BigDecimal monto = montoSolicitado.setScale(SCALE, RoundingMode.HALF_UP);
        if (monto.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto debe ser mayor a cero");
        }

        asegurarFilaBilletera(idUsuarioPaseador);

        Billetera b = billeteraRepository
                .findByIdUsuarioForUpdate(idUsuarioPaseador)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo bloquear la billetera"));

        BigDecimal disponible = nz(b.getSaldoActual());
        if (disponible.compareTo(monto) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saldo insuficiente");
        }

        BigDecimal nuevoSaldo = disponible.subtract(monto).setScale(SCALE, RoundingMode.HALF_UP);
        b.setSaldoActual(nuevoSaldo);
        billeteraRepository.save(b);

        Transaccion tx = transaccionRepository.save(
                Transaccion.builder()
                        .idReserva(null)
                        .idPago(null)
                        .montoBruto(monto)
                        .comisionApp(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP))
                        .montoNeto(monto)
                        .origen(Origen.PASEADOR)
                        .destino(Destino.BANCO)
                        .estadoPago(EstadoPago.PENDIENTE)
                        .tipoTransaccion(TipoTransaccion.RETIRO_PASEADOR)
                        .billetera(b)
                        .build());

        log.info(
                "Retiro paseador registrado: usuario={} monto={} idTransaccion={} saldoRestante={}",
                idUsuarioPaseador,
                monto,
                tx.getIdTransaccion(),
                nuevoSaldo);

        return new RetiroPaseadorResponse(tx.getIdTransaccion(), monto, nuevoSaldo);
    }

    /**
     * Garantiza fila {@link Billetera} antes del {@code FOR UPDATE} (primera operación del usuario).
     */
    private void asegurarFilaBilletera(Long idUsuarioPaseador) {
        if (billeteraRepository.findByIdUsuario(idUsuarioPaseador).isPresent()) {
            return;
        }
        try {
            billeteraRepository.saveAndFlush(
                    Billetera.builder()
                            .idUsuario(idUsuarioPaseador)
                            .saldoActual(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP))
                            .saldoRetenido(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP))
                            .saldoVerificacion(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP))
                            .build());
        } catch (DataIntegrityViolationException ignored) {
            // Otro hilo creó la fila; continuar al lock.
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        if (v == null) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return v.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
