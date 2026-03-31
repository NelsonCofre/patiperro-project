package com.patiperro.mascota.security;

/**
 * Principal del tutor tras validar el JWT emitido por tutores-service
 * (subject = correo, tutorId = claim tutorId).
 */
public record TutorPrincipal(String correo, Long tutorId) {
}
