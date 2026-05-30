package com.patiperro.chat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Almacenamiento local de fotos del paseo (mismo enfoque que {@code PaseadorPerfilFotoStorageService}).
 */
@Service
public class ChatMediaStorageService {

	public static final String MSG_VALIDACION_IMAGEN =
			"Solo puedes enviar imágenes (JPG/PNG) de hasta 10MB para asegurar la rapidez del chat";

	private static final Set<String> ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png");
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

	private final Path uploadDir;
	private final long maxBytes;

	public ChatMediaStorageService(
			@Value("${patiperro.chat.media.upload-dir:uploads/chat-paseo}") String uploadDirProperty,
			@Value("${patiperro.chat.media.max-bytes:10485760}") long maxBytes) {
		this.uploadDir = Paths.get(uploadDirProperty).toAbsolutePath().normalize();
		this.maxBytes = maxBytes > 0 ? maxBytes : 10L * 1024 * 1024;
	}

	public String save(MultipartFile file) throws IOException {
		validarArchivo(file);

		String ext = extensionOf(file.getOriginalFilename());
		byte[] header = leerCabecera(file);
		validarMagicBytes(ext, header);

		Files.createDirectories(uploadDir);
		String filename = UUID.randomUUID() + ext.toLowerCase(Locale.ROOT);
		Path target = uploadDir.resolve(filename).normalize();
		if (!target.startsWith(uploadDir)) {
			throw new IllegalArgumentException(MSG_VALIDACION_IMAGEN);
		}
		try (var in = file.getInputStream()) {
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
		}
		return filename;
	}

	public Path resolveExisting(String filename) {
		if (filename == null || !filename.matches("^[a-fA-F0-9\\-]{36}\\.(jpg|jpeg|png)$")) {
			return null;
		}
		Path target = uploadDir.resolve(filename).normalize();
		if (!target.startsWith(uploadDir) || !Files.isRegularFile(target)) {
			return null;
		}
		return target;
	}

	/** Elimina un archivo subido si la transacción de mensaje no se confirmó (rollback). */
	public void deleteQuietly(String filename) {
		Path path = resolveExisting(filename);
		if (path == null) {
			return;
		}
		try {
			Files.deleteIfExists(path);
		} catch (IOException ignored) {
			// best-effort
		}
	}

	private void validarArchivo(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException(MSG_VALIDACION_IMAGEN);
		}
		if (file.getSize() > maxBytes) {
			throw new IllegalArgumentException(MSG_VALIDACION_IMAGEN);
		}
		String ext = extensionOf(file.getOriginalFilename());
		if (ext == null || !ALLOWED_EXT.contains(ext.toLowerCase(Locale.ROOT))) {
			throw new IllegalArgumentException(MSG_VALIDACION_IMAGEN);
		}
		String contentType = file.getContentType();
		if (contentType != null && !contentType.isBlank()) {
			String normalized = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
			if (!ALLOWED_CONTENT_TYPES.contains(normalized)) {
				throw new IllegalArgumentException(MSG_VALIDACION_IMAGEN);
			}
		}
	}

	private static byte[] leerCabecera(MultipartFile file) throws IOException {
		try (var in = file.getInputStream()) {
			return in.readNBytes(12);
		}
	}

	private static void validarMagicBytes(String ext, byte[] header) {
		if (header == null || header.length < 3) {
			throw new IllegalArgumentException(MSG_VALIDACION_IMAGEN);
		}
		boolean jpeg = esJpeg(header);
		boolean png = esPng(header);
		if (!jpeg && !png) {
			throw new IllegalArgumentException(MSG_VALIDACION_IMAGEN);
		}
		String extLower = ext != null ? ext.toLowerCase(Locale.ROOT) : "";
		if (extLower.equals(".png") && !png) {
			throw new IllegalArgumentException(MSG_VALIDACION_IMAGEN);
		}
		if ((extLower.equals(".jpg") || extLower.equals(".jpeg")) && !jpeg) {
			throw new IllegalArgumentException(MSG_VALIDACION_IMAGEN);
		}
	}

	private static boolean esJpeg(byte[] header) {
		return header.length >= 3
				&& (header[0] & 0xFF) == 0xFF
				&& (header[1] & 0xFF) == 0xD8
				&& (header[2] & 0xFF) == 0xFF;
	}

	private static boolean esPng(byte[] header) {
		return header.length >= 8
				&& header[0] == (byte) 0x89
				&& header[1] == 0x50
				&& header[2] == 0x4E
				&& header[3] == 0x47
				&& header[4] == 0x0D
				&& header[5] == 0x0A
				&& header[6] == 0x1A
				&& header[7] == 0x0A;
	}

	private static String extensionOf(String originalName) {
		if (originalName == null || !originalName.contains(".")) {
			return null;
		}
		return originalName.substring(originalName.lastIndexOf('.'));
	}
}
