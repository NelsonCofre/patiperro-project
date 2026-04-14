package com.patiperro.agenda.service;

import com.patiperro.agenda.dto.AgendaBloqueoDiaRequestDTO;
import com.patiperro.agenda.dto.AgendaBloqueoDiaResponseDTO;
import com.patiperro.agenda.dto.WalkerBlackoutRequestDTO;
import com.patiperro.agenda.repository.AgendaBloqueRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalkerBlackoutService {

    private static final String MSG_CONFLICTO_RESERVAS =
            "Tienes paseos programados para este día. Debes gestionarlos individualmente antes de bloquear la fecha completa";

    private final AgendaBloqueRepository agendaBloqueRepository;
    private final AgendaBloqueoDiaService agendaBloqueoDiaService;
    private final RestClient reservaRestClient;

    @Transactional
    public List<AgendaBloqueoDiaResponseDTO> registrarBlackoutConChequeoReservas(WalkerBlackoutRequestDTO body) {
        LocalDate inicio = body.getFechaInicio();
        LocalDate fin = body.getFechaFin() != null ? body.getFechaFin() : body.getFechaInicio();
        if (inicio.isAfter(fin)) {
            throw new IllegalArgumentException("La fecha 'fechaInicio' no puede ser posterior a 'fechaFin'");
        }

        LocalDate hoy = LocalDate.now();
        if (inicio.isBefore(hoy)) {
            throw new IllegalArgumentException("No puedes bloquear fechas pasadas");
        }

        List<Integer> idsBloques = agendaBloqueRepository.findIdsByIdUsuarioAndFechaBetween(
                body.getIdUsuario(), inicio, fin);

        if (!idsBloques.isEmpty()) {
            try {
                Boolean conflicto = reservaRestClient.post()
                        .uri("/api/reserva/interno/conflicto-bloqueo")
                        .headers(h -> {
                            String auth = currentAuthorizationHeader();
                            if (auth != null && !auth.isBlank()) {
                                h.set("Authorization", auth);
                            }
                        })
                        .body(idsBloques)
                        .retrieve()
                        .body(Boolean.class);
                if (Boolean.TRUE.equals(conflicto)) {
                    throw new IllegalStateException(MSG_CONFLICTO_RESERVAS);
                }
            } catch (RestClientException e) {
                throw new IllegalArgumentException(
                        "No se pudo verificar conflictos con reservas. Intenta más tarde.");
            }
        }

        List<AgendaBloqueoDiaResponseDTO> creados = new ArrayList<>();
        for (LocalDate d = inicio; !d.isAfter(fin); d = d.plusDays(1)) {
            if (agendaBloqueoDiaService.existeBloqueo(body.getIdUsuario(), d)) {
                continue;
            }
            AgendaBloqueoDiaRequestDTO req = new AgendaBloqueoDiaRequestDTO();
            req.setIdUsuario(body.getIdUsuario());
            req.setFecha(d);
            req.setMotivo(body.getMotivo());
            creados.add(agendaBloqueoDiaService.crear(req));
        }
        return creados;
    }

    @Transactional
    public void eliminarBlackout(Integer id) {
        agendaBloqueoDiaService.eliminar(id);
    }

    private static String currentAuthorizationHeader() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        return request.getHeader("Authorization");
    }
}