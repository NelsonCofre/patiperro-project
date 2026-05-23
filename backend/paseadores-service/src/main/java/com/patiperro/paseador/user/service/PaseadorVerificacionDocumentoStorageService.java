package com.patiperro.paseador.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class PaseadorVerificacionDocumentoStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png", ".pdf");
    private static final Set<String> ALLOWED_MIME = Set.of(
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
        this.uploadDir = Paths.get(uploadDirProperty).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    /** Guarda un único PDF de verificación de identidad. */
    public String savePdf(MultipartFile file) throws IOException {
        validatePdfUpload(file);
        return persistFile(file);
    }

    public String save(MultipartFile file) throws IOException {
        validateUpload(file);
        return persistFile(file);
    }

    private String persistFile(MultipartFile file) throws IOException {
        Files.createDirectories(uploadDir);
        String ext = extensionOf(file.getOriginalFilename());
        String filename = UUID.randomUUID() + ext.toLowerCase(Locale.ROOT);
        Path target = uploadDir.resolve(filename).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Nombre de archivo inválido");
        }
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return filename;
    }

    private void validatePdfUpload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Archivo vacío");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo permitido (5 MB)");
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (ext == null || !".pdf".equals(ext.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Formato no permitido (solo PDF)");
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String normalized = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
            if (!"application/pdf".equals(normalized)) {
                throw new IllegalArgumentException("Tipo de contenido no permitido (solo PDF)");
            }
        }
        byte[] header = readHeader(file, 8);
        if (!matchesMagic(header, ".pdf")) {
            throw new IllegalArgumentException("El contenido del archivo no coincide con un PDF válido");
        }
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

    public void deleteQuietly(String filename) {
        if (filename == null || filename.isBlank()) {
            return;
        }
        Path path = resolveExisting(filename);
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Limpieza best-effort tras rollback o reemplazo.
        }
    }

    private void validateUpload(MultipartFile file) throws IOException {
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
            if (!ALLOWED_MIME.contains(normalized)) {
                throw new IllegalArgumentException("Tipo de contenido no permitido");
            }
        }
        byte[] header = readHeader(file, 8);
        if (!matchesMagic(header, ext.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("El contenido del archivo no coincide con el formato declarado");
        }
    }

    private static byte[] readHeader(MultipartFile file, int len) throws IOException {
        byte[] buf = new byte[len];
        try (InputStream in = file.getInputStream()) {
            int read = in.read(buf);
            if (read <= 0) {
                return new byte[0];
            }
            if (read < len) {
                byte[] trimmed = new byte[read];
                System.arraycopy(buf, 0, trimmed, 0, read);
                return trimmed;
            }
        }
        return buf;
    }

    private static boolean matchesMagic(byte[] header, String ext) {
        if (header == null || header.length < 3) {
            return false;
        }
        return switch (ext) {
            case ".jpg", ".jpeg" -> (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
            case ".png" -> header.length >= 4
                    && header[0] == (byte) 0x89
                    && header[1] == 0x50
                    && header[2] == 0x4E
                    && header[3] == 0x47;
            case ".pdf" -> header.length >= 4
                    && header[0] == '%'
                    && header[1] == 'P'
                    && header[2] == 'D'
                    && header[3] == 'F';
            default -> false;
        };
    }

    private static String extensionOf(String originalName) {
        if (originalName == null || !originalName.contains(".")) {
            return null;
        }
        return originalName.substring(originalName.lastIndexOf('.'));
    }
}
