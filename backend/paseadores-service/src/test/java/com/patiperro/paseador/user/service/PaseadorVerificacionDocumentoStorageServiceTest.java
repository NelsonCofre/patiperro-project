package com.patiperro.paseador.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void savePdf_aceptaPdfConMagicBytes() throws Exception {
        byte[] pdf = new byte[]{'%', 'P', 'D', 'F', '-', '1', '.', '4'};
        MockMultipartFile file = new MockMultipartFile(
                "documento",
                "cedula.pdf",
                "application/pdf",
                pdf);
        String filename = storageService.savePdf(file);
        assertNotNull(storageService.resolveExisting(filename));
    }

    @Test
    void savePdf_rechazaJpeg() {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02};
        MockMultipartFile file = new MockMultipartFile(
                "documento",
                "cedula.jpg",
                "image/jpeg",
                jpeg);
        assertThrows(IllegalArgumentException.class, () -> storageService.savePdf(file));
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
    void save_aceptaPngConMagicBytes() throws Exception {
        byte[] png = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x00
        };
        MockMultipartFile file = new MockMultipartFile(
                "cedulaFrontal",
                "cedula.png",
                "image/png",
                png);
        String filename = storageService.save(file);
        assertNotNull(storageService.resolveExisting(filename));
    }

    @Test
    void save_aceptaPdfConMagicBytes() throws Exception {
        byte[] pdf = "%PDF-1.4".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "cedulaReverso",
                "cedula.pdf",
                "application/pdf",
                pdf);
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

    @Test
    void resolveExisting_rechazaNombreConPathTraversal() {
        assertNull(storageService.resolveExisting("../../../secret.pdf"));
        assertNull(storageService.resolveExisting("not-a-uuid.jpg"));
    }

    @Test
    void constructor_usaDefaultSiMaxBytesInvalido() throws Exception {
        PaseadorVerificacionDocumentoStorageService conDefault =
                new PaseadorVerificacionDocumentoStorageService(tempDir.toString(), 0);
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        MockMultipartFile file = new MockMultipartFile("f", "a.jpg", "image/jpeg", jpeg);
        assertNotNull(conDefault.save(file));
    }

    @Test
    void save_devuelveNombreUuidConExtensionPermitida() throws Exception {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        MockMultipartFile file = new MockMultipartFile("cedulaFrontal", "doc.JPG", "image/jpeg", jpeg);
        String filename = storageService.save(file);
        assertTrue(filename.matches("^[a-fA-F0-9\\-]{36}\\.jpg$"));
    }

    @Test
    void save_rechazaContentTypeInvalidoSiVieneInformado() {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        MockMultipartFile file = new MockMultipartFile(
                "cedulaFrontal",
                "cedula.jpg",
                "application/octet-stream",
                jpeg);
        assertThrows(IllegalArgumentException.class, () -> storageService.save(file));
    }

    @Test
    void deleteQuietly_eliminaArchivoGuardado() throws Exception {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        MockMultipartFile file = new MockMultipartFile("cedulaFrontal", "cedula.jpg", "image/jpeg", jpeg);
        String filename = storageService.save(file);
        assertNotNull(storageService.resolveExisting(filename));
        storageService.deleteQuietly(filename);
        assertNull(storageService.resolveExisting(filename));
    }
}
