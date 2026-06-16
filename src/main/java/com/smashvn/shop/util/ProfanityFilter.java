package com.smashvn.shop.util;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ProfanityFilter {
    
    // Regex patterns tương ứng với các từ tục tĩu và các biến thể dấu chấm, dấu cách, dấu gạch ngang
    private static final Pattern[] BAD_WORDS_PATTERNS = {
        Pattern.compile("\\b[dđ][\\s\\.\\-_]*m+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b[dđ][\\s\\.\\-_]*c[\\s\\.\\-_]*m+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b[dđ][eé]o+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bl[oòô]n+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bc[uư]t+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bb[uư]ô?[iì]+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bc[aăâ]c+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bv[ck]l+\\b", Pattern.CASE_INSENSITIVE)
    };

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
}
