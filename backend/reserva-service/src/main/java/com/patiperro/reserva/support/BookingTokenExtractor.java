package com.patiperro.reserva.support;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public final class BookingTokenExtractor {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_DOWNSTREAM_AUTH = "X-Patiperro-Authorization";
    private static final String HEADER_DOWNSTREAM_COOKIE = "X-Patiperro-Forwarded-Cookie";
    private static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";

    private BookingTokenExtractor() {}

    /** JWT sin prefijo Bearer. */
    public static Optional<String> extractRawJwt(HttpServletRequest request) {
        Optional<String> fromAuthorization = extractBearerToken(request.getHeader(HEADER_AUTHORIZATION));
        if (fromAuthorization.isPresent()) {
            return fromAuthorization;
        }

        Optional<String> fromCookies = extractAccessTokenFromCookies(request.getCookies());
        if (fromCookies.isPresent()) {
            return fromCookies;
        }

        Optional<String> fromForwardedAuthorization = extractBearerToken(request.getHeader(HEADER_DOWNSTREAM_AUTH));
        if (fromForwardedAuthorization.isPresent()) {
            return fromForwardedAuthorization;
        }

        Optional<String> fromForwardedCookie = extractAccessTokenFromCookieHeader(
                request.getHeader(HEADER_DOWNSTREAM_COOKIE));
        if (fromForwardedCookie.isPresent()) {
            return fromForwardedCookie;
        }

        return Optional.empty();
    }

    private static Optional<String> extractBearerToken(String headerValue) {
        if (headerValue != null && headerValue.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = headerValue.substring(7).trim();
            if (!token.isEmpty()) {
                return Optional.of(token);
            }
        }
        return Optional.empty();
    }

    private static Optional<String> extractAccessTokenFromCookies(Cookie[] cookies) {
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (ACCESS_TOKEN_COOKIE_NAME.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                    return Optional.of(c.getValue().trim());
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> extractAccessTokenFromCookieHeader(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return Optional.empty();
        }
        String[] fragments = cookieHeader.split(";");
        for (String fragment : fragments) {
            String[] pair = fragment.split("=", 2);
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
