package de.progle.service;

import java.util.Locale;
import java.util.Set;

public final class UmlautTransliterator {

    private UmlautTransliterator() {
    }

    private static final Set<String> KEEP_STEMS = Set.of(
            "goethe", "israel", "maeander", "aerob", "aerosol",
            "poet", "poesie", "koeff", "koex");

    private static final Set<String> SS_STEMS = Set.of(
            "fuss", "gross", "strass", "bloss", "maass", "spass");

    private static final Set<String> SS_DIGRAPHS = Set.of(
            "au", "ei", "eu", "ie", "ai", "oe", "ue", "ae");

    private static final Set<String> SS_KEEP_STEMS = Set.of(
            "muesst");

    public static String toUmlauts(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(text.length() + 8);
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (c == 's' && i + 1 < n && text.charAt(i + 1) == 's' && shouldConvertSs(lower, i)) {
                out.append('ß');
                i += 2;
                continue;
            }
            if (i + 1 < n && lower.charAt(i + 1) == 'e') {
                boolean upper = Character.isUpperCase(c);
                char cLower = lower.charAt(i);
                if (cLower == 'a' && canConvertVowelPair(text, lower, i, false)) {
                    out.append(upper ? 'Ä' : 'ä');
                    i += 2;
                    continue;
                }
                if (cLower == 'o' && canConvertVowelPair(text, lower, i, false)) {
                    out.append(upper ? 'Ö' : 'ö');
                    i += 2;
                    continue;
                }
                if (cLower == 'u' && canConvertVowelPair(text, lower, i, true)) {
                    out.append(upper ? 'Ü' : 'ü');
                    i += 2;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    public static String convertJavaStrings(String code) {
        if (code == null || code.isEmpty()) {
            return code;
        }
        java.util.regex.Matcher matcher = JAVA_STRING.matcher(code);
        StringBuilder sb = new StringBuilder();
        int last = 0;
        while (matcher.find()) {
            String literal = matcher.group();
            String inner = literal.substring(1, literal.length() - 1);
            sb.append(code, last, matcher.start())
                    .append('"').append(toUmlauts(inner)).append('"');
            last = matcher.end();
        }
        sb.append(code.substring(last));
        return sb.toString();
    }

    public static String foldToAscii(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("ä", "ae").replace("ö", "oe").replace("ü", "ue")
                .replace("Ä", "Ae").replace("Ö", "Oe").replace("Ü", "Ue")
                .replace("ß", "ss");
    }

    private static boolean canConvertVowelPair(String text, String lower, int i, boolean ueExtraGuard) {
        if (i > 0) {
            char prev = lower.charAt(i - 1);
            if (prev == 'a' || prev == 'e' || prev == 'i' || prev == 'o' || prev == 'u'
                    || prev == 'ä' || prev == 'ö' || prev == 'ü' || prev == 'q') {
                return false;
            }
        }
        if (ueExtraGuard && i + 3 < text.length()
                && lower.charAt(i + 2) == 'l' && lower.charAt(i + 3) == 'l') {
            return false;
        }
        String word = wordAround(lower, i);
        for (String stem : KEEP_STEMS) {
            if (word.startsWith(stem)) {
                return false;
            }
        }
        return true;
    }

    private static boolean shouldConvertSs(String lower, int i) {
        String word = wordAround(lower, i);
        for (String stem : SS_KEEP_STEMS) {
            if (word.startsWith(stem)) {
                return false;
            }
        }
        if (i >= 2 && SS_DIGRAPHS.contains(lower.substring(i - 2, i))) {
            return true;
        }
        for (String stem : SS_STEMS) {
            if (word.startsWith(stem)) {
                return true;
            }
        }
        return false;
    }

    private static String wordAround(String lower, int pos) {
        int start = pos;
        while (start > 0 && Character.isLetter(lower.charAt(start - 1))) {
            start--;
        }
        int end = pos + 1;
        while (end < lower.length() && Character.isLetter(lower.charAt(end))) {
            end++;
        }
        return lower.substring(start, end);
    }

    private static final java.util.regex.Pattern JAVA_STRING =
            java.util.regex.Pattern.compile("\"(?:\\\\.|[^\"\\\\\\n])*\"");
}
