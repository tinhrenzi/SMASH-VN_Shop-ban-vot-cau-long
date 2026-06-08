package com.smashvn.shop.service;

import com.smashvn.shop.entity.ShippingZone;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ShippingZoneResolver {

    // Extensible list of local provinces (normalized, lowercase, accent-less)
    private static final Set<String> LOCAL_PROVINCES = Set.of(
            "thai nguyen",
            "ha noi"
    );

    public ShippingZone resolveZone(String address) {
        if (address == null || address.trim().isEmpty()) {
            return ShippingZone.NATIONWIDE;
        }

        String normalized = normalizeAddress(address);
        for (String province : LOCAL_PROVINCES) {
            if (normalized.contains(province)) {
                return ShippingZone.LOCAL;
            }
        }
        return ShippingZone.NATIONWIDE;
    }

    public String normalizeAddress(String address) {
        if (address == null) {
            return "";
        }

        // Convert to lowercase
        String normalized = address.toLowerCase();

        // Replace hyphens, commas, periods, colons, underscores, and question marks with spaces
        normalized = normalized.replaceAll("[\\-\\.,;:_\\?]", " ");

        // Remove Vietnamese accents / diacritics
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        Pattern diacriticsPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        normalized = diacriticsPattern.matcher(normalized).replaceAll("");

        // Replace specific characters (like đ -> d)
        normalized = normalized.replace("đ", "d");

        // Trim multiple spaces into a single space
        normalized = normalized.replaceAll("\\s+", " ").trim();

        return normalized;
    }
}
