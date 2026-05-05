package com.patiperro.reserva.service;

import com.patiperro.reserva.dto.BookingStatusPatchRequestDTO;
import com.patiperro.reserva.dto.BookingTimelineResponseDTO;
import com.patiperro.reserva.dto.CodigoReservaActivoResponseDTO;
import com.patiperro.reserva.dto.CodigoReservaValidarRequestDTO;
import com.patiperro.reserva.dto.CodigoReservaValidarResponseDTO;
import com.patiperro.reserva.dto.EstadoEncuentroResponseDTO;
import com.patiperro.reserva.dto.integracion.AgendaBloqueReservaClientDTO;
import com.patiperro.reserva.dto.AgendaBloqueResumenDTO;
import com.patiperro.reserva.dto.MascotaResumenDTO;
import com.patiperro.reserva.dto.PaseadorDecision;
import com.patiperro.reserva.dto.PaseadorResumenDTO;
import com.patiperro.reserva.dto.ReservaDtoMapper;
import com.patiperro.reserva.dto.ReservaRequestDTO;
import com.patiperro.reserva.dto.ReservaResponseDTO;
import com.patiperro.reserva.dto.MascotaPortadaUrlResponse;
import com.patiperro.reserva.dto.MascotaInternoDetalleResponseDTO;
import com.patiperro.reserva.dto.ReservaPaseadorSolicitudResponseDTO;
import com.patiperro.reserva.dto.ReservaParaPagoDto;
import com.patiperro.reserva.dto.interno.InternoBilleteraReservaDetalleDto;
import com.patiperro.reserva.dto.TutorCheckoutPreferenciaResponseDTO;
import com.patiperro.reserva.dto.ReservaTutorDetalleResponseDTO;
import com.patiperro.reserva.dto.BookingStatusPatchRequestDTO.TutorDecision;
import com.patiperro.reserva.dto.integracion.TutorReservaClientDTO;
import com.patiperro.reserva.model.EstadoReserva;
import com.patiperro.reserva.model.EstadoReservaCatalogo;
import com.patiperro.reserva.model.Reserva;
import com.patiperro.reserva.repository.ReservaRepository;
import com.patiperro.reserva.security.JwtService;
import com.patiperro.reserva.event.PaseoIniciadoDomainEvent;
import com.patiperro.reserva.event.ReservaEventPublisher;
import com.patiperro.reserva.support.AgendaIntegracionClient;
import com.patiperro.reserva.support.MascotaIntegracionClient;
import com.patiperro.reserva.support.PagosBilleteraIntegracionClient;
import com.patiperro.reserva.support.PagosCheckoutIntegracionClient;
import com.patiperro.reserva.support.PaseadorIntegracionClient;
import com.patiperro.reserva.support.TutorIntegracionClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private static final Logger log = LoggerFactory.getLogger(ReservaService.class);

    private final ReservaRepository reservaRepository;
    private final EstadoReservaService estadoReservaService;
    private final AgendaIntegracionClient agendaIntegracionClient;
    private final MascotaIntegracionClient mascotaIntegracionClient;
    private final PaseadorIntegracionClient paseadorIntegracionClient;
    private final TutorIntegracionClient tutorIntegracionClient;
    private final JwtService jwtService;
    private final Clock clock;
    private final ReservaEventPublisher reservaEventPublisher;
    private final PaseoInicioSideEffectsService paseoInicioSideEffectsService;
    private final ReservaReembolsoService reservaReembolsoService;
    private final ReservaPagoService reservaPagoService;
    private final PagosCheckoutIntegracionClient pagosCheckoutIntegracionClient;
    private final PagosBilleteraIntegracionClient pagosBilleteraIntegracionClient;

    @Value("${patiperro.reserva.codigo.validacion-expira-minutos:30}")
    private int codigoExpiraMinutos;
    @Value("${patiperro.reserva.codigo.max-intentos:3}")
    private int codigoMaxIntentos;
    @Value("${patiperro.reserva.codigo.bloqueo-minutos:5}")
    private int codigoBloqueoMinutos;

    /**
     * Si es true, STOMP/integración se ejecuta tras COMMIT. Si false, se mantiene el comportamiento
     * inline (útil para rollback de pruebas o despliegues sensibles).
     */
    @Value("${patiperro.reserva.paseo.efectos-despues-de-commit:true}")
    private boolean paseoEfectosDespuesDeCommit;

    /**
     * Si es true, bloquea transiciones a EN_CURSO vía {@code PATCH /status} (p. ej. {@code INICIAR_PASEO}).
     * El camino permitido queda en validar PIN. Desactiva explícitamente a {@code false} si aún dependen del PATCH.
     */
    @Value("${patiperro.reserva.paseo.bloquear-inicio-sin-codigo:true}")
    private boolean bloquearInicioSinCodigo;

    @Value("${patiperro.reserva.solicitud.expiracion-aceptacion.horas:24}")
    private long horasExpiracionAceptacion;

    public List<ReservaResponseDTO> listarTodas() {
        return reservaRepository.findAll().stream()
                .map(ReservaDtoMapper::toReservaResponse)
                .toList();
    }

    public ReservaResponseDTO obtenerPorId(Integer id) {
        return ReservaDtoMapper.toReservaResponse(obtenerEntidad(id));
    }

    @Transactional
    public ReservaResponseDTO crear(ReservaRequestDTO dto, String rawJwt) {
        EstadoReserva estado = estadoReservaService.obtenerPorNombreIgnoreCase(EstadoReservaCatalogo.NOMBRE_SOLICITADA);
        validarTutorJwt(dto.getIdTutorUsuario(), rawJwt);

        Reserva r = new Reserva();
        r.setIdReserva(null);
        r.setIdTutorUsuario(dto.getIdTutorUsuario());
        r.setIdMascota(dto.getIdMascota());
        r.setIdAgendaBloque(dto.getIdAgendaBloque());
        r.setIdTarifa(dto.getIdTarifa());
        // Marca de servidor para trazabilidad consistente (no depender del reloj del cliente).
        r.setFechaSolicitud(LocalDateTime.now(clock));
        r.setFechaAceptacion(dto.getFechaAceptacion());
        r.setMontoTotal(dto.getMontoTotal());
        r.setIdPago(dto.getIdPago());
        r.setEstadoReserva(estado);
        r.setFechaInicioReal(dto.getFechaInicioReal());
        r.setFechaFin(dto.getFechaFin());
        r.setCodigoEncuentro(dto.getCodigoEncuentro());
        r.setCodigoIntentosFallidos(0);
        r.setCodigoBloqueadoHasta(null);
        r.setMotivoRechazo(dto.getMotivoRechazo());
        r.setDetalleRechazo(dto.getDetalleRechazo());

        Reserva saved = reservaRepository.save(r);
        agendaIntegracionClient.marcarBloqueReservado(saved.getIdAgendaBloque(), rawJwt);
        return ReservaDtoMapper.toReservaResponse(saved);
    }

    /**
     * Endpoint legado-friendly: tutorId tomado desde claim {@code tutorId} del JWT.
     */
    public List<ReservaTutorDetalleResponseDTO> listarBookingsTutorDesdeJwt(String rawJwt) {
        if (rawJwt == null || rawJwt.isBlank()) {
            throw new IllegalArgumentException("Se requiere JWT de tutor");
        }
        if (!jwtService.isTokenValid(rawJwt)) {
            throw new IllegalArgumentException("Token inválido o expirado");
        }
        Long tutorId = jwtService.extractTutorId(rawJwt);
        if (tutorId == null) {
            throw new IllegalArgumentException("JWT sin claim tutorId");
        }
        return listarDetallePorTutor(tutorId.intValue(), rawJwt);
    }

    /**
     * Crea preferencia Mercado Pago Checkout Pro para el tutor autenticado.
     * Si la reserva está {@link EstadoReservaCatalogo#NOMBRE_SOLICITADA}, pasa a {@link EstadoReservaCatalogo#NOMBRE_PENDIENTE_PAGO}.
     */
    @Transactional
    public TutorCheckoutPreferenciaResponseDTO iniciarCheckoutMercadoPago(Integer idReserva, String rawJwt, String idempotencyKey) {
        Reserva r = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
        try {
            validarTutorJwt(r.getIdTutorUsuario(), rawJwt);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage());
        }
        if (!reservaPagoService.permiteReintentarPago(r)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La reserva no admite iniciar o reintentar el pago en este estado.");
        }
        if (!pagosCheckoutIntegracionClient.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Pago con pasarela no disponible (configuración).");
        }
        EstadoReserva est = r.getEstadoReserva();
        if (est != null && est.getIdEstadoReserva() != null
                && est.getIdEstadoReserva().equals(EstadoReservaCatalogo.ID_SOLICITADA)) {
            EstadoReserva pendiente = estadoReservaService.obtenerPorNombreIgnoreCase(EstadoReservaCatalogo.NOMBRE_PENDIENTE_PAGO);
            r.setEstadoReserva(pendiente);
            r = reservaRepository.save(r);
        }
        String titulo = "Reserva Patiperro #" + idReserva;
        Optional<TutorCheckoutPreferenciaResponseDTO> pref = pagosCheckoutIntegracionClient.crearPreferenciaCheckout(
                idReserva, r.getMontoTotal(), titulo, idempotencyKey);
        return pref.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "No se pudo crear la preferencia de pago. Intente más tarde."));
    }

    /**
     * Timeline de estados para la UI: solicitud, pasarela de pago, flujo del paseo y cierres excepcionales.
     */
    public BookingTimelineResponseDTO obtenerTimelineReserva(Integer idReserva, String rawJwt) {
        Reserva r = obtenerEntidad(idReserva);
        validarAccesoTimeline(r, rawJwt);
        EstadoReserva estadoActual = r.getEstadoReserva();

        List<BookingTimelineResponseDTO.TimelineStepDTO> steps = construirPasosTimeline(r, estadoActual);

        return new BookingTimelineResponseDTO(
                r.getIdReserva(),
                r.getIdTutorUsuario(),
                r.getIdMascota(),
                r.getIdAgendaBloque(),
                estadoActual != null ? estadoActual.getIdEstadoReserva() : null,
                estadoActual != null ? estadoActual.getNombreEstado() : null,
                steps
        );
    }

    private List<BookingTimelineResponseDTO.TimelineStepDTO> construirPasosTimeline(Reserva r, EstadoReserva estadoActual) {
        List<BookingTimelineResponseDTO.TimelineStepDTO> steps = new ArrayList<>();
        steps.add(new BookingTimelineResponseDTO.TimelineStepDTO(
                "solicitada",
                "Solicitada",
                r.getFechaSolicitud() != null,
                r.getFechaSolicitud()));

        boolean pasoPagoPasarela = esPendientePago(estadoActual)
                || reservaTieneCobroMercadoPago(r)
                || esAceptada(estadoActual)
                || esEnCurso(estadoActual)
                || esFinalizada(estadoActual);
        steps.add(new BookingTimelineResponseDTO.TimelineStepDTO(
                "pendiente_pago",
                "Pago (pasarela)",
                pasoPagoPasarela,
                esPendientePago(estadoActual) ? r.getFechaSolicitud() : null));

        boolean cobroRetenido = reservaTieneCobroMercadoPago(r)
                || esAceptada(estadoActual)
                || esEnCurso(estadoActual)
                || esFinalizada(estadoActual);
        steps.add(new BookingTimelineResponseDTO.TimelineStepDTO(
                "pago_confirmado",
                "Pago confirmado",
                cobroRetenido,
                cobroRetenido ? r.getFechaSolicitud() : null));

        steps.add(new BookingTimelineResponseDTO.TimelineStepDTO(
                "aceptada",
                "Aceptada por el paseador",
                r.getFechaAceptacion() != null,
                r.getFechaAceptacion()));
        steps.add(new BookingTimelineResponseDTO.TimelineStepDTO(
                "en_curso",
                "En curso",
                r.getFechaInicioReal() != null,
                r.getFechaInicioReal()));

        boolean finalizadaExitosa = esFinalizada(estadoActual);
        steps.add(new BookingTimelineResponseDTO.TimelineStepDTO(
                "finalizada",
                "Finalizada",
                finalizadaExitosa,
                finalizadaExitosa ? r.getFechaFin() : null));

        if (esRechazada(estadoActual)) {
            steps.add(new BookingTimelineResponseDTO.TimelineStepDTO(
                    "rechazada",
                    "Rechazada por el paseador",
                    true,
                    r.getFechaFin()));
        }
        if (esExpirada(estadoActual)) {
            steps.add(new BookingTimelineResponseDTO.TimelineStepDTO(
                    "expirada",
                    "Expirada (sin respuesta del paseador)",
                    true,
                    r.getFechaFin()));
        }
        if (esCancelada(estadoActual)) {
            steps.add(new BookingTimelineResponseDTO.TimelineStepDTO(
                    "cancelada",
                    "Cancelada por el tutor",
                    true,
                    r.getFechaFin()));
        }
        return steps;
    }

    private static boolean reservaTieneCobroMercadoPago(Reserva r) {
        if (r == null || r.getMercadopagoPaymentId() == null) {
            return false;
        }
        return !r.getMercadopagoPaymentId().isBlank();
    }

    /**
     * Retorna el PIN activo de una reserva para el tutor propietario.
     * Si está vencido, regenera un nuevo PIN y reinicia su expiración.
     */
    @Transactional
    public CodigoReservaActivoResponseDTO obtenerCodigoActivoReserva(Integer idReserva, String rawJwt) {
        if (rawJwt == null || rawJwt.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Se requiere autenticación");
        }
        if (!jwtService.isTokenValid(rawJwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido o expirado");
        }
        Long tutorId = jwtService.extractTutorId(rawJwt);
        if (tutorId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "JWT sin claim tutorId");
        }

        Reserva r = obtenerEntidad(idReserva);
        if (!tutorId.equals(r.getIdTutorUsuario().longValue())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para ver esta reserva");
        }
        if (!esAceptada(r.getEstadoReserva())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La reserva no está en estado válido para mostrar código");
        }
        if (r.getCodigoEncuentro() == null || r.getCodigoEncuentroExpiraEn() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La reserva aún no tiene código activo");
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        boolean regenerado = false;
        if (ahora.isAfter(r.getCodigoEncuentroExpiraEn())) {
            r.setCodigoEncuentro(generarCodigoEncuentroUnicoExcluyendo(r.getIdReserva()));
            r.setCodigoEncuentroExpiraEn(ahora.plusMinutes(Math.max(1, codigoExpiraMinutos)));
            r.setCodigoIntentosFallidos(0);
            r.setCodigoBloqueadoHasta(null);
            r = reservaRepository.save(r);
            regenerado = true;
        }

        String codigo4 = String.format("%04d", Math.abs(r.getCodigoEncuentro() % 10000));
        long segundos = Math.max(0, Duration.between(ahora, r.getCodigoEncuentroExpiraEn()).getSeconds());
        return new CodigoReservaActivoResponseDTO(
                r.getIdReserva(),
                codigo4,
                r.getCodigoEncuentroExpiraEn(),
                segundos,
                regenerado
        );
    }

    @Transactional
    public ReservaResponseDTO actualizar(Integer id, ReservaRequestDTO dto, String rawJwt) {
        Reserva r = obtenerEntidad(id);
        EstadoReserva estado = estadoReservaService.obtenerEntidad(dto.getIdEstadoReserva());
        r.setIdTutorUsuario(dto.getIdTutorUsuario());
        r.setIdMascota(dto.getIdMascota());
        r.setIdAgendaBloque(dto.getIdAgendaBloque());
        r.setIdTarifa(dto.getIdTarifa());
        r.setFechaSolicitud(dto.getFechaSolicitud());
        r.setFechaAceptacion(dto.getFechaAceptacion());
        r.setMontoTotal(dto.getMontoTotal());
        r.setIdPago(dto.getIdPago());
        r.setEstadoReserva(estado);
        r.setFechaInicioReal(dto.getFechaInicioReal());
        r.setFechaFin(dto.getFechaFin());
        r.setCodigoEncuentro(dto.getCodigoEncuentro());
        r.setMotivoRechazo(dto.getMotivoRechazo());
        r.setDetalleRechazo(dto.getDetalleRechazo());
        return ReservaDtoMapper.toReservaResponse(reservaRepository.save(r));
    }

    /**
     * Decisión del paseador (aceptar / rechazar / …).
     * <p>Paso 5 (solo backend, PDF): si pasa a RECHAZADA y la reserva estaba PAGADA (cobro retenido), se programa
     * devolución MP tras commit mediante {@link #programarReembolsoMercadoPagoTrasCommit(Integer, Reserva)}.</p>
     */
    @Transactional
    public ReservaResponseDTO aplicarDecisionPaseador(Integer idReserva, BookingStatusPatchRequestDTO dto, String rawJwt) {
        Reserva r = obtenerEntidad(idReserva);
        validarPaseadorPropietarioReserva(r, rawJwt);
        EstadoReserva actual = r.getEstadoReserva();
        EstadoReserva nuevo = resolverEstadoDestino(dto);
        if (bloquearInicioSinCodigo && esEnCurso(nuevo)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El inicio del paseo debe confirmarse validando el código de encuentro");
        }
        validarTransicionPaseador(actual, nuevo);

        r.setEstadoReserva(nuevo);
        aplicarMarcaTiempoTransicion(r, nuevo);
        aplicarDatosDecisionPaseador(r, dto, nuevo, rawJwt);
        Reserva saved = reservaRepository.save(r);

        if (esEstadoRechazada(nuevo)) {
            agendaIntegracionClient.marcarBloqueDisponible(saved.getIdAgendaBloque(), rawJwt);
            if (esPagada(actual)) {
                programarReembolsoMercadoPagoTrasCommit(saved.getIdReserva(), saved);
            }
        }
        if (esFinalizada(nuevo)) {
            programarBilleteraPasarVerificacionTrasCommit(saved);
        }
        return ReservaDtoMapper.toReservaResponse(saved);
    }

    /**
     * Job: expira solicitud por plazo de aceptación (sin JWT). Idempotente si otro hilo ya cambió el estado.
     * <p>Paso 5 (PDF): expiración por plazo — si la reserva estaba PAGADA (cobro), misma devolución MP tras commit
     * que en rechazo/cancelación tutor con cobro.</p>
     */
    @Transactional
    public void expirarReservaPorPlazoAceptacionJobItem(Integer idReserva) {
        Reserva r = reservaRepository.findById(idReserva).orElse(null);
        if (r == null) {
            return;
        }
        LocalDateTime limite = LocalDateTime.now(clock).minusHours(Math.max(1, horasExpiracionAceptacion));
        if (r.getFechaSolicitud() == null || !r.getFechaSolicitud().isBefore(limite)) {
            return;
        }
        EstadoReserva est = r.getEstadoReserva();
        if (!(esSolicitada(est) || esPendientePago(est) || esPagada(est))) {
            return;
        }
        boolean eraPagada = esPagada(est);
        EstadoReserva expirada = estadoReservaService.obtenerPorNombreIgnoreCase(EstadoReservaCatalogo.NOMBRE_EXPIRADA);
        r.setEstadoReserva(expirada);
        aplicarMarcaTiempoTransicion(r, expirada);
        reservaRepository.save(r);
        agendaIntegracionClient.marcarBloqueDisponibleInterno(r.getIdAgendaBloque());
        if (eraPagada) {
            programarReembolsoMercadoPagoTrasCommit(idReserva, r);
        }
    }

    /**
     * Punto único para paso 5 (disparadores PDF): no llama a Mercado Pago dentro de la transacción actual.
     * El cobro debe estar referenciado en {@code mercadopago_payment_id} (persistido al aprobar pago en
     * {@link ReservaPagoService#marcarReservaComoPagada}); si falta, se registra advertencia para operación.
     *
     * @param reservaAuditoriaPaymentId reserva ya persistida con el estado de cierre; solo para comprobar
     *                                  si existe id de pago MP antes del async (puede ser {@code null}).
     */
    private void programarReembolsoMercadoPagoTrasCommit(Integer idReserva, Reserva reservaAuditoriaPaymentId) {
        if (idReserva == null) {
            return;
        }
        if (reservaAuditoriaPaymentId != null
                && !StringUtils.hasText(reservaAuditoriaPaymentId.getMercadopagoPaymentId())) {
            log.warn(
                    "Reembolso MP programado tras commit pero la reserva no tiene mercadopago_payment_id; "
                            + "ReservaReembolsoService no invocará pagos-service hasta existir id (idReserva={})",
                    idReserva);
        }
        Runnable despuesDeCommit = () -> {
            pagosBilleteraIntegracionClient.revertirRetenido(idReserva);
            reservaReembolsoService.procesarReembolsoYNotificar(idReserva);
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            despuesDeCommit.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                despuesDeCommit.run();
            }
        });
    }

    private void programarBilleteraPasarVerificacionTrasCommit(Reserva saved) {
        if (!pagosBilleteraIntegracionClient.isEnabled() || saved == null) {
            return;
        }
        Integer idR = saved.getIdReserva();
        LocalDateTime fin = saved.getFechaFin();
        Runnable run = () -> {
            Long uid = resolverIdUsuarioPaseador(saved);
            if (uid == null) {
                log.debug("Billetera verificación omitida: sin id usuario paseador (idReserva={})", saved.getIdReserva());
                return;
            }
            pagosBilleteraIntegracionClient.pasarAVerificacion(idR, uid, fin);
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            run.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                run.run();
            }
        });
    }

    private Long resolverIdUsuarioPaseador(Reserva r) {
        if (r == null || !agendaIntegracionClient.isEnabled()) {
            return null;
        }
        try {
            AgendaBloqueReservaClientDTO bloque = agendaIntegracionClient.obtenerBloquePorIdInterno(r.getIdAgendaBloque());
            if (bloque == null || bloque.getIdUsuario() == null) {
                return null;
            }
            return bloque.getIdUsuario().longValue();
        } catch (RuntimeException e) {
            log.warn("No se pudo resolver id usuario paseador (idReserva={})", r.getIdReserva(), e);
            return null;
        }
    }

    @Transactional
    public CodigoReservaValidarResponseDTO validarCodigoEncuentro(CodigoReservaValidarRequestDTO dto, String rawJwt) {
        Reserva r = obtenerEntidad(dto.getIdReserva());
        validarPaseadorPropietarioReserva(r, rawJwt);

        if (esEnCurso(r.getEstadoReserva())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El código de encuentro ya fue validado");
        }
        if (!esAceptada(r.getEstadoReserva())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La reserva no está en estado válido para validar código");
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        if (r.getCodigoBloqueadoHasta() != null && ahora.isBefore(r.getCodigoBloqueadoHasta())) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Vuelve a intentar cuando termine el bloqueo");
        }

        if (r.getCodigoEncuentro() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La reserva aún no tiene código de encuentro");
        }

        if (r.getCodigoEncuentroExpiraEn() == null) {
            rellenarCodigoEncuentroExpiraSiAplica(r.getIdReserva(), rawJwt);
            r = obtenerEntidad(r.getIdReserva());
        }
        if (r.getCodigoEncuentroExpiraEn() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "No se pudo validar la vigencia del código de encuentro");
        }
        if (ahora.isAfter(r.getCodigoEncuentroExpiraEn())) {
            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "El código ha expirado, solicita al Tutor que actualice el código");
        }

        String esperado = String.format("%04d", Math.abs(r.getCodigoEncuentro() % 10000));
        if (!esperado.equals(dto.getCodigoIngresado())) {
            reservaRepository.incrementarIntentosFallidosCodigo(r.getIdReserva());
            Reserva trasFallo = reservaRepository.findById(r.getIdReserva())
                    .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
            int intentos = intentosConFallback(trasFallo);
            if (intentos >= Math.max(1, codigoMaxIntentos)) {
                reservaRepository.fijarBloqueoCodigoHasta(
                        r.getIdReserva(), ahora.plusMinutes(Math.max(1, codigoBloqueoMinutos)));
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Código incorrecto, inténtalo nuevamente");
        }

        EstadoReserva enCurso = estadoReservaService.obtenerPorNombreIgnoreCase(EstadoReservaCatalogo.NOMBRE_EN_CURSO);
        int afectados = reservaRepository.marcarEnCursoTrasValidarCodigo(
                enCurso,
                r.getIdReserva(),
                EstadoReservaCatalogo.ID_ACEPTADA);
        if (afectados != 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se pudo completar la validación; reintenta o verifica el estado de la reserva");
        }
        Reserva saved = reservaRepository.findById(r.getIdReserva())
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
        PaseoIniciadoDomainEvent evt = new PaseoIniciadoDomainEvent(saved.getIdReserva(), rawJwt);
        if (paseoEfectosDespuesDeCommit) {
            reservaEventPublisher.publicarPaseoIniciado(saved.getIdReserva(), rawJwt);
        } else {
            paseoInicioSideEffectsService.ejecutar(evt);
        }
        EstadoReserva estado = saved.getEstadoReserva();
        return new CodigoReservaValidarResponseDTO(
                saved.getIdReserva(),
                true,
                estado != null ? estado.getIdEstadoReserva() : null,
                estado != null ? estado.getNombreEstado() : null,
                saved.getFechaInicioReal()
        );
    }

    private static int intentosConFallback(Reserva r) {
        Integer n = r.getCodigoIntentosFallidos();
        return n == null ? 0 : n;
    }

    public EstadoEncuentroResponseDTO obtenerEstadoEncuentro(Integer idReserva, String rawJwt) {
        Reserva r = obtenerEntidad(idReserva);
        validarAccesoTimeline(r, rawJwt);
        EstadoReserva estado = r.getEstadoReserva();

        String estadoEncuentro = "PENDIENTE";
        String mensaje = "Pendiente de validación del código de encuentro";
        Long segundos = null;

        if (esEnCurso(estado) || esFinalizada(estado)) {
            estadoEncuentro = "CONFIRMADO";
            mensaje = "Código validado y paseo iniciado";
        } else if (isCodigoBloqueado(r)) {
            estadoEncuentro = "FALLIDO";
            segundos = ChronoUnit.SECONDS.between(LocalDateTime.now(clock), r.getCodigoBloqueadoHasta());
            mensaje = "Intentos agotados. Reintenta en " + Math.max(1, segundos) + " segundos";
        } else if (r.getCodigoIntentosFallidos() != null && r.getCodigoIntentosFallidos() > 0) {
            estadoEncuentro = "FALLIDO";
            mensaje = "Hay intentos fallidos recientes";
        }

        return new EstadoEncuentroResponseDTO(
                r.getIdReserva(),
                estadoEncuentro,
                estado != null ? estado.getIdEstadoReserva() : null,
                estado != null ? estado.getNombreEstado() : null,
                r.getCodigoIntentosFallidos() != null ? r.getCodigoIntentosFallidos() : 0,
                r.getCodigoBloqueadoHasta(),
                segundos != null ? Math.max(1, segundos) : null,
                mensaje
        );
    }

    /**
     * {@code PATCH /status}: decisión del paseador ({@link #aplicarDecisionPaseador}) o del tutor
     * ({@link #aplicarCancelacionTutor}).
     */
    @Transactional
    public ReservaResponseDTO aplicarCambioEstado(Integer idReserva, BookingStatusPatchRequestDTO dto, String rawJwt) {
        if (dto.getTutorDecision() != null) {
            return aplicarCancelacionTutor(idReserva, dto.getTutorDecision(), rawJwt);
        }
        return aplicarDecisionPaseador(idReserva, dto, rawJwt);
    }

    /**
     * El tutor anula la reserva antes de que el paseador la acepte: SOLICITADA, PENDIENTE_PAGO o PAGADA.
     * <p>Paso 5 (PDF): si estaba PAGADA (cobro), programa devolución MP tras commit (CANCELADA con cobro).</p>
     */
    @Transactional
    public ReservaResponseDTO aplicarCancelacionTutor(Integer idReserva, TutorDecision decision, String rawJwt) {
        if (decision != TutorDecision.CANCELAR_SOLICITUD) {
            throw new IllegalArgumentException("Decisión de tutor no soportada");
        }
        Reserva r = obtenerEntidad(idReserva);
        validarTutorJwt(r.getIdTutorUsuario(), rawJwt);
        EstadoReserva actual = r.getEstadoReserva();
        if (!(esSolicitada(actual) || esPendientePago(actual) || esPagada(actual))) {
            throw new IllegalArgumentException(
                    "Solo puede cancelar una solicitud pendiente de aceptación (SOLICITADA, PENDIENTE_PAGO o PAGADA)");
        }
        boolean eraPagada = esPagada(actual);
        EstadoReserva cancelada =
                estadoReservaService.obtenerPorNombreIgnoreCase(EstadoReservaCatalogo.NOMBRE_CANCELADA);
        r.setEstadoReserva(cancelada);
        aplicarMarcaTiempoTransicion(r, cancelada);
        Reserva saved = reservaRepository.save(r);
        agendaIntegracionClient.marcarBloqueDisponibleInterno(saved.getIdAgendaBloque());
        if (eraPagada) {
            programarReembolsoMercadoPagoTrasCommit(saved.getIdReserva(), saved);
        }
        return ReservaDtoMapper.toReservaResponse(saved);
    }

    @Transactional
    public void eliminar(Integer id) {
        if (!reservaRepository.existsById(id)) {
            throw new IllegalArgumentException("Reserva no encontrada");
        }
        reservaRepository.deleteById(id);
    }

    // =========================================================================
    // NUEVO MÉTODO DE VIGILANCIA: Protección de servicios comprometidos
    // =========================================================================
    /**
     * Verifica si existen reservas activas/comprometidas para una lista de bloques.
     * Esto impide que el paseador borre horarios que ya tienen un compromiso real.
     */
    public boolean tieneReservasComprometidasEnBloques(List<Integer> idsAgendaBloque) {
        if (idsAgendaBloque == null || idsAgendaBloque.isEmpty()) {
            return false; // No hay bloques para validar //
        }
        // Reserva que aún retiene el bloque: solicitada/pendiente pago/pagada/aceptada/en curso
        // (no rechazada ni cancelada ni finalizada).
        List<Integer> estadosComprometidos = List.of(
                EstadoReservaCatalogo.ID_SOLICITADA,
                EstadoReservaCatalogo.ID_PENDIENTE_PAGO,
                EstadoReservaCatalogo.ID_PAGADA,
                EstadoReservaCatalogo.ID_ACEPTADA,
                EstadoReservaCatalogo.ID_EN_CURSO);
        
        // Ejecuta la consulta de existencia en el repositorio //
        return reservaRepository.existsByIdAgendaBloqueInAndEstadoReserva_IdEstadoReservaIn(
            idsAgendaBloque,
            estadosComprometidos
        );
    }

    /**
     * Datos mínimos para iniciar checkout en pagos-service (llamada interna con secreto compartido).
     */
    @Transactional(readOnly = true)
    public ReservaParaPagoDto obtenerParaPagoInterno(Integer idReserva) {
        Reserva r = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
        EstadoReserva er = r.getEstadoReserva();
        Integer idEstado = er != null ? er.getIdEstadoReserva() : null;
        if (idEstado != null && idEstado == EstadoReservaCatalogo.ID_PAGADA) {
            throw new IllegalStateException("La reserva ya está pagada");
        }
        Set<Integer> permitidos = Set.of(
                EstadoReservaCatalogo.ID_SOLICITADA,
                EstadoReservaCatalogo.ID_PENDIENTE_PAGO,
                EstadoReservaCatalogo.ID_ACEPTADA);
        if (idEstado == null || !permitidos.contains(idEstado)) {
            String nombre = er != null ? er.getNombreEstado() : "SIN_ESTADO";
            throw new IllegalStateException("Estado no permite iniciar pago: " + nombre);
        }
        return new ReservaParaPagoDto(
                r.getIdReserva().longValue(),
                r.getIdTutorUsuario().longValue(),
                r.getMontoTotal(),
                r.getIdPago());
    }

    /**
     * Listado del tutor: con integración a agenda activa, ordena por inicio programado del bloque
     * (futuras primero ascendente, luego pasadas descendente) y exige JWT del tutor.
     * Sin integración, devuelve el listado sin ordenar por bloque.
     */
    public List<ReservaResponseDTO> listarPorTutor(Integer idTutorUsuario, String rawJwt) {
        List<Reserva> reservas = reservaRepository.findByIdTutorUsuario(idTutorUsuario);
        if (!agendaIntegracionClient.isEnabled()) {
            return reservas.stream()
                    .map(ReservaDtoMapper::toReservaResponse)
                    .toList();
        }
        validarTutorJwt(idTutorUsuario, rawJwt);
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<ReservaConInicio> conInicio = new ArrayList<>(reservas.size());
        for (Reserva r : reservas) {
            AgendaBloqueReservaClientDTO bloque =
                    agendaIntegracionClient.obtenerBloquePorId(r.getIdAgendaBloque(), rawJwt);
            conInicio.add(new ReservaConInicio(r, resolverInicioPaseo(r, bloque), bloque));
        }
        List<ReservaConInicio> futuras = conInicio.stream()
                .filter(x -> !x.inicio().isBefore(ahora))
                .sorted(Comparator.comparing(ReservaConInicio::inicio))
                .toList();
        List<ReservaConInicio> pasadas = conInicio.stream()
                .filter(x -> x.inicio().isBefore(ahora))
                .sorted(Comparator.comparing(ReservaConInicio::inicio).reversed())
                .toList();
        return Stream.concat(futuras.stream(), pasadas.stream())
                .map(x -> ReservaDtoMapper.toReservaResponse(x.reserva(), x.bloque()))
                .toList();
    }

    private static LocalDateTime resolverInicioPaseo(Reserva reserva, AgendaBloqueReservaClientDTO bloque) {
        if (bloque.getHoraInicio() != null) {
            return bloque.getHoraInicio();
        }
        if (bloque.getFecha() != null) {
            return bloque.getFecha().atStartOfDay();
        }
        return reserva.getFechaSolicitud();
    }

    private record ReservaConInicio(Reserva reserva, LocalDateTime inicio, AgendaBloqueReservaClientDTO bloque) {}

    public List<ReservaTutorDetalleResponseDTO> listarDetallePorTutor(Integer idTutorUsuario, String rawJwt) {
        validarTutorJwt(idTutorUsuario, rawJwt);
        return reservaRepository.findByIdTutorUsuario(idTutorUsuario).stream()
                .map(r -> toTutorDetalle(rellenarCodigoEncuentroExpiraSiAplica(r.getIdReserva(), rawJwt), rawJwt))
                .sorted((a, b) -> {
                    LocalDateTime fechaA = a.getHoraInicio() != null ? a.getHoraInicio() : a.getFechaSolicitud();
                    LocalDateTime fechaB = b.getHoraInicio() != null ? b.getHoraInicio() : b.getFechaSolicitud();
                    if (fechaA == null && fechaB == null) {
                        return 0;
                    }
                    if (fechaA == null) {
                        return 1;
                    }
                    if (fechaB == null) {
                        return -1;
                    }
                    return fechaA.compareTo(fechaB);
                })
                .toList();
    }

    /**
     * Reservas visibles en el panel del paseador cuyo bloque de agenda pertenece al paseador
     * (JWT {@code paseadorId}): SOLICITADA, ACEPTADA, EN_CURSO y RECHAZADA.
     */
    public List<ReservaPaseadorSolicitudResponseDTO> listarSolicitudesPendientesPaseador(
            Integer idPaseador, String rawJwt) {
        validarPaseadorJwt(idPaseador, rawJwt);
        if (!agendaIntegracionClient.isEnabled()) {
            return List.of();
        }
        List<AgendaBloqueReservaClientDTO> bloques =
                agendaIntegracionClient.listarBloquesPorUsuario(idPaseador, rawJwt);
        if (bloques.isEmpty()) {
            return List.of();
        }
        Map<Integer, AgendaBloqueReservaClientDTO> bloquePorId = bloques.stream()
                .filter(b -> b.getIdAgenda() != null)
                .collect(Collectors.toMap(AgendaBloqueReservaClientDTO::getIdAgenda, b -> b, (a, b) -> a));

        List<Integer> idsBloque = new ArrayList<>(bloquePorId.keySet());
        List<Integer> estadosVisibles = List.of(
                EstadoReservaCatalogo.ID_SOLICITADA,
                EstadoReservaCatalogo.ID_PENDIENTE_PAGO,
                EstadoReservaCatalogo.ID_PAGADA,
                EstadoReservaCatalogo.ID_ACEPTADA,
                EstadoReservaCatalogo.ID_EN_CURSO,
                EstadoReservaCatalogo.ID_RECHAZADA,
                EstadoReservaCatalogo.ID_EXPIRADA);
        List<Reserva> reservas = reservaRepository.findByIdAgendaBloqueInAndEstadoReserva_IdEstadoReservaIn(
                idsBloque, estadosVisibles);

        List<ReservaPaseadorSolicitudResponseDTO> salida = new ArrayList<>();
        for (Reserva r : reservas) {
            AgendaBloqueReservaClientDTO bloque = bloquePorId.get(r.getIdAgendaBloque());
            TutorReservaClientDTO tutor =
                    tutorIntegracionClient.obtenerTutor(r.getIdTutorUsuario().longValue(), rawJwt);
            salida.add(mapearSolicitudPaseador(r, bloque, tutor));
        }
        salida.sort(Comparator.comparing(
                ReservaPaseadorSolicitudResponseDTO::getFechaSolicitud,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return salida;
    }

    /**
     * Vista interna para enriquecer ítems de billetera en pagos-service.
     * Omite reservas inexistentes, bloques de otros paseadores o fallos puntuales de agenda (por ítem).
     */
    @Transactional(readOnly = true)
    public List<InternoBilleteraReservaDetalleDto> listarDetallesBilleteraInterno(
            Long idUsuarioPaseador, List<Integer> idsReserva) {
        if (idUsuarioPaseador == null || idsReserva == null || idsReserva.isEmpty()) {
            return List.of();
        }
        if (!agendaIntegracionClient.isEnabled()) {
            log.debug("detalles billetera interno: integración agenda deshabilitada");
            return List.of();
        }
        LinkedHashSet<Integer> uniq = new LinkedHashSet<>(idsReserva);
        List<InternoBilleteraReservaDetalleDto> salida = new ArrayList<>();
        int idPaseadorInt = idUsuarioPaseador.intValue();
        for (Integer idReserva : uniq) {
            if (idReserva == null) {
                continue;
            }
            Optional<Reserva> opt = reservaRepository.findById(idReserva);
            if (opt.isEmpty()) {
                continue;
            }
            Reserva r = opt.get();
            AgendaBloqueReservaClientDTO bloque;
            try {
                bloque = agendaIntegracionClient.obtenerBloquePorIdInterno(r.getIdAgendaBloque());
            } catch (RuntimeException ex) {
                log.debug(
                        "detalles billetera interno: sin bloque agenda reserva={} ({})",
                        idReserva,
                        ex.getMessage());
                continue;
            }
            if (bloque == null
                    || bloque.getIdUsuario() == null
                    || bloque.getIdUsuario() != idPaseadorInt) {
                continue;
            }
            salida.add(mapearInternoBilleteraDetalle(r, bloque));
        }
        return salida;
    }

    public List<ReservaResponseDTO> listarPorMascota(Integer idMascota) {
        return reservaRepository.findByIdMascota(idMascota).stream()
                .map(ReservaDtoMapper::toReservaResponse)
                .toList();
    }

    public List<ReservaResponseDTO> listarPorAgenda(Integer idAgendaBloque) {
        return reservaRepository.findByIdAgendaBloque(idAgendaBloque).stream()
                .map(ReservaDtoMapper::toReservaResponse)
                .toList();
    }

    public List<ReservaResponseDTO> listarPorEstado(Integer idEstadoReserva) {
        return reservaRepository.findByEstadoReserva_IdEstadoReserva(idEstadoReserva).stream()
                .map(ReservaDtoMapper::toReservaResponse)
                .toList();
    }

    private void validarPaseadorJwt(Integer idPaseador, String rawJwt) {
        if (rawJwt == null || rawJwt.isBlank()) {
            throw new IllegalArgumentException("Se requiere JWT de paseador");
        }
        if (!jwtService.isTokenValid(rawJwt)) {
            throw new IllegalArgumentException("Token inválido o expirado");
        }
        Long idJwt = jwtService.extractPaseadorId(rawJwt);
        if (idJwt == null) {
            throw new IllegalArgumentException("JWT sin claim paseadorId");
        }
        if (!idJwt.equals(idPaseador.longValue())) {
            throw new IllegalArgumentException("idPaseador no coincide con el token");
        }
    }

    private void validarPaseadorPropietarioReserva(Reserva r, String rawJwt) {
        if (!agendaIntegracionClient.isEnabled()) {
            return;
        }
        if (rawJwt == null || rawJwt.isBlank()) {
            throw new IllegalArgumentException("Se requiere JWT de paseador");
        }
        Long paseadorId = jwtService.extractPaseadorId(rawJwt);
        if (paseadorId == null) {
            throw new IllegalArgumentException("JWT sin claim paseadorId");
        }
        AgendaBloqueReservaClientDTO bloque =
                agendaIntegracionClient.obtenerBloquePorId(r.getIdAgendaBloque(), rawJwt);
        if (bloque.getIdUsuario() == null || bloque.getIdUsuario().longValue() != paseadorId) {
            throw new IllegalArgumentException("No tienes permiso para modificar esta reserva");
        }
    }

    private void validarAccesoTimeline(Reserva r, String rawJwt) {
        if (rawJwt == null || rawJwt.isBlank()) {
            throw new IllegalArgumentException("Se requiere JWT");
        }
        if (!jwtService.isTokenValid(rawJwt)) {
            throw new IllegalArgumentException("Token inválido o expirado");
        }

        Long tutorId = jwtService.extractTutorId(rawJwt);
        if (tutorId != null) {
            if (!tutorId.equals(r.getIdTutorUsuario().longValue())) {
                throw new IllegalArgumentException("No tienes permiso para ver esta reserva");
            }
            return;
        }

        Long paseadorId = jwtService.extractPaseadorId(rawJwt);
        if (paseadorId != null) {
            AgendaBloqueReservaClientDTO bloque =
                    agendaIntegracionClient.obtenerBloquePorId(r.getIdAgendaBloque(), rawJwt);
            if (bloque.getIdUsuario() == null || bloque.getIdUsuario().longValue() != paseadorId) {
                throw new IllegalArgumentException("No tienes permiso para ver esta reserva");
            }
            return;
        }

        throw new IllegalArgumentException("JWT sin claim tutorId/paseadorId");
    }

    private boolean isCodigoBloqueado(Reserva r) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        return r.getCodigoBloqueadoHasta() != null && ahora.isBefore(r.getCodigoBloqueadoHasta());
    }

    private InternoBilleteraReservaDetalleDto mapearInternoBilleteraDetalle(
            Reserva r, AgendaBloqueReservaClientDTO bloque) {
        String fechaAgenda = "";
        String horaIni = "";
        if (bloque != null) {
            if (bloque.getFecha() != null) {
                fechaAgenda = bloque.getFecha().toString();
            }
            horaIni = formatearHoraMinuto(bloque.getHoraInicio());
        }
        String tutorNombre = tutorNombreInternoBilletera(r.getIdTutorUsuario());
        String mascotaNombre = "Mascota #" + r.getIdMascota();
        try {
            MascotaInternoDetalleResponseDTO detalleMascota =
                    mascotaIntegracionClient.obtenerDetalleInterno(r.getIdMascota());
            if (detalleMascota != null && StringUtils.hasText(detalleMascota.getNombre())) {
                mascotaNombre = detalleMascota.getNombre().trim();
            } else {
                MascotaPortadaUrlResponse portada = mascotaIntegracionClient.obtenerPortadaInterno(r.getIdMascota());
                if (portada != null && StringUtils.hasText(portada.getNombre())) {
                    mascotaNombre = portada.getNombre().trim();
                }
            }
        } catch (RuntimeException ignored) {
            // mismo criterio que panel paseador
        }
        EstadoReserva estado = r.getEstadoReserva();
        String nombreEstado = estado != null ? estado.getNombreEstado() : null;
        return new InternoBilleteraReservaDetalleDto(
                r.getIdReserva(), mascotaNombre, tutorNombre, fechaAgenda, horaIni, nombreEstado);
    }

    private String tutorNombreInternoBilletera(Integer idTutorUsuario) {
        if (idTutorUsuario == null) {
            return "Tutor";
        }
        String correo = tutorIntegracionClient.obtenerCorreoInterno(idTutorUsuario.longValue());
        if (!StringUtils.hasText(correo)) {
            return "Tutor #" + idTutorUsuario;
        }
        int at = correo.indexOf('@');
        String local = at > 0 ? correo.substring(0, at).trim() : correo.trim();
        return local.isEmpty() ? "Tutor #" + idTutorUsuario : local;
    }

    private ReservaPaseadorSolicitudResponseDTO mapearSolicitudPaseador(
            Reserva r, AgendaBloqueReservaClientDTO bloque, TutorReservaClientDTO tutor) {
        EstadoReserva estado = r.getEstadoReserva();
        String nombreEstado = estado != null ? estado.getNombreEstado() : null;

        String fechaAgenda = "";
        String horaIni = "";
        String horaFin = "";
        if (bloque != null) {
            LocalDate f = bloque.getFecha();
            if (f != null) {
                fechaAgenda = f.toString();
            }
            horaIni = formatearHoraMinuto(bloque.getHoraInicio());
            horaFin = formatearHoraMinuto(bloque.getHoraFinal());
        }

        String tutorNombre;
        String tutorTel = "";
        String tutorCorreo = "";
        String tutorFoto = "";
        String tutorNotas = "";
        String comuna = "";
        String direccionRef = "";

        if (tutor != null) {
            tutorNombre = nombreCompletoTutor(tutor);
            if (tutor.getTelefono() != null) {
                tutorTel = tutor.getTelefono();
            }
            if (tutor.getCorreo() != null) {
                tutorCorreo = tutor.getCorreo();
            }
            if (tutor.getFotoPerfil() != null) {
                tutorFoto = tutor.getFotoPerfil();
            }
            if (tutor.getBiografia() != null) {
                tutorNotas = tutor.getBiografia();
            }
            TutorReservaClientDTO.DireccionTutorClientDTO d = tutor.getDireccion();
            if (d != null) {
                if (d.getComuna() != null) {
                    comuna = d.getComuna();
                }
                direccionRef = lineaDireccionTutor(d);
            }
        } else {
            tutorNombre = "Tutor #" + r.getIdTutorUsuario();
        }

        if (direccionRef.isBlank() && bloque != null) {
            direccionRef = "Bloque agenda #" + r.getIdAgendaBloque();
        }
        if (comuna.isBlank()) {
            comuna = "—";
        }

        String mascotaNombre = "Mascota #" + r.getIdMascota();
        String mascotaFotoUrl = null;
        String mascotaRaza = "";
        String mascotaTamano = "";
        String mascotaEdad = "";
        String mascotaPeso = "";
        String mascotaSexo = "";
        String mascotaCaracter = "";
        String mascotaCuidados = "";
        try {
            MascotaInternoDetalleResponseDTO detalleMascota = mascotaIntegracionClient.obtenerDetalleInterno(r.getIdMascota());
            if (detalleMascota != null) {
                mascotaFotoUrl = detalleMascota.getFotoPerfil();
                if (detalleMascota.getNombre() != null && !detalleMascota.getNombre().isBlank()) {
                    mascotaNombre = detalleMascota.getNombre();
                }
                mascotaRaza = detalleMascota.getRaza() != null ? detalleMascota.getRaza() : "";
                mascotaTamano = detalleMascota.getTamano() != null ? detalleMascota.getTamano() : "";
                mascotaEdad = detalleMascota.getEdad() != null ? detalleMascota.getEdad() : "";
                mascotaPeso = detalleMascota.getPeso() != null ? detalleMascota.getPeso() : "";
                mascotaSexo = detalleMascota.getSexo() != null ? detalleMascota.getSexo() : "";
                mascotaCaracter = detalleMascota.getCaracter() != null ? detalleMascota.getCaracter() : "";
                mascotaCuidados = detalleMascota.getCuidados() != null ? detalleMascota.getCuidados() : "";
            } else {
                MascotaPortadaUrlResponse portada = mascotaIntegracionClient.obtenerPortadaInterno(r.getIdMascota());
                if (portada != null) {
                    mascotaFotoUrl = portada.getUrl();
                    if (portada.getNombre() != null && !portada.getNombre().isBlank()) {
                        mascotaNombre = portada.getNombre();
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // Si mascotas no está o falla el interno, el panel del paseador sigue mostrando nombre genérico.
        }

        return new ReservaPaseadorSolicitudResponseDTO(
                r.getIdReserva(),
                r.getIdTutorUsuario(),
                r.getIdMascota(),
                r.getIdAgendaBloque(),
                r.getMontoTotal(),
                r.getFechaSolicitud(),
                nombreEstado,
                fechaAgenda,
                horaIni,
                horaFin,
                comuna,
                direccionRef,
                tutorNombre,
                tutorTel,
                tutorCorreo,
                tutorFoto,
                tutorNotas,
                mascotaFotoUrl,
                mascotaNombre,
                r.getCodigoEncuentro(),
                mascotaRaza,
                mascotaTamano,
                mascotaEdad,
                mascotaPeso,
                mascotaSexo,
                mascotaCaracter,
                mascotaCuidados);
    }

    private static String formatearHoraMinuto(LocalDateTime t) {
        if (t == null) {
            return "";
        }
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    private static String nombreCompletoTutor(TutorReservaClientDTO t) {
        StringBuilder sb = new StringBuilder();
        if (t.getPrimerNombre() != null && !t.getPrimerNombre().isBlank()) {
            sb.append(t.getPrimerNombre().trim());
        }
        if (t.getSegundoNombre() != null && !t.getSegundoNombre().isBlank()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(t.getSegundoNombre().trim());
        }
        if (t.getApellidoPaterno() != null && !t.getApellidoPaterno().isBlank()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(t.getApellidoPaterno().trim());
        }
        if (t.getApellidoMaterno() != null && !t.getApellidoMaterno().isBlank()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(t.getApellidoMaterno().trim());
        }
        String s = sb.toString().trim();
        return s.isEmpty() ? "Tutor" : s;
    }

    private static String lineaDireccionTutor(TutorReservaClientDTO.DireccionTutorClientDTO d) {
        List<String> partes = new ArrayList<>();
        if (d.getCalle() != null && !d.getCalle().isBlank()) {
            partes.add(d.getCalle().trim());
        }
        if (d.getNumeracion() != null) {
            partes.add(String.valueOf(d.getNumeracion()));
        }
        if (d.getCasaDepartamento() != null && !d.getCasaDepartamento().isBlank()) {
            partes.add(d.getCasaDepartamento().trim());
        }
        if (d.getCiudad() != null && !d.getCiudad().isBlank()) {
            partes.add(d.getCiudad().trim());
        }
        if (d.getComuna() != null && !d.getComuna().isBlank()) {
            partes.add(d.getComuna().trim());
        }
        return String.join(", ", partes);
    }

    private void validarTutorJwt(Integer idTutorUsuario, String rawJwt) {
        if (!agendaIntegracionClient.isEnabled()) {
            return;
        }
        if (rawJwt == null || rawJwt.isBlank()) {
            throw new IllegalArgumentException("Se requiere JWT de tutor para reservar bloque");
        }
        if (!jwtService.isTokenValid(rawJwt)) {
            throw new IllegalArgumentException("Token inválido o expirado");
        }
        Long tutorId = jwtService.extractTutorId(rawJwt);
        if (tutorId == null) {
            throw new IllegalArgumentException("JWT sin claim tutorId");
        }
        if (!tutorId.equals(idTutorUsuario.longValue())) {
            throw new IllegalArgumentException("idTutorUsuario no coincide con el token");
        }
    }

    private EstadoReserva resolverEstadoDestino(BookingStatusPatchRequestDTO dto) {
        if (dto.getIdEstadoReserva() != null) {
            return estadoReservaService.obtenerEntidad(dto.getIdEstadoReserva());
        }
        if (dto.getDecision() != null) {
            return buscarEstadoPorDecision(dto.getDecision());
        }
        throw new IllegalArgumentException("Debe enviar decision o idEstadoReserva");
    }

    private EstadoReserva buscarEstadoPorDecision(PaseadorDecision decision) {
        String nombre = switch (decision) {
            case ACEPTAR -> EstadoReservaCatalogo.NOMBRE_ACEPTADA;
            case RECHAZAR -> EstadoReservaCatalogo.NOMBRE_RECHAZADA;
            case INICIAR_PASEO -> EstadoReservaCatalogo.NOMBRE_EN_CURSO;
            case FINALIZAR_PASEO -> EstadoReservaCatalogo.NOMBRE_FINALIZADA;
        };
        return estadoReservaService.obtenerPorNombreIgnoreCase(nombre);
    }

    private boolean esEstadoRechazada(EstadoReserva estado) {
        if (estado == null) {
            return false;
        }
        Integer id = estado.getIdEstadoReserva();
        if (id != null && id.equals(EstadoReservaCatalogo.ID_RECHAZADA)) {
            return true;
        }
        return estado.getNombreEstado() != null
                && estado.getNombreEstado().equalsIgnoreCase(EstadoReservaCatalogo.NOMBRE_RECHAZADA);
    }

    private TutorReservaClientDTO obtenerTutorSeguro(Integer idTutor, String rawJwt) {
        try {
            return tutorIntegracionClient.obtenerTutor(idTutor.longValue(), rawJwt);
        } catch (Exception e) {
            log.debug("No se pudo obtener datos del tutor {}: {}", idTutor, e.getMessage());
            return null;
        }
    }

    private ReservaTutorDetalleResponseDTO toTutorDetalle(Reserva r, String rawJwt) {
        AgendaBloqueResumenDTO bloque = obtenerBloqueSeguro(r.getIdAgendaBloque(), rawJwt);
        MascotaResumenDTO mascota = obtenerMascotaSeguro(r.getIdMascota(), rawJwt);
        PaseadorResumenDTO paseador = bloque == null ? null : obtenerPaseadorSeguro(bloque.getIdUsuario());
        TutorReservaClientDTO tutor = obtenerTutorSeguro(r.getIdTutorUsuario(), rawJwt);
        return ReservaDtoMapper.toTutorDetalleResponse(
                r,
                bloque,
                mascota,
                paseador,
                tutor,
                reservaPagoService.permiteReintentarPago(r));
    }

    private AgendaBloqueResumenDTO obtenerBloqueSeguro(Integer idAgendaBloque, String rawJwt) {
        try {
            return agendaIntegracionClient.obtenerBloque(idAgendaBloque, rawJwt);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private MascotaResumenDTO obtenerMascotaSeguro(Integer idMascota, String rawJwt) {
        try {
            return mascotaIntegracionClient.obtenerResumen(idMascota, rawJwt);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private PaseadorResumenDTO obtenerPaseadorSeguro(Integer idPaseador) {
        try {
            return paseadorIntegracionClient.obtenerResumen(idPaseador);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void validarTransicionPaseador(EstadoReserva actual, EstadoReserva nuevo) {
        if (actual.getIdEstadoReserva() != null
                && nuevo.getIdEstadoReserva() != null
                && actual.getIdEstadoReserva().equals(nuevo.getIdEstadoReserva())) {
            return;
        }
        if (esSolicitada(actual) || esPendientePago(actual)) {
            if (esAceptada(nuevo) || esRechazada(nuevo)) {
                return;
            }
            throw new IllegalArgumentException("Desde SOLICITADA o PENDIENTE_PAGO solo se permite ACEPTADA o RECHAZADA");
        }
        if (esPagada(actual)) {
            if (esAceptada(nuevo) || esRechazada(nuevo)) {
                return;
            }
            throw new IllegalArgumentException("Desde PAGADA solo se permite ACEPTADA o RECHAZADA");
        }
        if (esAceptada(actual)) {
            if (esEnCurso(nuevo)) {
                return;
            }
            throw new IllegalArgumentException("Desde ACEPTADA solo se permite EN_CURSO");
        }
        if (esEnCurso(actual)) {
            if (esFinalizada(nuevo)) {
                return;
            }
            throw new IllegalArgumentException("Desde EN_CURSO solo se permite FINALIZADA");
        }
        throw new IllegalArgumentException("Transición de estado no permitida para el paseador");
    }

    private void aplicarMarcaTiempoTransicion(Reserva r, EstadoReserva nuevo) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        if (esAceptada(nuevo) && r.getFechaAceptacion() == null) {
            r.setFechaAceptacion(ahora);
        }
        if (esEnCurso(nuevo) && r.getFechaInicioReal() == null) {
            r.setFechaInicioReal(ahora);
        }
        if (esFinalizada(nuevo) && r.getFechaFin() == null) {
            r.setFechaFin(ahora);
        }
        if ((esRechazada(nuevo) || esCancelada(nuevo) || esExpirada(nuevo)) && r.getFechaFin() == null) {
            r.setFechaFin(ahora);
        }
    }

    private void aplicarDatosDecisionPaseador(
            Reserva r, BookingStatusPatchRequestDTO dto, EstadoReserva nuevo, String rawJwt) {
        if (esAceptada(nuevo)) {
            AgendaBloqueReservaClientDTO bloque = null;
            if (agendaIntegracionClient.isEnabled() && rawJwt != null && !rawJwt.isBlank()) {
                bloque = agendaIntegracionClient.obtenerBloquePorId(r.getIdAgendaBloque(), rawJwt);
            }
            LocalDateTime ahora = LocalDateTime.now(clock);
            LocalDateTime inicioProgramado = resolverInicioProgramadoPaseo(bloque, r, ahora);
            r.setCodigoEncuentro(generarCodigoEncuentroUnicoExcluyendo(r.getIdReserva()));
            r.setCodigoEncuentroExpiraEn(calcularExpiracionCodigoDesdeInicio(inicioProgramado, ahora));
            r.setCodigoIntentosFallidos(0);
            r.setCodigoBloqueadoHasta(null);
            r.setMotivoRechazo(null);
            r.setDetalleRechazo(null);
            return;
        }
        if (esRechazada(nuevo)) {
            String motivo = dto.getMotivoRechazo();
            String detalle = dto.getDetalleRechazo();
            r.setMotivoRechazo(motivo != null && !motivo.isBlank() ? motivo.trim() : null);
            r.setDetalleRechazo(detalle != null && !detalle.isBlank() ? detalle.trim() : null);
        }
    }

    /**
     * Inicio programado del paseo: hora del bloque de agenda, o fecha a 00:00, o ahora.
     */
    private LocalDateTime resolverInicioProgramadoPaseo(
            AgendaBloqueReservaClientDTO bloque, Reserva r, LocalDateTime ahora) {
        if (bloque != null && bloque.getHoraInicio() != null) {
            return bloque.getHoraInicio();
        }
        if (bloque != null && bloque.getFecha() != null) {
            return bloque.getFecha().atStartOfDay();
        }
        if (r.getFechaSolicitud() != null) {
            return r.getFechaSolicitud();
        }
        return ahora;
    }

    private LocalDateTime calcularExpiracionCodigoDesdeInicio(LocalDateTime inicioProgramado, LocalDateTime ahora) {
        LocalDateTime finValidez = inicioProgramado.plusMinutes(Math.max(1, codigoExpiraMinutos));
        if (!finValidez.isAfter(ahora)) {
            return ahora.plusMinutes(Math.max(1, codigoExpiraMinutos));
        }
        return finValidez;
    }

    private int generarCodigoEncuentroUnicoExcluyendo(Integer excludeReservaId) {
        List<Integer> estadosActivoCodigo = List.of(
                EstadoReservaCatalogo.ID_SOLICITADA,
                EstadoReservaCatalogo.ID_ACEPTADA,
                EstadoReservaCatalogo.ID_EN_CURSO);
        for (int i = 0; i < 64; i++) {
            int candidato = ThreadLocalRandom.current().nextInt(1000, 10000);
            if (!reservaRepository.existsOtraActivaConCodigo(candidato, excludeReservaId, estadosActivoCodigo)) {
                return candidato;
            }
        }
        throw new IllegalStateException("No se pudo generar un código de encuentro único");
    }

    /**
     * Job: solo rellena {@code codigoEncuentroExpiraEn} si faltaba; sin JWT (fallback de inicio vía reserva).
     */
    @Transactional
    public void rellenarCodigoEncuentroExpiraJobItem(Integer idReserva) {
        rellenarCodigoEncuentroExpiraSiAplica(idReserva, null);
    }

    /**
     * Tras vencer el PIN (sin inicio real): regenera un nuevo PIN y reinicia su expiración.
     */
    @Transactional
    public void regenerarCodigoEncuentroPorExpiracionJobItem(Integer idReserva) {
        Reserva r = obtenerEntidad(idReserva);
        if (!esAceptada(r.getEstadoReserva()) || r.getFechaInicioReal() != null) {
            return;
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        if (r.getCodigoEncuentroExpiraEn() == null || !ahora.isAfter(r.getCodigoEncuentroExpiraEn())) {
            return;
        }
        r.setCodigoEncuentro(generarCodigoEncuentroUnicoExcluyendo(r.getIdReserva()));
        r.setCodigoEncuentroExpiraEn(ahora.plusMinutes(Math.max(1, codigoExpiraMinutos)));
        r.setCodigoIntentosFallidos(0);
        r.setCodigoBloqueadoHasta(null);
        reservaRepository.save(r);
    }

    /**
     * Si aún aceptada y faltaba solo la expiración del PIN, la calcula (listado tutor / job).
     */
    private Reserva rellenarCodigoEncuentroExpiraSiAplica(Integer idReserva, String rawJwt) {
        Reserva r = obtenerEntidad(idReserva);
        if (!esAceptada(r.getEstadoReserva())) {
            return r;
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        if (r.getCodigoEncuentro() != null && r.getCodigoEncuentroExpiraEn() == null) {
            AgendaBloqueReservaClientDTO b = null;
            if (agendaIntegracionClient.isEnabled() && rawJwt != null && !rawJwt.isBlank()) {
                try {
                    b = agendaIntegracionClient.obtenerBloquePorId(r.getIdAgendaBloque(), rawJwt);
                } catch (RuntimeException ignored) {
                    // seguir con fallback
                }
            }
            LocalDateTime inicio = resolverInicioProgramadoPaseo(b, r, ahora);
            r.setCodigoEncuentroExpiraEn(calcularExpiracionCodigoDesdeInicio(inicio, ahora));
            return reservaRepository.save(r);
        }
        return r;
    }

    private static boolean coincideIdONombre(EstadoReserva e, int idEsperado, String nombreEsperado) {
        if (e == null) {
            return false;
        }
        Integer id = e.getIdEstadoReserva();
        if (id != null && id.equals(idEsperado)) {
            return true;
        }
        String n = e.getNombreEstado();
        return n != null && n.trim().equalsIgnoreCase(nombreEsperado);
    }

    private static boolean esSolicitada(EstadoReserva e) {
        return coincideIdONombre(e, EstadoReservaCatalogo.ID_SOLICITADA, EstadoReservaCatalogo.NOMBRE_SOLICITADA);
    }

    private static boolean esAceptada(EstadoReserva e) {
        return coincideIdONombre(e, EstadoReservaCatalogo.ID_ACEPTADA, EstadoReservaCatalogo.NOMBRE_ACEPTADA);
    }

    private static boolean esRechazada(EstadoReserva e) {
        return coincideIdONombre(e, EstadoReservaCatalogo.ID_RECHAZADA, EstadoReservaCatalogo.NOMBRE_RECHAZADA);
    }

    private static boolean esEnCurso(EstadoReserva e) {
        return coincideIdONombre(e, EstadoReservaCatalogo.ID_EN_CURSO, EstadoReservaCatalogo.NOMBRE_EN_CURSO);
    }

    private static boolean esFinalizada(EstadoReserva e) {
        return coincideIdONombre(e, EstadoReservaCatalogo.ID_FINALIZADA, EstadoReservaCatalogo.NOMBRE_FINALIZADA);
    }

    private static boolean esCancelada(EstadoReserva e) {
        return coincideIdONombre(e, EstadoReservaCatalogo.ID_CANCELADA, EstadoReservaCatalogo.NOMBRE_CANCELADA);
    }

    private static boolean esPendientePago(EstadoReserva e) {
        return coincideIdONombre(e, EstadoReservaCatalogo.ID_PENDIENTE_PAGO, EstadoReservaCatalogo.NOMBRE_PENDIENTE_PAGO);
    }

    private static boolean esPagada(EstadoReserva e) {
        return coincideIdONombre(e, EstadoReservaCatalogo.ID_PAGADA, EstadoReservaCatalogo.NOMBRE_PAGADA);
    }

    private static boolean esExpirada(EstadoReserva e) {
        return coincideIdONombre(e, EstadoReservaCatalogo.ID_EXPIRADA, EstadoReservaCatalogo.NOMBRE_EXPIRADA);
    }

    private Reserva obtenerEntidad(Integer id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
    }
}
