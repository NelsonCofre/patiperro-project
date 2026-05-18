package com.patiperro.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ConfigurationProperties(prefix = "patiperro.gateway.cors")
public class GatewayCorsProperties {

    private List<String> allowedOrigins = new ArrayList<>(List.of(
            "http://localhost:5173",
            "http://localhost:5174",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:5174"));

    /**
     * Orígenes extra (CSV), p. ej. túnel HTTPS del front: {@code GATEWAY_EXTRA_CORS_ORIGINS}.
     */
    private String extraAllowedOriginsCsv = "";

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins != null ? allowedOrigins : new ArrayList<>();
    }

    public String getExtraAllowedOriginsCsv() {
        return extraAllowedOriginsCsv;
    }

    public void setExtraAllowedOriginsCsv(String extraAllowedOriginsCsv) {
        this.extraAllowedOriginsCsv = extraAllowedOriginsCsv == null ? "" : extraAllowedOriginsCsv.trim();
    }

    /** Lista base + orígenes del CSV, sin duplicados, orden estable. */
    public List<String> resolvedAllowedOrigins() {
        Set<String> merged = new LinkedHashSet<>();
        for (String o : allowedOrigins) {
            if (o != null && !o.isBlank()) {
                merged.add(o.trim());
            }
        }
        if (!extraAllowedOriginsCsv.isBlank()) {
            for (String part : extraAllowedOriginsCsv.split(",")) {
                String t = part.trim();
                if (!t.isEmpty()) {
                    merged.add(t);
                }
            }
        }
        return List.copyOf(merged);
    }
}
