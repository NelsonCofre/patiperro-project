package com.patiperro.mascota.service;

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
 * Almacenamiento local de foto de perfil de mascota (mismo enfoque que
 * {@code TutorPerfilFotoStorageService} / {@code ChatMediaStorageService}, con validación reforzada).
 */
@Service
public class MascotaFotoStorageService {

    public static final String PUBLIC_URL_PREFIX = "/api/mascotas/public/mascota/";

    public static final String MSG_FORMATO =
            "El archivo seleccionado no es válido. Por favor, sube una imagen en formato JPG, JPEG o PNG.";
    public static final String MSG_PESO =
            "La imagen es demasiado pesada. El tamaño máximo permitido es 5MB.";

    private static final Set<String> ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");
    private static final String FILENAME_PATTERN = "^[a-fA-F0-9\\-]{36}\\.(jpg|jpeg|png)$";

    private final Path uploadDir;
    private final long maxBytes;

    public MascotaFotoStorageService(
            @Value("${patiperro.mascotas.foto.upload-dir:uploads/mascota-perfil}") String uploadDirProperty,
            @Value("${patiperro.mascotas.foto.max-bytes:5242880}") long maxBytes) {
        this.uploadDir = Paths.get(uploadDirProperty).toAbsolutePath().normalize();
        this.maxBytes = maxBytes > 0 ? maxBytes : 5L * 1024 * 1024;
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
            throw new IllegalArgumentException(MSG_FORMATO);
        }
        try (var in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return filename;
    }

    public String buildPublicUrl(String filename) {
        return PUBLIC_URL_PREFIX + filename;
    }

    public Path resolveExisting(String filename) {
        if (filename == null || !filename.matches(FILENAME_PATTERN)) {
            return null;
        }
        Path target = uploadDir.resolve(filename).normalize();
        if (!target.startsWith(uploadDir) || !Files.isRegularFile(target)) {
            return null;
        }
        return target;
    }

    /**
     * Solo borra archivos subidos por este servicio ({@link #PUBLIC_URL_PREFIX}).
     */
    public String filenameFromPublicUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();
        if (!trimmed.startsWith(PUBLIC_URL_PREFIX)) {
            return null;
        }
        String filename = trimmed.substring(PUBLIC_URL_PREFIX.length());
        if (!filename.matches(FILENAME_PATTERN)) {
            return null;
        }
        return filename;
    }

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
            throw new IllegalArgumentException(MSG_FORMATO);
        }
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(MSG_PESO);
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (ext == null || !ALLOWED_EXT.contains(ext.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(MSG_FORMATO);
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String normalized = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
            if (!ALLOWED_CONTENT_TYPES.contains(normalized)) {
                throw new IllegalArgumentException(MSG_FORMATO);
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
            throw new IllegalArgumentException(MSG_FORMATO);
        }
        boolean jpeg = esJpeg(header);
        boolean png = esPng(header);
        if (!jpeg && !png) {
            throw new IllegalArgumentException(MSG_FORMATO);
        }
        String extLower = ext != null ? ext.toLowerCase(Locale.ROOT) : "";
        if (extLower.equals(".png") && !png) {
            throw new IllegalArgumentException(MSG_FORMATO);
        }
        if ((extLower.equals(".jpg") || extLower.equals(".jpeg")) && !jpeg) {
            throw new IllegalArgumentException(MSG_FORMATO);
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
