package com.patiperro.mascota.security;

import com.patiperro.mascota.exception.ForbiddenOperationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class TutorSecurity {

    private TutorSecurity() {
    }

    public static TutorPrincipal requireTutor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof TutorPrincipal tp)) {
            throw new ForbiddenOperationException("Sesión de tutor requerida");
        }
        if (tp.tutorId() == null) {
            throw new ForbiddenOperationException(
                    "El token no incluye id de tutor; cierre sesión y vuelva a iniciar sesión.");
        }
        return tp;
    }
}
