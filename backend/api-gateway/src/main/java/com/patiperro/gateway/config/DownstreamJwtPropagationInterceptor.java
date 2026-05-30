package com.patiperro.gateway.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

/**
 * El proxy WebMVC del gateway no reenvía {@code Authorization} ni
 * {@code Cookie} a los microservicios.
 * Pagos (y otros) leen
 * {@link DuplicateAuthorizationForDownstreamFilter#HEADER_DOWNSTREAM_AUTH} y
 * cookie reenviada.
 * Este interceptor copia esos valores en cada salida HTTP del cliente del
 * gateway.
 */
@Component
public class DownstreamJwtPropagationInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest incoming = attrs.getRequest();
            String auth = incoming.getHeader(HttpHeaders.AUTHORIZATION);
            if (!StringUtils.hasText(auth)) {
                auth = incoming.getHeader(DuplicateAuthorizationForDownstreamFilter.HEADER_DOWNSTREAM_AUTH);
            }
            if (StringUtils.hasText(auth) && request.getHeaders()
                    .get(DuplicateAuthorizationForDownstreamFilter.HEADER_DOWNSTREAM_AUTH) == null) {
                request.getHeaders().add(DuplicateAuthorizationForDownstreamFilter.HEADER_DOWNSTREAM_AUTH, auth.trim());
            }

            String cookie = incoming.getHeader(HttpHeaders.COOKIE);
            if (!StringUtils.hasText(cookie)) {
                cookie = incoming.getHeader(DuplicateAuthorizationForDownstreamFilter.HEADER_DOWNSTREAM_COOKIE);
            }
            if (StringUtils.hasText(cookie)
                    && request.getHeaders()
                            .get(DuplicateAuthorizationForDownstreamFilter.HEADER_DOWNSTREAM_COOKIE) == null) {
                request.getHeaders().add(DuplicateAuthorizationForDownstreamFilter.HEADER_DOWNSTREAM_COOKIE,
                        cookie.trim());
            }
        }
        return execution.execute(request, body);
    }
}
