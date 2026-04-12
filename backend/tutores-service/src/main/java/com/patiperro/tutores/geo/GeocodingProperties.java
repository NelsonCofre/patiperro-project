package com.patiperro.tutores.geo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "patiperro.geocoding")
public class GeocodingProperties {

    private boolean enabled = true;
    private String nominatimBaseUrl = "https://nominatim.openstreetmap.org";
    private String userAgent = "PatiperroApp/1.0 (desarrollo)";
}
