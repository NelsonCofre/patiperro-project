package com.patiperro.pagos.service;

import com.patiperro.pagos.dto.billetera.BilleteraBucketResponse;
import com.patiperro.pagos.dto.billetera.BilleteraReservaItemResponse;
import com.patiperro.pagos.dto.billetera.BilleteraResumenPaseadorResponse;
import com.patiperro.pagos.dto.billetera.LiberacionBatchOutcome;
import com.patiperro.pagos.dto.billetera.LiberacionBatchOutcome.LiberacionLineaPaseador;
import com.patiperro.pagos.reserva.ReservaConsultaClient;
import com.patiperro.pagos.reserva.dto.ReservaBilleteraDetalleDto;
import com.patiperro.pagos.model.Billetera;
import com.patiperro.pagos.model.BilleteraReservaFase;
import com.patiperro.pagos.model.BilleteraReservaTracking;
import com.patiperro.pagos.model.EstadoPago;
import com.patiperro.pagos.model.Transaccion;
import com.patiperro.pagos.support.BilleteraLiberacionCalendario;
import com.patiperro.pagos.repository.BilleteraRepository;
import com.patiperro.pagos.repository.BilleteraReservaTrackingRepository;
import com.patiperro.pagos.repository.TransaccionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class BilleteraService {

    private static final Logger log = LoggerFactory.getLogger(BilleteraService.class);

    private static final int SCALE = 2;

    /**
     * Texto de ayuda para el bucket de verificación: alinea con regla N / N+1 / N+2 y el campo por ítem en la respuesta JSON.
     */
    private static final String BUCKET_VERIFICACION_DESCRIPCION =
            "Montos de paseos ya finalizados en periodo de retención (días N y N+1). "
                    + "Desde el inicio del día calendario N+2 (según la zona de la billetera), si no hay disputa activa, "
                    + "el sistema puede pasarlos a saldo disponible en la corrida programada. "
                    + "Cada reserva incluye la fecha y hora estimadas en que podrían quedar disponibles para retiro.";

    private final BilleteraRepository billeteraRepository;
    private final BilleteraReservaTrackingRepository trackingRepository;
    private final TransaccionRepository transaccionRepository;
    private final ReservaConsultaClient reservaConsultaClient;
    private final BilleteraLiberacionTransaccionalService billeteraLiberacionTransaccionalService;

    @Value("${patiperro.pagos.billetera.zona:America/Santiago}")
    private String zonaId;

    /** Tope de filas del historial de liberaciones en GET billetera (acotado por seguridad y rendimiento). */
    @Value("${patiperro.pagos.billetera.historial-liberaciones-max:100}")
    private int historialLiberacionesMax;

    private Pageable pageableHistorialLiberaciones() {
        int cap = Math.max(1, Math.min(historialLiberacionesMax, 500));
        return PageRequest.of(0, cap);
    }

    /**
     * Idempotente por {@code id_reserva}: si ya existe tracking, no altera saldos.
     */
    @Transactional
    public void acreditarRetenido(Integer idReserva, Long idUsuarioPaseador, Long idTransaccionPagos) {
        if (idReserva == null || idUsuarioPaseador == null || idTransaccionPagos == null) {
            log.warn("Billetera acreditar omitido: argumentos null (idReserva={}, idUsuario={}, idTx={})",
                    idReserva, idUsuarioPaseador, idTransaccionPagos);
            return;
        }
        if (trackingRepository.findByIdReserva(idReserva).isPresent()) {
            log.debug("Billetera acreditar idempotente (idReserva={})", idReserva);
            return;
        }
        Transaccion tx = transaccionRepository.findById(idTransaccionPagos).orElse(null);
        if (tx == null || tx.getIdReserva() == null || !Objects.equals(tx.getIdReserva(), Long.valueOf(idReserva))) {
            log.warn("Billetera acreditar: transacción {} no coincide con reserva {}", idTransaccionPagos, idReserva);
            return;
        }
        if (tx.getEstadoPago() != EstadoPago.APROBADO) {
            log.warn("Billetera acreditar: transacción {} no está APROBADO", idTransaccionPagos);
            return;
        }
        BigDecimal neto = nz(tx.getMontoNeto());
        BigDecimal bruto = nz(tx.getMontoBruto());
        BigDecimal comision = nz(tx.getComisionApp());
        if (neto.signum() <= 0) {
            log.warn("Billetera acreditar: monto neto inválido para reserva {}", idReserva);
            return;
        }

        Billetera b = obtenerOCrearBilletera(idUsuarioPaseador);
        b.setSaldoRetenido(nz(b.getSaldoRetenido()).add(neto).setScale(SCALE, RoundingMode.HALF_UP));
        billeteraRepository.save(b);

        trackingRepository.save(BilleteraReservaTracking.builder()
                .idReserva(idReserva)
                .idUsuarioPaseador(idUsuarioPaseador)
                .idTransaccionPagos(idTransaccionPagos)
                .montoBruto(bruto)
                .comisionApp(comision)
                .montoNeto(neto)
                .fase(BilleteraReservaFase.EN_RETENIDO)
                .build());
        log.info("Billetera: acreditado retenido reserva={} usuario={} neto={}", idReserva, idUsuarioPaseador, neto);
    }

    @Transactional
    public void pasarAVerificacion(Integer idReserva, Long idUsuarioPaseador, LocalDateTime fechaFinServicio) {
        if (idReserva == null || idUsuarioPaseador == null) {
            return;
        }
        BilleteraReservaTracking t = trackingRepository.findByIdReserva(idReserva).orElse(null);
        if (t == null) {
            log.debug("Billetera pasar verificación: sin tracking (idReserva={})", idReserva);
            return;
        }
        if (!idUsuarioPaseador.equals(t.getIdUsuarioPaseador())) {
            log.warn("Billetera pasar verificación: id usuario no coincide (idReserva={})", idReserva);
            return;
        }
        if (t.getFase() != BilleteraReservaFase.EN_RETENIDO) {
            log.debug("Billetera pasar verificación idempotente (idReserva={}, fase={})", idReserva, t.getFase());
            return;
        }
        LocalDateTime fin = fechaFinServicio != null ? fechaFinServicio : LocalDateTime.now();
        BigDecimal neto = nz(t.getMontoNeto());

        Billetera b = obtenerOCrearBilletera(idUsuarioPaseador);
        BigDecimal ret = nz(b.getSaldoRetenido());
        BigDecimal ver = nz(b.getSaldoVerificacion());
        ret = ret.subtract(neto).setScale(SCALE, RoundingMode.HALF_UP);
        ver = ver.add(neto).setScale(SCALE, RoundingMode.HALF_UP);
        if (ret.signum() < 0) {
            log.warn("Billetera pasar verificación: saldo_retenido quedaría negativo (idReserva={}); se ajusta a cero",
                    idReserva);
            ret = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        b.setSaldoRetenido(ret);
        b.setSaldoVerificacion(ver);
        billeteraRepository.save(b);

        t.setFase(BilleteraReservaFase.EN_VERIFICACION);
        t.setFechaFinServicio(fin);
        trackingRepository.save(t);
        log.info("Billetera: paso a verificación reserva={} neto={}", idReserva, neto);
    }

    /**
     * Reversa los montos de una reserva ante reembolso al tutor (rechazo/expiración/cancelación con cobro).
     * Idempotente si ya no hay tracking.
     * <ul>
     *   <li>{@link BilleteraReservaFase#EN_RETENIDO}: descuenta {@code saldo_retenido}.</li>
     *   <li>{@link BilleteraReservaFase#EN_VERIFICACION} sin liberar: descuenta {@code saldo_verificacion}.</li>
     *   <li>{@link BilleteraReservaFase#EN_VERIFICACION} ya liberado a disponible: descuenta {@code saldo_actual}
     *       (el tutor fue reembolsado por MP).</li>
     * </ul>
     */
    @Transactional
    public void revertirRetenido(Integer idReserva) {
        if (idReserva == null) {
            return;
        }
        BilleteraReservaTracking t = trackingRepository.findByIdReserva(idReserva).orElse(null);
        if (t == null) {
            log.debug("Billetera revertir: sin tracking (idReserva={})", idReserva);
            return;
        }
        BigDecimal neto = nz(t.getMontoNeto());
        Long usuario = t.getIdUsuarioPaseador();
        Billetera b = billeteraRepository.findByIdUsuario(usuario).orElse(null);

        if (t.getFase() == BilleteraReservaFase.EN_RETENIDO) {
            if (b != null) {
                BigDecimal ret = nz(b.getSaldoRetenido()).subtract(neto).setScale(SCALE, RoundingMode.HALF_UP);
                if (ret.signum() < 0) {
                    log.warn("Billetera revertir: saldo_retenido negativo tras revertir; se fija a cero (idReserva={})",
                            idReserva);
                    ret = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
                }
                b.setSaldoRetenido(ret);
                billeteraRepository.save(b);
            }
            trackingRepository.delete(t);
            log.info("Billetera: revertido retenido reserva={} neto={}", idReserva, neto);
            return;
        }

        if (t.getFase() == BilleteraReservaFase.EN_VERIFICACION) {
            if (b == null) {
                log.warn(
                        "Billetera revertir: tracking EN_VERIFICACION sin fila billetera usuario={} (idReserva={}); "
                                + "se elimina tracking",
                        usuario,
                        idReserva);
                trackingRepository.delete(t);
                return;
            }
            if (t.getLiberadoEn() == null) {
                BigDecimal ver = nz(b.getSaldoVerificacion()).subtract(neto).setScale(SCALE, RoundingMode.HALF_UP);
                if (ver.signum() < 0) {
                    log.warn(
                            "Billetera revertir: saldo_verificacion negativo tras reembolso; se fija a cero (idReserva={})",
                            idReserva);
                    ver = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
                }
                b.setSaldoVerificacion(ver);
            } else {
                BigDecimal disp = nz(b.getSaldoActual()).subtract(neto).setScale(SCALE, RoundingMode.HALF_UP);
                if (disp.signum() < 0) {
                    log.warn(
                            "Billetera revertir: saldo_actual insuficiente tras reembolso MP; se fija a cero "
                                    + "(idReserva={}, neto={})",
                            idReserva,
                            neto);
                    disp = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
                }
                b.setSaldoActual(disp);
            }
            billeteraRepository.save(b);
            trackingRepository.delete(t);
            log.info(
                    "Billetera: revertido por reembolso (verificación, liberado={}) reserva={} neto={}",
                    t.getLiberadoEn() != null,
                    idReserva,
                    neto);
            return;
        }

        log.warn("Billetera revertir: fase {} no soportada (idReserva={})", t.getFase(), idReserva);
    }

    /**
     * Programación masiva: una transacción independiente por candidato vía
     * {@link BilleteraLiberacionTransaccionalService} (idempotente si {@code liberado_en} ya existe).
     */
    public LiberacionBatchOutcome ejecutarLiberacionesPendientes() {
        ZoneId zone = ZoneId.of(zonaId);
        LocalDate hoy = LocalDate.now(zone);
        List<BilleteraReservaTracking> candidatos =
                trackingRepository.findByFaseAndLiberadoEnIsNull(BilleteraReservaFase.EN_VERIFICACION);
        LinkedHashMap<Long, BigDecimal> sumas = new LinkedHashMap<>();
        LinkedHashMap<Long, Integer> conteos = new LinkedHashMap<>();
        int liberados = 0;
        for (BilleteraReservaTracking t : candidatos) {
            if (!BilleteraLiberacionCalendario.diaActualPermiteLiberacion(hoy, t.getFechaFinServicio(), zone)) {
                continue;
            }
            if (t.getIdTracking() != null
                    && billeteraLiberacionTransaccionalService.liberarSiPendiente(t.getIdTracking(), zone)) {
                liberados++;
                Long uid = t.getIdUsuarioPaseador();
                BigDecimal neto = nz(t.getMontoNeto());
                sumas.merge(uid, neto, (a, b) -> a.add(b).setScale(SCALE, RoundingMode.HALF_UP));
                conteos.merge(uid, 1, Integer::sum);
            }
        }
        if (liberados == 0) {
            return LiberacionBatchOutcome.vacio();
        }
        List<LiberacionLineaPaseador> lineas = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> e : sumas.entrySet()) {
            Long uid = e.getKey();
            lineas.add(new LiberacionLineaPaseador(
                    uid, e.getValue(), conteos.getOrDefault(uid, 0)));
        }
        return new LiberacionBatchOutcome(liberados, List.copyOf(lineas));
    }

    @Transactional(readOnly = true)
    public BilleteraResumenPaseadorResponse resumenParaPaseador(Long idUsuarioPaseador) {
        ZoneId zone = ZoneId.of(zonaId);
        Billetera b = billeteraRepository.findByIdUsuario(idUsuarioPaseador).orElse(null);
        BigDecimal retAgg = b != null ? nz(b.getSaldoRetenido()) : BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal verAgg = b != null ? nz(b.getSaldoVerificacion()) : BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal dispAgg = b != null ? nz(b.getSaldoActual()) : BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);

        List<BilleteraReservaTracking> todos = new ArrayList<>();
        todos.addAll(trackingRepository.findByIdUsuarioPaseadorAndFase(idUsuarioPaseador, BilleteraReservaFase.EN_RETENIDO));
        todos.addAll(trackingRepository.findByIdUsuarioPaseadorAndFase(idUsuarioPaseador, BilleteraReservaFase.EN_VERIFICACION));

        List<BilleteraReservaItemResponse> itemsRet = new ArrayList<>();
        List<BilleteraReservaItemResponse> itemsVer = new ArrayList<>();
        for (BilleteraReservaTracking t : todos) {
            if (t.getLiberadoEn() != null) {
                continue;
            }
            Instant disponibleEn = t.getFase() == BilleteraReservaFase.EN_VERIFICACION
                    ? BilleteraLiberacionCalendario.inicioDiaDisponibleParaRetiro(t.getFechaFinServicio(), zone)
                    : null;
            BilleteraReservaItemResponse item = new BilleteraReservaItemResponse(
                    t.getIdReserva(),
                    t.getMontoBruto(),
                    t.getComisionApp(),
                    t.getMontoNeto(),
                    etiquetaFase(t.getFase()),
                    null,
                    null,
                    null,
                    null,
                    null,
                    t.getIdTransaccionPagos(),
                    disponibleEn);
            if (t.getFase() == BilleteraReservaFase.EN_RETENIDO) {
                itemsRet.add(item);
            } else {
                itemsVer.add(item);
            }
        }

        List<Integer> idsDetalle = Stream.concat(itemsRet.stream(), itemsVer.stream())
                .map(BilleteraReservaItemResponse::idReserva)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Integer, ReservaBilleteraDetalleDto> detallePorReserva =
                reservaConsultaClient.obtenerDetallesBilleteraPaseador(idUsuarioPaseador, idsDetalle);
        itemsRet = aplicarYOrdenarDetalles(itemsRet, detallePorReserva);
        itemsVer = aplicarYOrdenarDetalles(itemsVer, detallePorReserva);

        List<BilleteraReservaTracking> liberados =
                trackingRepository.findByIdUsuarioPaseadorAndLiberadoEnIsNotNullOrderByLiberadoEnDesc(
                        idUsuarioPaseador, pageableHistorialLiberaciones());
        List<BilleteraReservaItemResponse> itemsLibHistorial = new ArrayList<>();
        for (BilleteraReservaTracking t : liberados) {
            itemsLibHistorial.add(
                    new BilleteraReservaItemResponse(
                            t.getIdReserva(),
                            t.getMontoBruto(),
                            t.getComisionApp(),
                            t.getMontoNeto(),
                            "LIBERADO_A_DISPONIBLE",
                            null,
                            null,
                            null,
                            null,
                            null,
                            t.getIdTransaccionPagos(),
                            null));
        }
        List<Integer> idsHistorial =
                itemsLibHistorial.stream().map(BilleteraReservaItemResponse::idReserva).filter(Objects::nonNull).distinct().toList();
        Map<Integer, ReservaBilleteraDetalleDto> detalleHistorial =
                reservaConsultaClient.obtenerDetallesBilleteraPaseador(idUsuarioPaseador, idsHistorial);
        itemsLibHistorial = itemsLibHistorial.stream()
                .map(i -> enriquecerItem(i, detalleHistorial))
                .toList();

        LocalDateTime updatedAt = LocalDateTime.now(zone);

        return new BilleteraResumenPaseadorResponse(
                new BilleteraBucketResponse(
                        "retenido",
                        "Saldo Retenido",
                        "Este saldo corresponde a servicios pagados que aún no han finalizado. Una vez que marques el paseo como terminado, este monto iniciará su proceso de liberación.",
                        retAgg,
                        sumaBruto(itemsRet),
                        sumaComision(itemsRet),
                        itemsRet),
                new BilleteraBucketResponse(
                        "verificacion",
                        "Saldo en Verificación",
                        BUCKET_VERIFICACION_DESCRIPCION,
                        verAgg,
                        sumaBruto(itemsVer),
                        sumaComision(itemsVer),
                        itemsVer),
                new BilleteraBucketResponse(
                        "disponible",
                        "Saldo Disponible",
                        "El monto mostrado es tu saldo disponible neto actual (columna saldo_actual). "
                                + "El historial de liberaciones se entrega aparte; su suma puede no coincidir si hubo retiros u otros movimientos.",
                        dispAgg,
                        BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP),
                        BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP),
                        List.of()),
                itemsLibHistorial,
                updatedAt);
    }

    private static List<BilleteraReservaItemResponse> aplicarYOrdenarDetalles(
            List<BilleteraReservaItemResponse> items,
            Map<Integer, ReservaBilleteraDetalleDto> detallePorReserva) {
        List<BilleteraReservaItemResponse> enriched =
                items.stream().map(i -> enriquecerItem(i, detallePorReserva)).toList();
        List<BilleteraReservaItemResponse> sorted = new ArrayList<>(enriched);
        sorted.sort(BilleteraService::compareAgendaDesc);
        return sorted;
    }

    private static BilleteraReservaItemResponse enriquecerItem(
            BilleteraReservaItemResponse i, Map<Integer, ReservaBilleteraDetalleDto> detallePorReserva) {
        if (detallePorReserva == null || detallePorReserva.isEmpty()) {
            return i;
        }
        ReservaBilleteraDetalleDto d = detallePorReserva.get(i.idReserva());
        if (d == null) {
            return i;
        }
        return new BilleteraReservaItemResponse(
                i.idReserva(),
                i.montoBruto(),
                i.comision(),
                i.montoNeto(),
                i.estadoEtiqueta(),
                d.mascotaNombre(),
                d.tutorNombre(),
                d.fechaAgenda(),
                d.horaInicio(),
                d.nombreEstadoReserva(),
                i.idTransaccionPagos(),
                i.disponibleParaRetiroEn());
    }

    /**
     * Recientes primero por fecha/hora de bloque cuando vienen de reserva-service.
     */
    private static int compareAgendaDesc(BilleteraReservaItemResponse a, BilleteraReservaItemResponse b) {
        String fa = a.fechaAgenda();
        String fb = b.fechaAgenda();
        if (fa == null && fb == null) {
            return Integer.compare(b.idReserva(), a.idReserva());
        }
        if (fa == null) {
            return 1;
        }
        if (fb == null) {
            return -1;
        }
        int c = fb.compareTo(fa);
        if (c != 0) {
            return c;
        }
        String ha = a.horaInicio() != null ? a.horaInicio() : "";
        String hb = b.horaInicio() != null ? b.horaInicio() : "";
        c = hb.compareTo(ha);
        if (c != 0) {
            return c;
        }
        return Integer.compare(b.idReserva(), a.idReserva());
    }

    private static BigDecimal sumaBruto(List<BilleteraReservaItemResponse> items) {
        return items.stream()
                .map(BilleteraReservaItemResponse::montoBruto)
                .map(BilleteraService::nz)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal sumaComision(List<BilleteraReservaItemResponse> items) {
        return items.stream()
                .map(BilleteraReservaItemResponse::comision)
                .map(BilleteraService::nz)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static String etiquetaFase(BilleteraReservaFase fase) {
        return fase == BilleteraReservaFase.EN_RETENIDO ? "EN_RETENIDO" : "EN_VERIFICACION";
    }

    private Billetera obtenerOCrearBilletera(Long idUsuarioPaseador) {
        return billeteraRepository.findByIdUsuario(idUsuarioPaseador).orElseGet(() -> billeteraRepository.save(
                Billetera.builder()
                        .idUsuario(idUsuarioPaseador)
                        .saldoActual(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP))
                        .saldoRetenido(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP))
                        .saldoVerificacion(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP))
                        .build()));
    }

    private static BigDecimal nz(BigDecimal v) {
        if (v == null) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return v.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
