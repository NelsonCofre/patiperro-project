package com.patiperro.mascota.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MascotaFotoStorageServiceTest {

    @TempDir
    Path tempDir;

    private MascotaFotoStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new MascotaFotoStorageService(tempDir.toString(), 5L * 1024 * 1024);
    }

    @Test
    void rechazaArchivoVacio() {
        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", new byte[0]);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> storageService.save(file));
        assertTrue(ex.getMessage().contains("JPG"));
    }

    @Test
    void rechazaExtensionPdf() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "%PDF".getBytes());
        assertThrows(IllegalArgumentException.class, () -> storageService.save(file));
    }

    @Test
    void rechazaJpgSinCabeceraJpeg() {
        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", "not-an-image".getBytes());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> storageService.save(file));
        assertEquals(MascotaFotoStorageService.MSG_FORMATO, ex.getMessage());
    }

    @Test
    void rechazaArchivoMayorA5Mb() {
        byte[] oversized = new byte[(int) (5L * 1024 * 1024) + 1];
        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", oversized);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> storageService.save(file));
        assertEquals(MascotaFotoStorageService.MSG_PESO, ex.getMessage());
    }

    @Test
    void guardaPngValidoYResuelvePorUuid() throws Exception {
        byte[] png = {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x00
        };
        MockMultipartFile file = new MockMultipartFile("file", "mascota.png", "image/png", png);
        String filename = storageService.save(file);
        assertNotNull(filename);
        assertTrue(filename.endsWith(".png"));
        assertNotNull(storageService.resolveExisting(filename));
        assertNull(storageService.resolveExisting("../" + filename));
        assertNull(storageService.resolveExisting("../../../etc/passwd"));
    }

    @Test
    void buildPublicUrlYFilenameFromUrlSonConsistentes() throws Exception {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46};
        MockMultipartFile file = new MockMultipartFile("file", "perro.jpeg", "image/jpeg", jpeg);
        String filename = storageService.save(file);
        String url = storageService.buildPublicUrl(filename);
        assertEquals(filename, storageService.filenameFromPublicUrl(url));
        assertNull(storageService.filenameFromPublicUrl("https://evil.example/steal.jpg"));
    }
}
