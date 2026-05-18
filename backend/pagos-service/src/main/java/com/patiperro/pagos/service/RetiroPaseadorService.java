package com.patiperro.pagos.service;

import com.patiperro.pagos.config.RetiroProperties;
import com.patiperro.pagos.dto.billetera.RetiroHistorialItemResponse;
import com.patiperro.pagos.dto.billetera.RetiroPaseadorResponse;
import com.patiperro.pagos.model.Banco;
import com.patiperro.pagos.model.Billetera;
import com.patiperro.pagos.model.Cuenta;
import com.patiperro.pagos.model.Destino;
import com.patiperro.pagos.model.EstadoPago;
import com.patiperro.pagos.model.Origen;
import com.patiperro.pagos.model.RetiroFondo;
import com.patiperro.pagos.model.TipoCuenta;
import com.patiperro.pagos.model.TipoTransaccion;
import com.patiperro.pagos.model.Transaccion;
import com.patiperro.pagos.repository.BilleteraRepository;
import com.patiperro.pagos.repository.CuentaRepository;
import com.patiperro.pagos.repository.RetiroFondoRepository;
import com.patiperro.pagos.repository.TransaccionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Retiro desde saldo disponible: una transacción atómica con bloqueo pesimístico ({@code FOR UPDATE}) sobre la fila
 * {@link Billetera} para evitar retiros concurrentes por encima del saldo.
 */
@Service
@RequiredArgsConstructor
public class RetiroPaseadorService {

    private static final Logger log = LoggerFactory.getLogger(RetiroPaseadorService.class);

    private static final int SCALE = 2;
    private static final String MENSAJE_RETIRO_OK =
            "Su solicitud de retiro ha sido recibida y será procesada en las próximas 48 horas hábiles";

    private final BilleteraRepository billeteraRepository;
    private final CuentaRepository cuentaRepository;
    private final TransaccionRepository transaccionRepository;
    private final RetiroFondoRepository retiroFondoRepository;
    private final RetiroProperties retiroProperties;

    @Value("${patiperro.pagos.retiro.historial-max:100}")
    private int historialRetirosMax;

    @Transactional(readOnly = true)
    public List<RetiroHistorialItemResponse> listarHistorialRetiros(Long idUsuarioPaseador) {
        if (idUsuarioPaseador == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario no identificado");
        }
        int cap = Math.max(1, Math.min(historialRetirosMax, 500));
        String cuentaResumen = resumenCuentaDestino(idUsuarioPaseador);
        List<RetiroFondo> filas = retiroFondoRepository.findHistorialByIdUsuarioPaseador(
                idUsuarioPaseador, PageRequest.of(0, cap));
        List<RetiroHistorialItemResponse> salida = new ArrayList<>(filas.size());
        for (RetiroFondo r : filas) {
            Transaccion tx = r.getTransaccion();
            EstadoPago estado = tx != null ? tx.getEstadoPago() : EstadoPago.PENDIENTE;
            Long idTx = tx != null ? tx.getIdTransaccion() : null;
            salida.add(new RetiroHistorialItemResponse(
                    r.getIdRetiroFondos(),
                    idTx,
                    idTx != null ? "RET-" + idTx : "RET-" + r.getIdRetiroFondos(),
                    nz(r.getMonto()),
                    estado != null ? estado.name() : EstadoPago.PENDIENTE.name(),
                    etiquetaEstadoRetiro(estado),
                    r.getCreadoEn(),
                    cuentaResumen));
        }
        return salida;
    }

    @Transactional(rollbackFor = Exception.class)
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

        BigDecimal minimo = nz(retiroProperties.getMinimo());
        if (monto.compareTo(minimo) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El monto debe ser mayor o igual al mínimo de retiro (" + minimo + ")");
        }

        validarCuentaBancariaConfigurada(b);

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

        retiroFondoRepository.save(
                RetiroFondo.builder()
                        .transaccion(tx)
                        .idUsuarioPaseador(idUsuarioPaseador)
                        .monto(monto)
                        .creadoEn(LocalDateTime.now())
                        .build());

        return new RetiroPaseadorResponse(tx.getIdTransaccion(), monto, nuevoSaldo, MENSAJE_RETIRO_OK);
    }

    private void validarCuentaBancariaConfigurada(Billetera billetera) {
        Long idBilletera = billetera == null ? null : billetera.getIdBilletera();
        if (idBilletera == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Billetera inválida");
        }

        Cuenta cuenta = cuentaRepository.findByBilletera_IdBilletera(idBilletera)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Debe registrar su información bancaria antes de solicitar un retiro"));

        if (!StringUtils.hasText(cuenta.getNumeroCuenta()) || cuenta.getBanco() == null || cuenta.getTipoCuenta() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe registrar su información bancaria antes de solicitar un retiro");
        }
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

    private String resumenCuentaDestino(Long idUsuarioPaseador) {
        return billeteraRepository
                .findByIdUsuario(idUsuarioPaseador)
                .flatMap(b -> cuentaRepository.findByBilletera_IdBilletera(b.getIdBilletera()))
                .map(RetiroPaseadorService::formatearCuentaResumen)
                .orElse(null);
    }

    private static String formatearCuentaResumen(Cuenta cuenta) {
        if (cuenta == null) {
            return null;
        }
        Banco banco = cuenta.getBanco();
        TipoCuenta tipo = cuenta.getTipoCuenta();
        String bancoNombre = banco != null && StringUtils.hasText(banco.getNombre()) ? banco.getNombre().trim() : "Banco";
        String tipoNombre = tipo != null && StringUtils.hasText(tipo.getNombre()) ? tipo.getNombre().trim() : "Cuenta";
        String enmascarado = enmascararNumeroCuenta(cuenta.getNumeroCuenta());
        return bancoNombre + " · " + tipoNombre + " · " + enmascarado;
    }

    private static String enmascararNumeroCuenta(String numeroCuenta) {
        if (!StringUtils.hasText(numeroCuenta)) {
            return "****";
        }
        String digits = numeroCuenta.replaceAll("\\s+", "");
        if (digits.length() <= 4) {
            return "****" + digits;
        }
        return "****" + digits.substring(digits.length() - 4);
    }

    private static String etiquetaEstadoRetiro(EstadoPago estado) {
        if (estado == null) {
            return "Retiro en proceso";
        }
        return switch (estado) {
            case APROBADO -> "Completado";
            case RECHAZADO -> "Rechazado";
            case PENDIENTE -> "Retiro en proceso";
        };
    }

    private static BigDecimal nz(BigDecimal v) {
        if (v == null) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return v.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
