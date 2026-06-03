package com.smashvn.shop.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class LocationService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Cacheable(value = "ipLocationCache", key = "#ip")
    public Map<String, Object> getIpLocation(String ip) {
        System.out.println("[LocationService] Fetching location from external provider for IP: " + ip);
        Map<String, Object> result = new HashMap<>();
        result.put("ip", ip);

        // Try ipapi.co first
        try {
            HttpRequest ipapiReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://ipapi.co/" + ip + "/json/"))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(Duration.ofSeconds(4))
                    .build();
            HttpResponse<String> response = httpClient.send(ipapiReq, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.has("latitude") && root.has("longitude")) {
                    double lat = root.get("latitude").asDouble();
                    double lon = root.get("longitude").asDouble();
                    
                    if (isValidCoordinate(lat, lon)) {
                        result.put("latitude", lat);
                        result.put("longitude", lon);
                        result.put("city", root.has("city") ? root.get("city").asText() : "");
                        result.put("region", root.has("region") ? root.get("region").asText() : "");
                        result.put("country", root.has("country_name") ? root.get("country_name").asText() : "Vietnam");
                        result.put("source", "ipapi.co");
                        return result;
                    } else {
                        System.err.println("[LocationService] ipapi.co returned invalid coordinates: lat=" + lat + ", lon=" + lon);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[LocationService] Failed to fetch from ipapi.co: " + e.getMessage());
        }

        // Try ipinfo.io as fallback
        try {
            HttpRequest ipinfoReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://ipinfo.io/" + ip + "/json"))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(Duration.ofSeconds(4))
                    .build();
            HttpResponse<String> response = httpClient.send(ipinfoReq, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.has("loc")) {
                    String loc = root.get("loc").asText();
                    String[] latLon = loc.split(",");
                    if (latLon.length == 2) {
                        double lat = Double.parseDouble(latLon[0].trim());
                        double lon = Double.parseDouble(latLon[1].trim());
                        
                        if (isValidCoordinate(lat, lon)) {
                            result.put("latitude", lat);
                            result.put("longitude", lon);
                            result.put("city", root.has("city") ? root.get("city").asText() : "");
                            result.put("region", root.has("region") ? root.get("region").asText() : "");
                            result.put("country", root.has("country") ? root.get("country").asText() : "Vietnam");
                            result.put("source", "ipinfo.io");
                            return result;
                        } else {
                            System.err.println("[LocationService] ipinfo.io returned invalid coordinates: lat=" + lat + ", lon=" + lon);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[LocationService] Failed to fetch from ipinfo.io: " + e.getMessage());
        }

        throw new RuntimeException("Unable to determine location from IP: " + ip);
    }

    @Cacheable(value = "geocodeSearchCache", key = "#query")
    public Map<String, Object> searchAddress(String query) {
        System.out.println("[LocationService] Searching coordinates for address: " + query);
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Encode query string
            String encodedQuery = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            
            // Search Nominatim with countrycodes=vn and limit=1
            HttpRequest searchReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://nominatim.openstreetmap.org/search?format=json&q=" + encodedQuery + "&countrycodes=vn&limit=1"))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(Duration.ofSeconds(4))
                    .build();
            HttpResponse<String> response = httpClient.send(searchReq, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.isArray() && root.size() > 0) {
                    JsonNode firstResult = root.get(0);
                    if (firstResult.has("lat") && firstResult.has("lon")) {
                        double lat = Double.parseDouble(firstResult.get("lat").asText());
                        double lon = Double.parseDouble(firstResult.get("lon").asText());
                        
                        if (isValidCoordinate(lat, lon)) {
                            result.put("latitude", lat);
                            result.put("longitude", lon);
                            result.put("display_name", firstResult.has("display_name") ? firstResult.get("display_name").asText() : "");
                            return result;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[LocationService] Failed to search address coordinates: " + e.getMessage());
        }
        
        throw new RuntimeException("Unable to geocode address: " + query);
    }

    private boolean isValidCoordinate(double latitude, double longitude) {
        return latitude >= -90.0 && latitude <= 90.0 && longitude >= -180.0 && longitude <= 180.0;
    }
}
