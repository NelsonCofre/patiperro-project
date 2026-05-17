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
        String fromForwardedCookie = extractAccessTokenFromCookieHeader(
                request.getHeader("X-Patiperro-Forwarded-Cookie"));
        if (fromForwardedCookie != null) {
            return Optional.of(fromForwardedCookie);
        }
        String xAuth = request.getHeader("X-Patiperro-Authorization");
        if (xAuth != null && xAuth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String t = xAuth.substring(7).trim();
            if (!t.isEmpty()) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    private static String extractAccessTokenFromCookieHeader(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return null;
        }
        for (String part : cookieHeader.split(";")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && "access_token".equals(kv[0].trim())) {
                String v = kv[1].trim();
                if (!v.isEmpty()) {
                    return v;
                }
            }
        }
        return null;
    }
}
