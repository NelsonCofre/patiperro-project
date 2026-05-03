package com.patiperro.pagos.checkout;

import com.patiperro.pagos.checkout.dto.CheckoutProResponseDto;
import com.patiperro.pagos.checkout.dto.MercadoPagoPreferenceCreatedDto;
import com.patiperro.pagos.model.Destino;
import com.patiperro.pagos.model.EstadoPago;
import com.patiperro.pagos.model.Origen;
import com.patiperro.pagos.model.TipoTransaccion;
import com.patiperro.pagos.model.Transaccion;
import com.patiperro.pagos.repository.TransaccionRepository;
import com.patiperro.pagos.reserva.ReservaConsultaClient;
import com.patiperro.pagos.reserva.dto.ReservaConsultaDto;
import com.patiperro.pagos.support.MercadoPagoApiClient;
import com.patiperro.pagos.support.ReservaPagosIntegracionClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CheckoutProService {

    private final ReservaConsultaClient reservaConsultaClient;
    private final MercadoPagoApiClient mercadoPagoApiClient;
    private final TransaccionRepository transaccionRepository;
    private final ReservaPagosIntegracionClient reservaPagosIntegracionClient;

    @Value("${patiperro.pagos.checkout.front-base-url:http://localhost:5173}")
    private String frontBaseUrl;

    @Value("${patiperro.pagos.checkout.notification-url:}")
    private String notificationUrl;

    @Value("${patiperro.pagos.checkout.currency-id:CLP}")
    private String currencyId;

    @Value("${patiperro.mercadopago.checkout-use-sandbox:true}")
    private boolean checkoutUseSandbox;

    @Transactional
    public CheckoutProResponseDto iniciar(long idReserva, long usuarioIdAutenticado) {
        ReservaConsultaDto reserva;
        try {
            reserva = reservaConsultaClient.obtenerReservaParaPago(idReserva);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        if (!Long.valueOf(usuarioIdAutenticado).equals(reserva.idTutorUsuario())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La reserva no pertenece al tutor autenticado");
        }
        if (reserva.montoTotal() == null || reserva.montoTotal().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Monto inválido");
        }

        long unitPrice = reserva.montoTotal().setScale(0, RoundingMode.HALF_UP).longValue();
        String extRef = String.valueOf(reserva.idReserva());

        Transaccion tx = transaccionRepository
                .findFirstByIdReservaAndEstadoPagoOrderByIdTransaccionDesc(reserva.idReserva(), EstadoPago.PENDIENTE)
                .map(existing -> {
                    existing.setMontoBruto(reserva.montoTotal());
                    existing.setComisionApp(BigDecimal.ZERO);
                    existing.setMontoNeto(reserva.montoTotal());
                    return existing;
                })
                .orElseGet(() -> Transaccion.builder()
                        .idReserva(reserva.idReserva())
                        .montoBruto(reserva.montoTotal())
                        .comisionApp(BigDecimal.ZERO)
                        .montoNeto(reserva.montoTotal())
                        .origen(Origen.CLIENTE)
                        .destino(Destino.BANCO)
                        .estadoPago(EstadoPago.PENDIENTE)
                        .tipoTransaccion(TipoTransaccion.PAGO_CLIENTE)
                        .build());
        transaccionRepository.save(tx);
        reservaPagosIntegracionClient.vincularTransaccionReserva(reserva.idReserva().intValue(), tx.getIdTransaccion());

        String base = trimTrailingSlash(frontBaseUrl);
        String qTotal = URLEncoder.encode(String.valueOf(unitPrice), StandardCharsets.UTF_8);
        String success = base + "/tutor/reservas?checkout=ok";
        String failure = base + "/tutor/pago-reserva?idReserva=" + extRef + "&total=" + qTotal + "&checkout=fail";
        String pending = base + "/tutor/pago-reserva?idReserva=" + extRef + "&total=" + qTotal + "&checkout=pending";

        String title = "Reserva #" + extRef;
        Optional<MercadoPagoPreferenceCreatedDto> prefOpt = mercadoPagoApiClient.crearPreferenciaCheckoutPro(
                title,
                currencyId,
                unitPrice,
                extRef,
                notificationUrl,
                success,
                failure,
                pending);

        MercadoPagoPreferenceCreatedDto created = prefOpt.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo crear la preferencia en Mercado Pago"));

        String initPoint = checkoutUseSandbox && StringUtils.hasText(created.sandboxInitPoint())
                ? created.sandboxInitPoint()
                : created.initPoint();
        if (!StringUtils.hasText(initPoint)) {
            initPoint = created.initPoint();
        }
        if (!StringUtils.hasText(initPoint)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Mercado Pago no devolvió URL de checkout");
        }

        return new CheckoutProResponseDto(initPoint, created.preferenceId());
    }

    private static String trimTrailingSlash(String u) {
        if (u == null || u.isBlank()) {
            return "http://localhost:5173";
        }
        String t = u.trim();
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }
}
