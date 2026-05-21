package com.patiperro.paseador.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaseadorVerificacionDocumentoStorageServiceTest {

    @TempDir
    Path tempDir;

    private PaseadorVerificacionDocumentoStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new PaseadorVerificacionDocumentoStorageService(
                tempDir.toString(),
                5 * 1024 * 1024);
    }

    @Test
    void save_aceptaJpegConMagicBytes() throws Exception {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02};
        MockMultipartFile file = new MockMultipartFile(
                "cedulaFrontal",
                "cedula.jpg",
                "image/jpeg",
                jpeg);
        String filename = storageService.save(file);
        assertNotNull(storageService.resolveExisting(filename));
    }

    @Test
    void save_rechazaExtensionSinMagic() {
        MockMultipartFile file = new MockMultipartFile(
                "cedulaFrontal",
                "fake.jpg",
                "image/jpeg",
                "not-an-image".getBytes());
        assertThrows(IllegalArgumentException.class, () -> storageService.save(file));
    }

    @Test
    void save_rechazaArchivoMayorA5Mb() {
        byte[] big = new byte[5 * 1024 * 1024 + 1];
        big[0] = (byte) 0xFF;
        big[1] = (byte) 0xD8;
        big[2] = (byte) 0xFF;
        MockMultipartFile file = new MockMultipartFile(
                "cedulaFrontal",
                "big.jpg",
                "image/jpeg",
                big);
        assertThrows(IllegalArgumentException.class, () -> storageService.save(file));
    }
}
