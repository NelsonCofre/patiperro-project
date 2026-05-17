package com.patiperro.reserva.support;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/**
 * Extrae JWT de peticiones de reserva (cookie, Authorization o cabeceras reenviadas por api-gateway).
 */
public final class BookingTokenExtractor {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_DOWNSTREAM_AUTH = "X-Patiperro-Authorization";
    private static final String HEADER_DOWNSTREAM_COOKIE = "X-Patiperro-Forwarded-Cookie";
    private static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";

    private BookingTokenExtractor() {}

    /** JWT sin prefijo Bearer. */
    public static Optional<String> extractRawJwt(HttpServletRequest request) {
        Optional<String> token = extractBearerToken(request.getHeader(HEADER_AUTHORIZATION));
        if (token.isPresent()) {
            return token;
        }

        token = extractBearerToken(request.getHeader(HEADER_DOWNSTREAM_AUTH));
        if (token.isPresent()) {
            return token;
        }

        token = extractAccessTokenFromCookies(request.getCookies());
        if (token.isPresent()) {
            return token;
        }

        return extractAccessTokenFromCookieHeader(request.getHeader(HEADER_DOWNSTREAM_COOKIE));
    }

    private static Optional<String> extractBearerToken(String headerValue) {
        if (headerValue != null && headerValue.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String raw = headerValue.substring(7).trim();
            if (!raw.isEmpty()) {
                return Optional.of(raw);
            }
        }
        return Optional.empty();
    }

    private static Optional<String> extractAccessTokenFromCookies(Cookie[] cookies) {
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName())
                    && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue().trim());
            }
        }
        return Optional.empty();
    }

    private static Optional<String> extractAccessTokenFromCookieHeader(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return Optional.empty();
        }
        for (String fragment : cookieHeader.split(";")) {
            String[] pair = fragment.trim().split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            String name = pair[0].trim();
            String value = pair[1].trim();
            if (ACCESS_TOKEN_COOKIE_NAME.equals(name) && !value.isBlank()) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
