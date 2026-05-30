package com.patiperro.reserva.controller;

import com.patiperro.reserva.dto.CodigoReservaActivoResponseDTO;
import com.patiperro.reserva.dto.CodigoReservaValidarRequestDTO;
import com.patiperro.reserva.dto.CodigoReservaValidarResponseDTO;
import com.patiperro.reserva.dto.BookingTimelineResponseDTO;
import com.patiperro.reserva.dto.EstadoEncuentroResponseDTO;
import com.patiperro.reserva.dto.TutorCheckoutPreferenciaResponseDTO;
import com.patiperro.reserva.dto.ReservaTutorDetalleResponseDTO;
import com.patiperro.reserva.support.PagosCheckoutIntegracionClient;
import com.patiperro.reserva.service.ReservaService;
import com.patiperro.reserva.support.BookingTokenExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Endpoints orientados a flujos de reserva (booking) para el cliente web: consultas
 * (tutor/paseador) y acciones de encuentro: obtener PIN, validar PIN, estado del encuentro.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Booking (encuentro y consultas)")
public class BookingQueryController {

    private static final String MSG_SE_REQUIERE_JWT =
            "Se requiere autenticación (cookie access_token o encabezado Authorization: Bearer)";

    private final ReservaService reservaService;

    @GetMapping("/tutor/bookings")
    @Operation(summary = "Listar reservas del tutor (JWT)", description = "Toma el tutorId desde el claim del JWT; cookie access_token o Bearer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de reservas con detalle"),
            @ApiResponse(responseCode = "401", description = "Sin token"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida o token/claim inválido")
    })
    public List<ReservaTutorDetalleResponseDTO> tutorBookings(HttpServletRequest request) {
        return reservaService.listarBookingsTutorDesdeJwt(exigirJwt(request));
    }

    /**
     * Dos rutas equivalentes (gateway: {@code /api/bookings/**} y {@code /api/tutor/**} → reserva-service).
     */
    @PostMapping(value = {
            "/bookings/{id}/mercadopago/checkout",
            "/tutor/reservas/{id}/mercadopago/checkout"
    })
    @Operation(
            summary = "Iniciar o reintentar pago (Mercado Pago Checkout Pro)",
            description = "JWT de tutor. Opcional: cabecera Idempotency-Key para reintentos estables. "
                    + "Si la reserva está SOLICITADA, pasa a PENDIENTE_PAGO antes de llamar a pagos-service.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "preferenceId y urlCheckout para redirigir al tutor"),
            @ApiResponse(responseCode = "401", description = "Sin token o tutor no coincide"),
            @ApiResponse(responseCode = "404", description = "Reserva no existe"),
            @ApiResponse(responseCode = "409", description = "Estado no permite pagar / reintentar"),
            @ApiResponse(responseCode = "502", description = "pagos-service no pudo crear la preferencia"),
            @ApiResponse(responseCode = "503", description = "Integración pagos deshabilitada")
    })
    public TutorCheckoutPreferenciaResponseDTO iniciarCheckoutMercadoPago(
            @PathVariable Integer id,
            @RequestHeader(value = PagosCheckoutIntegracionClient.HEADER_IDEMPOTENCY_KEY, required = false) String idempotencyKey,
            HttpServletRequest request) {
        return reservaService.iniciarCheckoutMercadoPago(id, exigirJwt(request), idempotencyKey);
    }

    @GetMapping("/bookings/{id}/timeline")
    @Operation(summary = "Línea de tiempo de estados de una reserva", description = "Requiere JWT de tutor o paseador con acceso a la reserva.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Etapas solicitada, aceptada, en curso, finalizada"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/expirado"),
            @ApiResponse(responseCode = "400", description = "Sin acceso a la reserva o JWT sin claims esperados")
    })
    public BookingTimelineResponseDTO bookingTimeline(
            @PathVariable Integer id,
            HttpServletRequest request) {
        return reservaService.obtenerTimelineReserva(id, exigirJwt(request));
    }

    @PostMapping("/reservas/codigo/validar")
    @Operation(
            summary = "Validar código de encuentro (paseador)",
            description = "Comprueba el PIN de 4 dígitos; en éxito pasa la reserva a EN_CURSO y fija fecha_inicio_real. "
                    + "Requiere JWT de paseador y que el bloque de agenda corresponda al paseador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validación correcta"),
            @ApiResponse(responseCode = "400", description = "Pin incorrecto, estado inválido, etc."),
            @ApiResponse(responseCode = "401", description = "Sin token"),
            @ApiResponse(responseCode = "403", description = "Paseador no asignado a la reserva"),
            @ApiResponse(responseCode = "410", description = "Código expirado"),
            @ApiResponse(responseCode = "429", description = "Bloqueo temporal por intentos")
    })
    public CodigoReservaValidarResponseDTO validarCodigoReserva(
            @Valid @RequestBody CodigoReservaValidarRequestDTO body,
            HttpServletRequest request) {
        return reservaService.validarCodigoEncuentro(body, exigirJwt(request));
    }

    @GetMapping("/reservas/{id}/codigo")
    @Operation(
            summary = "Obtener código activo de una reserva (tutor)",
            description = "Retorna el PIN activo (4 dígitos) para la reserva en estado ACEPTADA. "
                    + "Requiere JWT de tutor y que el tutor sea propietario de la reserva.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código activo"),
            @ApiResponse(responseCode = "401", description = "Sin token"),
            @ApiResponse(responseCode = "403", description = "Tutor no es propietario"),
            @ApiResponse(responseCode = "400", description = "Estado inválido"),
            @ApiResponse(responseCode = "409", description = "Reserva sin código activo")
    })
    public CodigoReservaActivoResponseDTO obtenerCodigoActivoReserva(
            @PathVariable Integer id,
            HttpServletRequest request) {
        return reservaService.obtenerCodigoActivoReserva(id, exigirJwt(request));
    }

    @GetMapping("/reservas/{id}/estado-encuentro")
    @Operation(
            summary = "Estado del encuentro (tutor o paseador)",
            description = "Resumen de estado de la etapa de encuentro por PIN: pendiente, confirmado, o fallas/bloqueo por intentos. "
                    + "Requiere JWT de tutor o paseador con acceso a la reserva.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado calculado"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido/expirado"),
            @ApiResponse(responseCode = "400", description = "Sin acceso a la reserva o JWT sin claims esperados")
    })
    public EstadoEncuentroResponseDTO estadoEncuentro(
            @PathVariable Integer id,
            HttpServletRequest request) {
        return reservaService.obtenerEstadoEncuentro(id, exigirJwt(request));
    }

    private static String exigirJwt(HttpServletRequest request) {
        return BookingTokenExtractor.extractRawJwt(request)
                .filter(j -> j != null && !j.isBlank())
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, MSG_SE_REQUIERE_JWT));
    }
}
