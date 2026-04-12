package com.patiperro.paseador.geo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Geocodificacion Nominatim (OSM); politica de uso: max 1 req/s en produccion. */
@Getter
@Setter
@ConfigurationProperties(prefix = "patiperro.geocoding")
public class GeocodingProperties {

    private boolean enabled = true;
    private String nominatimBaseUrl = "https://nominatim.openstreetmap.org";
    /** Obligatorio para Nominatim: identificar la app (email de contacto recomendado en prod). */
    private String userAgent = "PatiperroApp/1.0 (desarrollo)";
}
