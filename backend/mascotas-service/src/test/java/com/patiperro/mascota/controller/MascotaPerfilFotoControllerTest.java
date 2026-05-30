package com.patiperro.mascota.controller;

import com.patiperro.mascota.service.MascotaFotoStorageService;
import com.patiperro.mascota.service.MascotaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MascotaPerfilFotoControllerTest {

    @Mock
    private MascotaService mascotaService;

    @Mock
    private MascotaFotoStorageService mascotaFotoStorageService;

    @InjectMocks
    private MascotaPerfilFotoController controller;

    @Test
    void serveConNombreInvalidoDevuelve404() throws Exception {
        when(mascotaFotoStorageService.resolveExisting("no-uuid.jpg")).thenReturn(null);

        ResponseEntity<?> response = controller.serve("no-uuid.jpg");

        assertEquals(404, response.getStatusCode().value());
    }
}
