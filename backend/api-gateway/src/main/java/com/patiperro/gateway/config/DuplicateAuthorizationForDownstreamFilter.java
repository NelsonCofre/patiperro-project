package com.patiperro.gateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Spring Cloud Gateway WebMVC no reenvía {@code Authorization} ni {@code Cookie} a los microservicios (headers
 * "sensibles"). Duplicamos en headers {@value #HEADER_DOWNSTREAM_AUTH} y {@value #HEADER_DOWNSTREAM_COOKIE}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 200)
public class DuplicateAuthorizationForDownstreamFilter extends OncePerRequestFilter {

    public static final String HEADER_DOWNSTREAM_AUTH = "X-Patiperro-Authorization";
    public static final String HEADER_DOWNSTREAM_COOKIE = "X-Patiperro-Forwarded-Cookie";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        String cookieHeader = request.getHeader("Cookie");
        boolean hasBearer =
                StringUtils.hasText(auth) && auth.regionMatches(true, 0, "Bearer ", 0, 7);
        boolean hasCookie = StringUtils.hasText(cookieHeader);
        if (!hasBearer && !hasCookie) {
            filterChain.doFilter(request, response);
            return;
        }
        final String bearerValue = hasBearer ? auth.trim() : null;
        final String cookieValue = hasCookie ? cookieHeader.trim() : null;
        filterChain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if (HEADER_DOWNSTREAM_AUTH.equalsIgnoreCase(name) && bearerValue != null) {
                    return bearerValue;
                }
                if (HEADER_DOWNSTREAM_COOKIE.equalsIgnoreCase(name) && cookieValue != null) {
                    return cookieValue;
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if (HEADER_DOWNSTREAM_AUTH.equalsIgnoreCase(name) && bearerValue != null) {
                    return Collections.enumeration(List.of(bearerValue));
                }
                if (HEADER_DOWNSTREAM_COOKIE.equalsIgnoreCase(name) && cookieValue != null) {
                    return Collections.enumeration(List.of(cookieValue));
                }
                return super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = new ArrayList<>();
                Enumeration<String> e = super.getHeaderNames();
                while (e.hasMoreElements()) {
                    names.add(e.nextElement());
                }
                if (bearerValue != null
                        && names.stream().noneMatch(HEADER_DOWNSTREAM_AUTH::equalsIgnoreCase)) {
                    names.add(HEADER_DOWNSTREAM_AUTH);
                }
                if (cookieValue != null
                        && names.stream().noneMatch(HEADER_DOWNSTREAM_COOKIE::equalsIgnoreCase)) {
                    names.add(HEADER_DOWNSTREAM_COOKIE);
                }
                return Collections.enumeration(names);
            }
        }, response);
    }
}
