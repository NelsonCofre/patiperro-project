package com.patiperro.paseador.user.service;

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

@Service
public class PaseadorPerfilFotoStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    private final Path uploadDir = Paths.get("uploads/paseador-perfil").toAbsolutePath().normalize();
    private final long maxFileSizeBytes;

    public PaseadorPerfilFotoStorageService(
            @Value("${patiperro.paseadores.perfil-foto.max-file-size-bytes:10485760}") long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public String save(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Archivo vacío");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("La foto supera el tamaño máximo permitido (10 MB)");
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (ext == null || !ALLOWED_EXT.contains(ext.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Formato no permitido (usa jpg, png, gif o webp)");
        }

        Files.createDirectories(uploadDir);
        String filename = UUID.randomUUID() + ext.toLowerCase(Locale.ROOT);
        Path target = uploadDir.resolve(filename).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Nombre inválido");
        }
        try (var in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return filename;
    }

    public Path resolveExisting(String filename) {
        if (!filename.matches("^[a-fA-F0-9\\-]{36}\\.(jpg|jpeg|png|gif|webp)$")) {
            return null;
        }
        Path target = uploadDir.resolve(filename).normalize();
        if (!target.startsWith(uploadDir) || !Files.isRegularFile(target)) {
            return null;
        }
        return target;
    }

    public static final String PUBLIC_URL_PREFIX = "/api/paseadores/public/perfil/";

    public String buildPublicUrl(String filename) {
        return PUBLIC_URL_PREFIX + filename;
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
            // Best effort: no interrumpir flujo principal.
        }
    }

    public String extractFilenameFromPublicUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();
        int idx = trimmed.lastIndexOf(PUBLIC_URL_PREFIX);
        if (idx >= 0) {
            return trimmed.substring(idx + PUBLIC_URL_PREFIX.length());
        }
        if (trimmed.matches("^[a-fA-F0-9\\-]{36}\\.(jpg|jpeg|png|gif|webp)$")) {
            return trimmed;
        }
        return null;
    }

    private static String extensionOf(String originalName) {
        if (originalName == null || !originalName.contains(".")) {
            return null;
        }
        return originalName.substring(originalName.lastIndexOf('.'));
    }
}
