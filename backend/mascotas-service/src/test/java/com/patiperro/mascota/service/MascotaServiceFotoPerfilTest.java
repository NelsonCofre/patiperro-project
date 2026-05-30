package com.patiperro.mascota.service;

import com.patiperro.mascota.exception.ForbiddenOperationException;
import com.patiperro.mascota.model.Mascota;
import com.patiperro.mascota.repository.EspecieRepository;
import com.patiperro.mascota.repository.FotoRepository;
import com.patiperro.mascota.repository.MascotaRepository;
import com.patiperro.mascota.repository.RazaRepository;
import com.patiperro.mascota.repository.TamanoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MascotaServiceFotoPerfilTest {

    @Mock
    private MascotaRepository mascotaRepository;
    @Mock
    private EspecieRepository especieRepository;
    @Mock
    private RazaRepository razaRepository;
    @Mock
    private TamanoRepository tamanoRepository;
    @Mock
    private FotoRepository fotoRepository;
    @Mock
    private MascotaFotoStorageService mascotaFotoStorageService;

    @InjectMocks
    private MascotaService mascotaService;

    @Test
    void actualizarFotoPerfilRechazaMascotaDeOtroTutor() throws Exception {
        Mascota ajena = new Mascota();
        ajena.setIdMascota(10L);
        ajena.setIdTutor(99L);
        when(mascotaRepository.findById(10L)).thenReturn(Optional.of(ajena));

        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", new byte[] {1});

        assertThrows(ForbiddenOperationException.class,
                () -> mascotaService.actualizarFotoPerfil(10L, file, 1L));

        verify(mascotaFotoStorageService, never()).save(any());
    }
}
