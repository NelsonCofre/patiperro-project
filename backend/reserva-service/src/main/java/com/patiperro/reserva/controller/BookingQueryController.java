package com.patiperro.reserva.controller;

import com.patiperro.reserva.dto.CodigoReservaValidarRequestDTO;
import com.patiperro.reserva.dto.CodigoReservaValidarResponseDTO;
import com.patiperro.reserva.dto.BookingTimelineResponseDTO;
import com.patiperro.reserva.dto.EstadoEncuentroResponseDTO;
import com.patiperro.reserva.dto.ReservaTutorDetalleResponseDTO;
import com.patiperro.reserva.service.ReservaService;
import com.patiperro.reserva.support.BookingTokenExtractor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints "booking" orientados a lectura para clientes web.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookingQueryController {

    private final ReservaService reservaService;

    /**
     * Reemplazo compatible de {@code GET /api/tutor/bookings}: toma el tutorId del JWT.
     */
    @GetMapping("/tutor/bookings")
    public List<ReservaTutorDetalleResponseDTO> tutorBookings(HttpServletRequest request) {
        String jwt = BookingTokenExtractor.extractRawJwt(request).orElse(null);
        return reservaService.listarBookingsTutorDesdeJwt(jwt);
    }

    /**
     * Línea de tiempo de estado para una reserva específica.
     */
    @GetMapping("/bookings/{id}/timeline")
    public BookingTimelineResponseDTO bookingTimeline(
            @PathVariable Integer id,
            HttpServletRequest request) {
        String jwt = BookingTokenExtractor.extractRawJwt(request).orElse(null);
        return reservaService.obtenerTimelineReserva(id, jwt);
    }

    /**
     * Validación de código de encuentro (paseador).
     */
    @PostMapping("/reservas/codigo/validar")
    public CodigoReservaValidarResponseDTO validarCodigoReserva(
            @Valid @RequestBody CodigoReservaValidarRequestDTO body,
            HttpServletRequest request) {
        String jwt = BookingTokenExtractor.extractRawJwt(request).orElse(null);
        return reservaService.validarCodigoEncuentro(body, jwt);
    }

    @GetMapping("/reservas/{id}/estado-encuentro")
    public EstadoEncuentroResponseDTO estadoEncuentro(
            @PathVariable Integer id,
            HttpServletRequest request) {
        String jwt = BookingTokenExtractor.extractRawJwt(request).orElse(null);
        return reservaService.obtenerEstadoEncuentro(id, jwt);
    }
}

