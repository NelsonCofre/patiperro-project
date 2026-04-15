package com.patiperro.reserva.support;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public final class BookingTokenExtractor {

    private BookingTokenExtractor() {}

    /** JWT sin prefijo Bearer. */
    public static Optional<String> extractRawJwt(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String t = header.substring(7).trim();
            if (!t.isEmpty()) {
                return Optional.of(t);
            }
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("access_token".equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                    return Optional.of(c.getValue().trim());
                }
            }
        }
        return Optional.empty();
    }
}
