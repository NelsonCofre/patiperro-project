package com.patiperro.reserva.service;

import com.patiperro.reserva.dto.PaseoDiarioDTO;
import com.patiperro.reserva.dto.ReservaPaseadorSolicitudResponseDTO;
import com.patiperro.reserva.dto.integracion.AgendaBloqueReservaClientDTO;
import com.patiperro.reserva.dto.integracion.TutorReservaClientDTO;
import com.patiperro.reserva.event.ReservaEventPublisher;
import com.patiperro.reserva.model.EstadoReserva;
import com.patiperro.reserva.model.EstadoReservaCatalogo;
import com.patiperro.reserva.model.Reserva;
import com.patiperro.reserva.repository.ReservaRepository;
import com.patiperro.reserva.security.JwtService;
import com.patiperro.reserva.support.AgendaIntegracionClient;
import com.patiperro.reserva.support.MascotaIntegracionClient;
import com.patiperro.reserva.support.PagosBilleteraIntegracionClient;
import com.patiperro.reserva.support.PagosCheckoutIntegracionClient;
import com.patiperro.reserva.support.PaseadorIntegracionClient;
import com.patiperro.reserva.support.TutorIntegracionClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservaServiceAgendaHoyTest {

    private static final String JWT = "test-jwt";
    private static final int ID_PASEADOR = 10;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private EstadoReservaService estadoReservaService;

    @Mock
    private AgendaIntegracionClient agendaIntegracionClient;

    @Mock
    private MascotaIntegracionClient mascotaIntegracionClient;

    @Mock
    private PaseadorIntegracionClient paseadorIntegracionClient;

    @Mock
    private TutorIntegracionClient tutorIntegracionClient;

    @Mock
    private JwtService jwtService;

    @Mock
    private ReservaEventPublisher reservaEventPublisher;

    @Mock
    private PaseoInicioSideEffectsService paseoInicioSideEffectsService;

    @Mock
    private ReservaReembolsoService reservaReembolsoService;

    @Mock
    private ReservaPagoService reservaPagoService;

    @Mock
    private PagosCheckoutIntegracionClient pagosCheckoutIntegracionClient;

    @Mock
    private PagosBilleteraIntegracionClient pagosBilleteraIntegracionClient;

    private ReservaService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-13T12:00:00Z"), ZoneOffset.UTC);
        service = new ReservaService(
                reservaRepository,
                estadoReservaService,
                agendaIntegracionClient,
                mascotaIntegracionClient,
                paseadorIntegracionClient,
                tutorIntegracionClient,
                jwtService,
                clock,
                reservaEventPublisher,
                paseoInicioSideEffectsService,
                reservaReembolsoService,
                reservaPagoService,
                pagosCheckoutIntegracionClient,
                pagosBilleteraIntegracionClient);
        when(jwtService.isTokenValid(JWT)).thenReturn(true);
        when(jwtService.extractPaseadorId(JWT)).thenReturn((long) ID_PASEADOR);
    }

    @Test
    void agendaDesactivada_panelYLegacy_vacios() {
        when(agendaIntegracionClient.isEnabled()).thenReturn(false);

        assertThat(service.listarAgendaDiariaPaseadorAceptadasHoyPanel(ID_PASEADOR, JWT)).isEmpty();
        assertThat(service.listarAgendaDiariaPaseadorAceptadasHoy(ID_PASEADOR, JWT)).isEmpty();
        verify(reservaRepository, never()).findByIdAgendaBloqueInAndEstadoReserva_IdEstadoReserva(anyList(), any());
    }

    @Test
    void sinBloquesParaHoy_noConsultaReservas() {
        when(agendaIntegracionClient.isEnabled()).thenReturn(true);
        AgendaBloqueReservaClientDTO bloqueOtroDia = bloque(100, ID_PASEADOR, LocalDate.of(2026, 5, 12), 9, 10);
        when(agendaIntegracionClient.listarBloquesPorUsuario(ID_PASEADOR, JWT)).thenReturn(List.of(bloqueOtroDia));

        assertThat(service.listarAgendaDiariaPaseadorAceptadasHoyPanel(ID_PASEADOR, JWT)).isEmpty();
        verify(reservaRepository, never()).findByIdAgendaBloqueInAndEstadoReserva_IdEstadoReserva(anyList(), any());
    }

    @Test
    void panel_tutorFalla_degradaDireccionYNoIncluyeCamposDeSolicitud() {
        when(agendaIntegracionClient.isEnabled()).thenReturn(true);
        AgendaBloqueReservaClientDTO bloque = bloque(100, ID_PASEADOR, LocalDate.of(2026, 5, 13), 9, 10);
        when(agendaIntegracionClient.listarBloquesPorUsuario(ID_PASEADOR, JWT)).thenReturn(List.of(bloque));
        when(reservaRepository.findByIdAgendaBloqueInAndEstadoReserva_IdEstadoReserva(
                        eq(List.of(100)), eq(EstadoReservaCatalogo.ID_ACEPTADA)))
                .thenReturn(List.of(reservaAceptada(1, 100, 20, 30)));
        when(tutorIntegracionClient.obtenerTutor(20L, JWT)).thenThrow(new IllegalStateException("tutores caído"));
        when(mascotaIntegracionClient.obtenerDetalleInterno(30)).thenReturn(null);
        when(mascotaIntegracionClient.obtenerPortadaInterno(30)).thenReturn(null);

        List<PaseoDiarioDTO> panel = service.listarAgendaDiariaPaseadorAceptadasHoyPanel(ID_PASEADOR, JWT);

        assertThat(panel).hasSize(1);
        PaseoDiarioDTO dto = panel.get(0);
        assertThat(dto.getIdReserva()).isEqualTo(1);
        assertThat(dto.getHoraInicio()).isEqualTo("09:00");
        assertThat(dto.getHoraFin()).isEqualTo("10:00");
        assertThat(dto.getInicioProgramado()).isEqualTo(LocalDateTime.of(2026, 5, 13, 9, 0));
        assertThat(dto.getComuna()).isEqualTo("—");
        assertThat(dto.getDireccionReferencia()).contains("Bloque agenda #100");
        assertThat(dto.getMascotaNombre()).isEqualTo("Mascota #30");
        assertThat(dto.getIdEstadoReserva()).isEqualTo(EstadoReservaCatalogo.ID_ACEPTADA);
    }

    @Test
    void legacy_devuelveSolicitudCompleta() {
        when(agendaIntegracionClient.isEnabled()).thenReturn(true);
        AgendaBloqueReservaClientDTO bloque = bloque(100, ID_PASEADOR, LocalDate.of(2026, 5, 13), 9, 10);
        when(agendaIntegracionClient.listarBloquesPorUsuario(ID_PASEADOR, JWT)).thenReturn(List.of(bloque));
        when(reservaRepository.findByIdAgendaBloqueInAndEstadoReserva_IdEstadoReserva(
                        eq(List.of(100)), eq(EstadoReservaCatalogo.ID_ACEPTADA)))
                .thenReturn(List.of(reservaAceptada(1, 100, 20, 30)));
        TutorReservaClientDTO tutor = new TutorReservaClientDTO();
        tutor.setPrimerNombre("Ana");
        tutor.setTelefono("+56900000000");
        when(tutorIntegracionClient.obtenerTutor(20L, JWT)).thenReturn(tutor);
        when(mascotaIntegracionClient.obtenerDetalleInterno(30)).thenReturn(null);
        when(mascotaIntegracionClient.obtenerPortadaInterno(30)).thenReturn(null);

        List<ReservaPaseadorSolicitudResponseDTO> legacy = service.listarAgendaDiariaPaseadorAceptadasHoy(ID_PASEADOR, JWT);

        assertThat(legacy).hasSize(1);
        assertThat(legacy.get(0).getTutorTelefono()).isEqualTo("+56900000000");
        assertThat(legacy.get(0).getTutorNombre()).contains("Ana");
        assertThat(legacy.get(0).getMontoTotal()).isEqualByComparingTo(BigDecimal.TEN);
    }

    @Test
    void sinJwt_lanza() {
        assertThatThrownBy(() -> service.listarAgendaDiariaPaseadorAceptadasHoyPanel(ID_PASEADOR, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT");
    }

    private static AgendaBloqueReservaClientDTO bloque(
            int idAgenda, int idUsuario, LocalDate fecha, int horaIni, int horaFin) {
        AgendaBloqueReservaClientDTO b = new AgendaBloqueReservaClientDTO();
        b.setIdAgenda(idAgenda);
        b.setIdUsuario(idUsuario);
        b.setFecha(fecha);
        b.setHoraInicio(LocalDateTime.of(fecha, java.time.LocalTime.of(horaIni, 0)));
        b.setHoraFinal(LocalDateTime.of(fecha, java.time.LocalTime.of(horaFin, 0)));
        return b;
    }

    private static Reserva reservaAceptada(int idReserva, int idAgendaBloque, int idTutorUsuario, int idMascota) {
        EstadoReserva estado = new EstadoReserva();
        estado.setIdEstadoReserva(EstadoReservaCatalogo.ID_ACEPTADA);
        estado.setNombreEstado("Aceptada");
        Reserva r = new Reserva();
        r.setIdReserva(idReserva);
        r.setIdAgendaBloque(idAgendaBloque);
        r.setIdTutorUsuario(idTutorUsuario);
        r.setIdMascota(idMascota);
        r.setIdTarifa(1);
        r.setFechaSolicitud(LocalDateTime.of(2026, 5, 10, 8, 0));
        r.setMontoTotal(BigDecimal.TEN);
        r.setEstadoReserva(estado);
        r.setCodigoEncuentro(null);
        return r;
    }
}
