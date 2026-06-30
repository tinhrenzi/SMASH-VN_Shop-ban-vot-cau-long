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

    public String filter(String input, List<String> customKeywords) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        // 1. Filter hardcoded bad words first
        String result = filter(input);

        // 2. Filter custom keywords
        if (customKeywords != null && !customKeywords.isEmpty()) {
            for (String kw : customKeywords) {
                if (kw == null) {
                    continue;
                }
                String trimmedKw = kw.trim();
                if (trimmedKw.isEmpty() || trimmedKw.length() < 2) {
                    continue;
                }

                // Replace ignoring case safely
                Pattern pattern = Pattern.compile(Pattern.quote(trimmedKw), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
                result = pattern.matcher(result).replaceAll("***");
            }
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

    public SeverityLevel getSeverity(String input, List<String> customKeywords) {
        if (input == null || input.trim().isEmpty()) {
            return SeverityLevel.NONE;
        }

        // 1. Get severity from hardcoded patterns
        SeverityLevel hardcodedSeverity = getSeverity(input);

        // 2. Check custom keywords
        SeverityLevel customSeverity = SeverityLevel.NONE;
        if (customKeywords != null && !customKeywords.isEmpty()) {
            String normalizedInput = normalizeAndCollapse(input);
            String paddedInput = " " + normalizedInput + " ";
            String rawLowerInput = input.toLowerCase();

            for (String kw : customKeywords) {
                if (kw == null) {
                    continue;
                }
                String trimmedKw = kw.trim();
                if (trimmedKw.isEmpty()) {
                    continue;
                }

                boolean matched = false;

                // 2a. Raw lowercase check (handles c++, a+b, test.com directly if raw length >= 2)
                if (trimmedKw.length() >= 2) {
                    if (rawLowerInput.contains(trimmedKw.toLowerCase())) {
                        matched = true;
                    }
                }

                // 2b. Normalized word boundary check (handles d.m, obfuscation, accents if normalized length >= 2)
                if (!matched) {
                    String normalizedKw = normalizeAndCollapse(trimmedKw);
                    if (normalizedKw.length() >= 2) {
                        String paddedKw = " " + normalizedKw + " ";
                        if (paddedInput.contains(paddedKw)) {
                            matched = true;
                        }
                    }
                }

                if (matched) {
                    customSeverity = SeverityLevel.MEDIUM;
                    break;
                }
            }
        }

        // Return higher severity
        if (hardcodedSeverity.ordinal() > customSeverity.ordinal()) {
            return hardcodedSeverity;
        }
        return customSeverity;
    }

    private boolean matchesAny(String input, Pattern[] patterns) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(input).find()) {
                return true;
            }
        }
        return false;
    }

    private String normalizeAndCollapse(String input) {
        if (input == null) {
            return "";
        }
        // 1. Replace đ/Đ first
        String temp = input.toLowerCase();
        temp = temp.replace('đ', 'd').replace('Đ', 'd');

        // 2. Accent normalization
        temp = java.text.Normalizer.normalize(temp, java.text.Normalizer.Form.NFD);
        temp = temp.replaceAll("\\p{M}+", "");

        // 3. Remove non-alphanumeric except spaces
        temp = temp.replaceAll("[^a-z0-9\\s]", " ");

        // 4. Collapse single characters
        String[] words = temp.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        int prevWordLen = 0;
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (word.length() == 1) {
                if (prevWordLen == 1) {
                    sb.append(word);
                } else {
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append(word);
                }
                prevWordLen = 1;
            } else {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(word);
                prevWordLen = word.length();
            }
        }
        return sb.toString();
    }
}
