package com.patiperro.tutores.geo;

import com.patiperro.tutores.user.model.Direccion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NominatimGeocodingService {

    private static final Pattern PAT_LAT = Pattern.compile("\"lat\"\\s*:\\s*\"(-?[0-9]+(?:\\.[0-9]+)?)\"");
    private static final Pattern PAT_LON = Pattern.compile("\"lon\"\\s*:\\s*\"(-?[0-9]+(?:\\.[0-9]+)?)\"");

    private final GeocodingProperties properties;

    public void tryEnrich(Direccion direccion) {
        if (!properties.isEnabled()) {
            return;
        }
        if (direccion.getLatitud() != null && direccion.getLongitud() != null) {
            return;
        }
        String q = buildSearchQuery(direccion);
        if (q.isBlank()) {
            log.debug("Geocoding omitido: direccion sin datos suficientes");
            return;
        }
        try {
            String encoded = URLEncoder.encode(q, StandardCharsets.UTF_8);
            String base = properties.getNominatimBaseUrl().replaceAll("/$", "");
            String url = base + "/search?q=" + encoded + "&format=json&limit=1";

            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", properties.getUserAgent())
                    .header("Accept-Language", "es")
                    .GET()
                    .timeout(Duration.ofSeconds(12))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Nominatim HTTP {} para query {}", response.statusCode(), q);
                return;
            }
            Optional<double[]> coords = parseFirstLatLonFromNominatimJson(response.body());
            if (coords.isEmpty()) {
                log.debug("Nominatim sin resultados parseables para: {}", q);
                return;
            }
            double[] ll = coords.get();
            direccion.setLatitud(ll[0]);
            direccion.setLongitud(ll[1]);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Geocoding interrumpido: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Geocoding fallido (se guarda direccion sin coords): {}", e.getMessage());
        }
    }

    static Optional<double[]> parseFirstLatLonFromNominatimJson(String body) {
        if (body == null || body.isBlank()) {
            return Optional.empty();
        }
        Matcher mLat = PAT_LAT.matcher(body);
        Matcher mLon = PAT_LON.matcher(body);
        if (!mLat.find() || !mLon.find()) {
            return Optional.empty();
        }
        try {
            double lat = Double.parseDouble(mLat.group(1));
            double lon = Double.parseDouble(mLon.group(1));
            return Optional.of(new double[] {lat, lon});
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static String buildSearchQuery(Direccion d) {
        List<String> parts = new ArrayList<>();
        if (d.getCalle() != null && !d.getCalle().isBlank()) {
            String calle = d.getCalle().trim();
            if (d.getNumeracion() != null) {
                calle = d.getNumeracion() + " " + calle;
            }
            parts.add(calle);
        }
        if (d.getComuna() != null && !d.getComuna().isBlank()) {
            parts.add(d.getComuna().trim());
        }
        if (d.getCiudad() != null && !d.getCiudad().isBlank()) {
            parts.add(d.getCiudad().trim());
        }
        if (d.getRegion() != null && !d.getRegion().isBlank()) {
            parts.add(d.getRegion().trim());
        }
        if (d.getPais() != null && !d.getPais().isBlank()) {
            parts.add(d.getPais().trim());
        }
        return parts.stream().collect(Collectors.joining(", "));
    }
}
