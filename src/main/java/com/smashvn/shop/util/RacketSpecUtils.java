package com.smashvn.shop.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RacketSpecUtils {

    private static final BigDecimal POUNDS_PER_KILOGRAM = new BigDecimal("2.20462");
    private static final Pattern VALUE_WITH_UNIT = Pattern.compile("^([0-9]+(?:[\\.,][0-9]+)?)\\s*(kg|kgs|kilogram|kilograms|lb|lbs)?$", Pattern.CASE_INSENSITIVE);

    private RacketSpecUtils() {
    }

    public static String normalizeStringTensionToLbs(String mucCang) {
        if (mucCang == null) {
            return null;
        }

        String cleaned = mucCang.trim();
        if (cleaned.isEmpty()) {
            return null;
        }

        Matcher matcher = VALUE_WITH_UNIT.matcher(cleaned);
        if (!matcher.matches()) {
            if (cleaned.matches("(?i).*\\b(kg|kgs|kilogram|kilograms)\\b.*")) {
                throw new IllegalArgumentException("Mức căng phải được lưu theo đơn vị lbs.");
            }
            return cleaned.replaceAll("(?i)\\b(lb|lbs)\\b", "lbs");
        }

        BigDecimal value = new BigDecimal(matcher.group(1).replace(',', '.'));
        String unit = matcher.group(2);
        if (unit != null && unit.toLowerCase().startsWith("kg")) {
            value = value.multiply(POUNDS_PER_KILOGRAM);
        }

        BigDecimal rounded = value.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros();
        return rounded.toPlainString() + " lbs";
    }
}
