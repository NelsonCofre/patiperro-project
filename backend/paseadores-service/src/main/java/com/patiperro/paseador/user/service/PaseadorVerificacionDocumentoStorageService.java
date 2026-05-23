package com.patiperro.paseador.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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
 * Almacenamiento local de documentos de verificación de identidad (cédula).
 * Mismo enfoque que {@link PaseadorPerfilFotoStorageService} y el media storage de chat-service:
 * UUID en disco, validación de extensión/MIME/magic bytes y resolución segura por regex.
 */
@Service
public class PaseadorVerificacionDocumentoStorageService {

    public static final String MSG_VALIDACION_DOCUMENTO =
            "Formato no permitido o archivo inválido (usa JPG, PNG o PDF de hasta 5 MB)";

    private static final long DEFAULT_MAX_BYTES = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png", ".pdf");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "application/pdf");
    private static final String FILENAME_PATTERN =
            "^[a-fA-F0-9\\-]{36}\\.(jpg|jpeg|png|pdf)$";

    private final Path uploadDir;
    private final long maxFileSizeBytes;

    public PaseadorVerificacionDocumentoStorageService(
            @Value("${patiperro.paseadores.verificacion-identidad.upload-dir:uploads/paseador-verificacion}") String uploadDirProperty,
            @Value("${patiperro.paseadores.verificacion-identidad.max-file-size-bytes:5242880}") long maxFileSizeBytes) {
        String dir = StringUtils.hasText(uploadDirProperty)
                ? uploadDirProperty
                : "uploads/paseador-verificacion";
        this.uploadDir = Paths.get(dir).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes > 0 ? maxFileSizeBytes : DEFAULT_MAX_BYTES;
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
            throw new IllegalArgumentException(MSG_VALIDACION_DOCUMENTO);
        }
        try (var in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return filename;
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

    /** Limpieza best-effort tras rollback o reemplazo de documentos. */
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
            throw new IllegalArgumentException("Archivo vacío");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo permitido (5 MB)");
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (ext == null || !ALLOWED_EXT.contains(ext.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Formato no permitido (usa JPG, PNG o PDF)");
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String normalized = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
            if (!ALLOWED_CONTENT_TYPES.contains(normalized)) {
                throw new IllegalArgumentException("Tipo de contenido no permitido");
            }
        }
    }

    private static byte[] leerCabecera(MultipartFile file) throws IOException {
        try (var in = file.getInputStream()) {
            return in.readNBytes(12);
        }
    }

    private static void validarMagicBytes(String ext, byte[] header) {
        if (ext == null) {
            throw new IllegalArgumentException(MSG_VALIDACION_DOCUMENTO);
        }
        String extLower = ext.toLowerCase(Locale.ROOT);
        boolean jpeg = esJpeg(header);
        boolean png = esPng(header);
        boolean pdf = esPdf(header);

        if (!matchesMagic(extLower, jpeg, png, pdf)) {
            throw new IllegalArgumentException("El contenido del archivo no coincide con el formato declarado");
        }
        if (extLower.equals(".png") && !png) {
            throw new IllegalArgumentException(MSG_VALIDACION_DOCUMENTO);
        }
        if ((extLower.equals(".jpg") || extLower.equals(".jpeg")) && !jpeg) {
            throw new IllegalArgumentException(MSG_VALIDACION_DOCUMENTO);
        }
        if (extLower.equals(".pdf") && !pdf) {
            throw new IllegalArgumentException(MSG_VALIDACION_DOCUMENTO);
        }
    }

    private static boolean matchesMagic(String extLower, boolean jpeg, boolean png, boolean pdf) {
        return switch (extLower) {
            case ".jpg", ".jpeg" -> jpeg;
            case ".png" -> png;
            case ".pdf" -> pdf;
            default -> false;
        };
    }

    private static boolean esJpeg(byte[] header) {
        return header != null
                && header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
    }

    private static boolean esPng(byte[] header) {
        return header != null
                && header.length >= 8
                && header[0] == (byte) 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A;
    }

    private static boolean esPdf(byte[] header) {
        return header != null
                && header.length >= 4
                && header[0] == '%'
                && header[1] == 'P'
                && header[2] == 'D'
                && header[3] == 'F';
    }

    private static String extensionOf(String originalName) {
        if (originalName == null || !originalName.contains(".")) {
            return null;
        }
        return originalName.substring(originalName.lastIndexOf('.'));
    }
}
