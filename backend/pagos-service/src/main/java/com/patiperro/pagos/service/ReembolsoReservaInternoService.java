package com.patiperro.pagos.service;

import com.patiperro.pagos.model.EstadoPago;
import com.patiperro.pagos.model.PagoExterno;
import com.patiperro.pagos.model.Transaccion;
import com.patiperro.pagos.repository.PagoExternoRepository;
import com.patiperro.pagos.repository.TransaccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReembolsoReservaInternoService {

    private final TransaccionRepository transaccionRepository;
    private final PagoExternoRepository pagoExternoRepository;

    public ReembolsoFlags consultarFlags(Integer idReserva) {
        if (idReserva == null) {
            return new ReembolsoFlags(false, false, false);
        }
        Optional<Transaccion> txOpt = transaccionRepository
                .findFirstByIdReservaAndEstadoPagoOrderByIdTransaccionDesc(idReserva.longValue(), EstadoPago.APROBADO);
        if (txOpt.isEmpty()) {
            return new ReembolsoFlags(false, false, false);
        }
        Transaccion tx = txOpt.get();
        Optional<PagoExterno> peOpt = pagoExternoRepository.findByTransaccion_IdTransaccion(tx.getIdTransaccion());
        if (peOpt.isEmpty()) {
            return new ReembolsoFlags(true, false, false);
        }
        PagoExterno pe = peOpt.get();
        boolean esMp = PagoExternoService.PROVIDER_MERCADOPAGO.equalsIgnoreCase(safe(pe.getProvider()));
        if (!esMp) {
            return new ReembolsoFlags(true, false, false);
        }
        boolean reembolsoMp = StringUtils.hasText(pe.getRefundProviderId()) || pe.getRefundFecha() != null;
        boolean correo = pe.getNotificacionReembolsoCorreoEnviadaEn() != null;
        return new ReembolsoFlags(true, reembolsoMp, correo);
    }

    public List<Integer> listarCandidatosCorreoReembolso(Pageable pageable) {
        List<PagoExterno> rows = pagoExternoRepository.findCorreoReembolsoPendiente(
                PagoExternoService.PROVIDER_MERCADOPAGO, EstadoPago.APROBADO, pageable);
        Set<Integer> seen = new LinkedHashSet<>();
        List<Integer> out = new ArrayList<>();
        for (PagoExterno pe : rows) {
            Transaccion t = pe.getTransaccion();
            if (t == null || t.getIdReserva() == null) {
                continue;
            }
            int id = t.getIdReserva().intValue();
            if (seen.add(id)) {
                out.add(id);
            }
        }
        return out;
    }

    @Transactional
    public void marcarCorreoReembolsoEnviado(Integer idReserva) {
        if (idReserva == null) {
            return;
        }
        LocalDateTime ahora = LocalDateTime.now();
        transaccionRepository
                .findFirstByIdReservaAndEstadoPagoOrderByIdTransaccionDesc(idReserva.longValue(), EstadoPago.APROBADO)
                .flatMap(tx -> pagoExternoRepository.findByTransaccion_IdTransaccion(tx.getIdTransaccion()))
                .ifPresent(pe -> {
                    pe.setNotificacionReembolsoCorreoEnviadaEn(ahora);
                    pagoExternoRepository.save(pe);
                });
    }

    private static String safe(String s) {
        if (s == null) {
            return "";
        }
        return s.trim();
    }

    public record ReembolsoFlags(boolean tieneCobroAprobadoMp, boolean reembolsoMpRegistrado, boolean correoReembolsoEnviado) {
    }
}
