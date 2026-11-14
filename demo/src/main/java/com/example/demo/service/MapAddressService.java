package com.example.demo.service;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.demo.dto.MapAddressDTO;
import com.example.demo.errors.ApiException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Service
public class MapAddressService {

    private final RestClient http;
    private final String baseUrl;

    private final Map<String, Cached> cache = new LinkedHashMap<>();

    private long nextRequestAt = System.nanoTime();
    private long blockedUntil = 0;

    public MapAddressService(
            @Value("${terra.geocoding.base-url:https://nominatim.openstreetmap.org}") String baseUrl) {

        this.baseUrl = baseUrl.replaceAll("/+$", "");

        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(4));
        factory.setReadTimeout(Duration.ofSeconds(8));

        this.http = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(
                        "User-Agent",
                        "TerraGalega/1.0 (checkout address picker)")
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Accept-Language", "es")
                .build();
    }

    // Busca la dirección escrita para centrar el mapa al abrirlo.
    public MapAddressDTO search(String query) {
        if (query == null || query.isBlank() || query.length() > 160) {
            throw ApiException.badRequest(
                    "Escribe una dirección de hasta 160 caracteres.");
        }

        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/search")
                .queryParam("format", "jsonv2")
                .queryParam("addressdetails", 1)
                .queryParam("limit", 1)
                .queryParam("countrycodes", "co")
                .queryParam("q", query.trim())
                .build()
                .encode()
                .toUri();

        Result[] results = request(uri, Result[].class);

        return results.length == 0 ? null : toDTO(results[0]);
    }

    // Convierte el punto elegido en una dirección.
    public MapAddressDTO reverse(double lat, double lon) {
        if (!Double.isFinite(lat) || !Double.isFinite(lon)
                || lat < -90 || lat > 90
                || lon < -180 || lon > 180) {

            throw ApiException.badRequest(
                    "Las coordenadas no son válidas.");
        }

        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/reverse")
                .queryParam("format", "jsonv2")
                .queryParam("addressdetails", 1)
                .queryParam("zoom", 18)
                .queryParam("lat", String.format(Locale.ROOT, "%.6f", lat))
                .queryParam("lon", String.format(Locale.ROOT, "%.6f", lon))
                .build()
                .encode()
                .toUri();

        return toDTO(request(uri, Result.class));
    }

    // Una consulta externa a la vez en esta instancia.
    // Reutiliza respuestas durante 30 minutos.
    private synchronized <T> T request(URI uri, Class<T> type) {
        String key = uri.toString();
        Cached previous = cache.get(key);
        long now = System.currentTimeMillis();

        if (previous != null && previous.expiresAt() > now) {
            return type.cast(previous.value());
        }

        if (now < blockedUntil) {
            throw unavailable();
        }

        try {
            long wait = nextRequestAt - System.nanoTime();

            if (wait > 0) {
                TimeUnit.NANOSECONDS.sleep(wait);
            }

            nextRequestAt = System.nanoTime()
                    + TimeUnit.MILLISECONDS.toNanos(1100);

            T value = http.get()
                    .uri(uri)
                    .retrieve()
                    .body(type);

            if (value == null) {
                throw unavailable();
            }

            if (cache.size() >= 256) {
                cache.remove(cache.keySet().iterator().next());
            }

            cache.put(
                    key,
                    new Cached(
                            value,
                            System.currentTimeMillis() + 1_800_000));

            return value;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable();

        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw ApiException.notFound(
                        "No se encontró una dirección para ese punto.");
            }

            if (e.getStatusCode().value() == 429
                    || e.getStatusCode().value() == 403) {
                blockedUntil = System.currentTimeMillis() + 60_000;
            }

            throw unavailable();

        } catch (RestClientException e) {
            throw unavailable();
        }
    }

    private MapAddressDTO toDTO(Result result) {
        if (result.error() != null
                || result.lat() == null
                || result.lon() == null) {

            throw ApiException.notFound(
                    "No se encontró una dirección para ese punto.");
        }

        Map<String, String> address = result.address() == null ? Map.of() : result.address();

        String road = first(
                address,
                "road",
                "pedestrian",
                "residential",
                "footway");

        String label;

        if (road != null) {
            String number = address.get("house_number");

            var parts = new LinkedHashSet<String>();
            parts.add(road + (number == null ? "" : " #" + number));

            String area = first(
                    address,
                    "neighbourhood",
                    "suburb");

            String city = first(
                    address,
                    "city",
                    "town",
                    "village",
                    "municipality");

            if (area != null) {
                parts.add(area);
            }

            if (city != null) {
                parts.add(city);
            }

            label = String.join(", ", parts);
        } else {
            label = result.display_name();
        }

        if (label == null || label.isBlank()) {
            throw ApiException.notFound(
                    "No se encontró una dirección para ese punto.");
        }

        try {
            return new MapAddressDTO(
                    label,
                    Double.parseDouble(result.lat()),
                    Double.parseDouble(result.lon()));
        } catch (NumberFormatException e) {
            throw unavailable();
        }
    }

    private String first(Map<String, String> values, String... keys) {
        for (String key : keys) {
            String value = values.get(key);

            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }

    private ApiException unavailable() {
        return ApiException.serviceUnavailable(
                "No se pudo consultar la dirección. Intenta de nuevo o escríbela.");
    }

    private record Cached(Object value, long expiresAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            String lat,
            String lon,
            String display_name,
            Map<String, String> address,
            String error) {
    }
}