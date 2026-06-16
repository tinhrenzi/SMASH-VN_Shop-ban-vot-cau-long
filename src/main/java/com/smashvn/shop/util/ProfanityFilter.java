package com.smashvn.shop.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ProfanityFilter {
    
    private static final Pattern[] LOW_PATTERNS = {
        Pattern.compile("\\bv[ck]l+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bvl+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bwtf\\b", Pattern.CASE_INSENSITIVE)
    };

    private static final Pattern[] MEDIUM_PATTERNS = {
        Pattern.compile("\\b[dđ][eé]o+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bc[uư]t+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bc[aăâ]c+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bb[uư]ô?[iì]+\\b", Pattern.CASE_INSENSITIVE)
    };

    private static final Pattern[] HIGH_PATTERNS = {
        Pattern.compile("\\b[dđ][\\s\\.\\-_]*m+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b[dđ][\\s\\.\\-_]*c[\\s\\.\\-_]*m+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bl[oòô]n+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bđồ\\s+chó\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bchó\\s+đẻ\\b", Pattern.CASE_INSENSITIVE)
    };

    private static final Pattern[] CRITICAL_PATTERNS = {
        Pattern.compile("\\bgiết\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bhiếp\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bđe\\s+dọa\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\blừa\\s+đảo\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\băn\\s+cướp\\b", Pattern.CASE_INSENSITIVE)
    };

    private static final Pattern[] BAD_WORDS_PATTERNS;
    static {
        List<Pattern> all = new ArrayList<>();
        all.addAll(Arrays.asList(CRITICAL_PATTERNS));
        all.addAll(Arrays.asList(HIGH_PATTERNS));
        all.addAll(Arrays.asList(MEDIUM_PATTERNS));
        all.addAll(Arrays.asList(LOW_PATTERNS));
        BAD_WORDS_PATTERNS = all.toArray(new Pattern[0]);
    }

    public String filter(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        
        String result = input;
        for (Pattern pattern : BAD_WORDS_PATTERNS) {
            result = pattern.matcher(result).replaceAll("***");
        }
        return result;
    }

    public SeverityLevel getSeverity(String input) {
        if (input == null || input.trim().isEmpty()) {
            return SeverityLevel.NONE;
        }
        if (matchesAny(input, CRITICAL_PATTERNS)) {
            return SeverityLevel.CRITICAL;
        }
        if (matchesAny(input, HIGH_PATTERNS)) {
            return SeverityLevel.HIGH;
        }
        if (matchesAny(input, MEDIUM_PATTERNS)) {
            return SeverityLevel.MEDIUM;
        }
        if (matchesAny(input, LOW_PATTERNS)) {
            return SeverityLevel.LOW;
        }
        return SeverityLevel.NONE;
    }

    private boolean matchesAny(String input, Pattern[] patterns) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(input).find()) {
                return true;
            }
        }
        return false;
    }
}
