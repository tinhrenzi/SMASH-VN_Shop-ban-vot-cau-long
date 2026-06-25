package com.smashvn.shop.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ProfanityFilter {
    
    private static final Pattern[] LOW_PATTERNS = {
        Pattern.compile("(?U)\\bv[ck]l+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\bvl+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\bwtf\\b", Pattern.CASE_INSENSITIVE)
    };

    private static final Pattern[] MEDIUM_PATTERNS = {
        Pattern.compile("(?U)\\b[d\u0111][e\u00e9]o+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\bc[u\u01b0]t+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\bc[a\u0103\u00e2]c+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\bb[u\u01b0]\u00f4?[i\u00ec]+\\b", Pattern.CASE_INSENSITIVE)
    };

    private static final Pattern[] HIGH_PATTERNS = {
        Pattern.compile("(?U)\\b[d\u0111][\\s\\.\\-_]*m+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\b[d\u0111][\\s\\.\\-_]*c[\\s\\.\\-_]*m+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\bl[o\u00f2\u00f4]n+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\b\u0111\u1ed3\\s+ch\u00f3\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\bch\u00f3\\s+\u0111\u1ebb\\b", Pattern.CASE_INSENSITIVE)
    };

    private static final Pattern[] CRITICAL_PATTERNS = {
        Pattern.compile("(?U)\\bgi\u1ebft\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\bhi\u1ebfp\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\b\u0111e\\s+d\u1ecd\u0061\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\bl\u1eeba\\s+\u0111\u1ea3o\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\b\u0103n\\s+c\u01b0\u1edbp\\b", Pattern.CASE_INSENSITIVE)
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
