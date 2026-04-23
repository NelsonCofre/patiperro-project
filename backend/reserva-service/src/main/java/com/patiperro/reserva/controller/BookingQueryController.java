package com.patiperro.reserva.controller;

import com.patiperro.reserva.dto.CodigoReservaValidarRequestDTO;
import com.patiperro.reserva.dto.CodigoReservaValidarResponseDTO;
import com.patiperro.reserva.dto.BookingTimelineResponseDTO;
import com.patiperro.reserva.dto.EstadoEncuentroResponseDTO;
import com.patiperro.reserva.dto.ReservaTutorDetalleResponseDTO;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Endpoints orientados a flujos de reserva (booking) para el cliente web: consultas
 * y la acción de validar el código de encuentro (cambia el estado a EN_CURSO).
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Booking (consultas y código de encuentro)")
public class BookingQueryController {

    private static final String MSG_SE_REQUIERE_JWT =
            "Se requiere autenticación (cookie access_token o encabezado Authorization: Bearer)";

    private final ReservaService reservaService;

    @GetMapping("/tutor/bookings")
    @Operation(summary = "Listar reservas del tutor (JWT)", description = "Toma el tutorId desde el claim del JWT; cookie access_token o Bearer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de reservas con detalle"),
            @ApiResponse(responseCode = "401", description = "Sin token"),
            @ApiResponse(responseCode = "400", description = "Token inválido o claim faltante")
    })
    public List<ReservaTutorDetalleResponseDTO> tutorBookings(HttpServletRequest request) {
        return reservaService.listarBookingsTutorDesdeJwt(exigirJwt(request));
    }

    @GetMapping("/bookings/{id}/timeline")
    @Operation(summary = "Línea de tiempo de estados de una reserva", description = "Requiere JWT de tutor o paseador con acceso a la reserva.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Etapas solicitada, aceptada, en curso, finalizada"),
            @ApiResponse(responseCode = "401", description = "Sin token"),
            @ApiResponse(responseCode = "400", description = "Sin permiso o token inválido")
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

    private static String exigirJwt(HttpServletRequest request) {
        return BookingTokenExtractor.extractRawJwt(request)
                .filter(j -> j != null && !j.isBlank())
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, MSG_SE_REQUIERE_JWT));
    }

    @GetMapping("/reservas/{id}/estado-encuentro")
    public EstadoEncuentroResponseDTO estadoEncuentro(
            @PathVariable Integer id,
            HttpServletRequest request) {
        String jwt = BookingTokenExtractor.extractRawJwt(request).orElse(null);
        return reservaService.obtenerEstadoEncuentro(id, jwt);
    }
}
