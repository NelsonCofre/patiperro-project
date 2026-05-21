package com.patiperro.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatMediaStorageServiceTest {

	@TempDir
	Path tempDir;

	private ChatMediaStorageService storageService;

	@BeforeEach
	void setUp() {
		storageService = new ChatMediaStorageService(tempDir.toString(), 10L * 1024 * 1024);
	}

	@Test
	void saveAceptaJpegValido() throws Exception {
		byte[] jpeg = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0};
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"foto.jpg",
				"image/jpeg",
				jpeg);

		String filename = storageService.save(file);

		assertNotNull(filename);
		assertTrue(filename.endsWith(".jpg"));
		assertNotNull(storageService.resolveExisting(filename));
	}

	@Test
	void saveRechazaArchivoVacio() {
		MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", new byte[0]);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> storageService.save(file));
		assertTrue(ex.getMessage().contains("JPG/PNG"));
	}

	@Test
	void saveRechazaExtensionNoPermitida() {
		byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
		MockMultipartFile file = new MockMultipartFile("file", "foto.gif", "image/gif", png);

		assertThrows(IllegalArgumentException.class, () -> storageService.save(file));
	}

	@Test
	void resolveExistingRechazaNombreInvalido() {
		assertNull(storageService.resolveExisting("../etc/passwd"));
		assertNull(storageService.resolveExisting("no-uuid.jpg"));
	}
}
