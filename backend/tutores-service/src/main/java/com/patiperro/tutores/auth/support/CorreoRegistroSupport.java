package com.patiperro.tutores.auth.support;

import java.util.Locale;

public final class CorreoRegistroSupport {

    private CorreoRegistroSupport() {
    }

    public static String normalizar(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException("El correo es obligatorio");
        }
        return correo.trim().toLowerCase(Locale.ROOT);
    }
}
