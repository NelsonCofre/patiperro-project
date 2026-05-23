package com.patiperro.paseador.user.service;

import com.patiperro.paseador.model.EstadoVerificacionIdentidad;
import com.patiperro.paseador.model.Paseador;
import com.patiperro.paseador.repository.PaseadorRepository;
import com.patiperro.paseador.user.dto.VerificacionIdentidadResponseDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaseadorVerificacionServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private PaseadorRepository paseadorRepository;

    @InjectMocks
    private PaseadorVerificacionService verificacionService;

    private PaseadorVerificacionDocumentoStorageService storageService;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void obtenerEstadoAutenticado_sinSesion_lanza401() {
        assertThrows(
                ResponseStatusException.class,
                () -> verificacionService.obtenerEstadoAutenticado());
    }

    @Test
    void subirDocumentos_rechazaSiVerificacionEnProceso() throws Exception {
        initStorage();
        Paseador paseador = paseadorEnProceso();
        autenticar(paseador.getCorreo());
        when(paseadorRepository.findByCorreo(paseador.getCorreo())).thenReturn(Optional.of(paseador));

        MockMultipartFile frontal = jpegFile("cedulaFrontal", "frontal.jpg");
        MockMultipartFile reverso = jpegFile("cedulaReverso", "reverso.jpg");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> verificacionService.subirDocumentos(frontal, reverso));

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void subirDocumentos_rechazaSiYaAprobado() throws Exception {
        initStorage();
        Paseador paseador = Paseador.builder()
                .id(4L)
                .correo("aprobado@test.cl")
                .estadoVerificacionIdentidad(EstadoVerificacionIdentidad.APROBADO)
                .archivoCedulaFrontal("f.jpg")
                .archivoCedulaReverso("r.jpg")
                .build();
        autenticar(paseador.getCorreo());
        when(paseadorRepository.findByCorreo(paseador.getCorreo())).thenReturn(Optional.of(paseador));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> verificacionService.subirDocumentos(
                        jpegFile("cedulaFrontal", "frontal.jpg"),
                        jpegFile("cedulaReverso", "reverso.jpg")));

        assertEquals(409, ex.getStatusCode().value());
        verify(paseadorRepository, never()).save(any());
    }

    @Test
    void subirDocumentos_pasaAEnProcesoCuandoPuedeSubir() throws Exception {
        initStorage();
        Paseador paseador = Paseador.builder()
                .id(1L)
                .correo("paseador@test.cl")
                .estadoVerificacionIdentidad(EstadoVerificacionIdentidad.SIN_ENVIAR)
                .build();
        autenticar(paseador.getCorreo());
        when(paseadorRepository.findByCorreo(paseador.getCorreo())).thenReturn(Optional.of(paseador));
        when(paseadorRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(paseador));
        when(paseadorRepository.save(any(Paseador.class))).thenAnswer(inv -> inv.getArgument(0));

        VerificacionIdentidadResponseDTO response = verificacionService.subirDocumentos(
                jpegFile("cedulaFrontal", "frontal.jpg"),
                jpegFile("cedulaReverso", "reverso.jpg"));

        assertEquals(EstadoVerificacionIdentidad.EN_PROCESO, response.getEstado());
        assertFalse(response.isPuedeSubir());
        assertTrue(response.isTieneFrontal());
        assertTrue(response.isTieneReverso());
        verify(paseadorRepository).save(any(Paseador.class));
    }

    @Test
    void revisarVerificacionInterna_rechazado_conMotivo_guardaMotivo() {
        Paseador paseador = paseadorEnProceso();
        when(paseadorRepository.findById(2L)).thenReturn(Optional.of(paseador));
        when(paseadorRepository.save(any(Paseador.class))).thenAnswer(inv -> inv.getArgument(0));

        VerificacionIdentidadResponseDTO response = verificacionService.revisarVerificacionInterna(
                2L,
                EstadoVerificacionIdentidad.RECHAZADO,
                "Documento ilegible");

        assertEquals(EstadoVerificacionIdentidad.RECHAZADO, response.getEstado());
        assertEquals("Documento ilegible", response.getMotivoRechazo());
        assertTrue(response.isPuedeSubir());
    }

    @Test
    void revisarVerificacionInterna_sinDocumentos_lanza409() {
        Paseador paseador = Paseador.builder()
                .id(5L)
                .estadoVerificacionIdentidad(EstadoVerificacionIdentidad.EN_PROCESO)
                .build();
        when(paseadorRepository.findById(5L)).thenReturn(Optional.of(paseador));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> verificacionService.revisarVerificacionInterna(
                        5L,
                        EstadoVerificacionIdentidad.APROBADO,
                        null));

        assertEquals(409, ex.getStatusCode().value());
        verify(paseadorRepository, never()).save(any());
    }

    @Test
    void revisarVerificacionInterna_aprobado_actualizaEstado() {
        Paseador paseador = paseadorEnProceso();
        when(paseadorRepository.findById(2L)).thenReturn(Optional.of(paseador));
        when(paseadorRepository.save(any(Paseador.class))).thenAnswer(inv -> inv.getArgument(0));

        VerificacionIdentidadResponseDTO response = verificacionService.revisarVerificacionInterna(
                2L,
                EstadoVerificacionIdentidad.APROBADO,
                null);

        assertEquals(EstadoVerificacionIdentidad.APROBADO, response.getEstado());
        assertFalse(response.isPuedeSubir());
        assertNull(response.getMotivoRechazo());
        verify(paseadorRepository).save(any(Paseador.class));
    }

    @Test
    void revisarVerificacionInterna_rechazadoSinMotivo_lanza400() {
        assertThrows(
                IllegalArgumentException.class,
                () -> verificacionService.revisarVerificacionInterna(
                        2L,
                        EstadoVerificacionIdentidad.RECHAZADO,
                        "  "));
        verify(paseadorRepository, never()).save(any());
    }

    @Test
    void revisarVerificacionInterna_noEnProceso_lanza409() {
        Paseador paseador = Paseador.builder()
                .id(3L)
                .estadoVerificacionIdentidad(EstadoVerificacionIdentidad.SIN_ENVIAR)
                .archivoCedulaFrontal("a.jpg")
                .archivoCedulaReverso("b.jpg")
                .build();
        when(paseadorRepository.findById(3L)).thenReturn(Optional.of(paseador));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> verificacionService.revisarVerificacionInterna(
                        3L,
                        EstadoVerificacionIdentidad.APROBADO,
                        null));

        assertEquals(409, ex.getStatusCode().value());
        verify(paseadorRepository, never()).save(any());
    }

    @Test
    void resolverDocumentoAutenticado_ladoInvalido_lanza400() throws Exception {
        initStorage();
        Paseador paseador = paseadorEnProceso();
        autenticar(paseador.getCorreo());
        when(paseadorRepository.findByCorreo(paseador.getCorreo())).thenReturn(Optional.of(paseador));

        assertThrows(
                IllegalArgumentException.class,
                () -> verificacionService.resolverDocumentoAutenticado("trasero"));
    }

    @Test
    void toResponse_marcaPuedeSubirSiRechazado() {
        Paseador paseador = Paseador.builder()
                .estadoVerificacionIdentidad(EstadoVerificacionIdentidad.RECHAZADO)
                .motivoRechazoVerificacionIdentidad("Documento ilegible")
                .build();

        VerificacionIdentidadResponseDTO response = VerificacionIdentidadResponseDTO.from(paseador);

        assertTrue(response.isPuedeSubir());
        assertEquals("Rechazado", response.getEstadoEtiqueta());
    }

    private void initStorage() {
        storageService = new PaseadorVerificacionDocumentoStorageService(
                tempDir.toString(),
                5 * 1024 * 1024);
        verificacionService = new PaseadorVerificacionService(paseadorRepository, storageService);
    }

    private static Paseador paseadorEnProceso() {
        return Paseador.builder()
                .id(2L)
                .correo("enproceso@test.cl")
                .estadoVerificacionIdentidad(EstadoVerificacionIdentidad.EN_PROCESO)
                .archivoCedulaFrontal("existing-front.jpg")
                .archivoCedulaReverso("existing-back.jpg")
                .build();
    }

    private static void autenticar(String correo) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(correo, null));
    }

    private static MockMultipartFile jpegFile(String name, String originalFilename) {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02};
        return new MockMultipartFile(name, originalFilename, "image/jpeg", jpeg);
    }
}
