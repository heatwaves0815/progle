package de.progle.ui;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JavaSyntaxHighlighter {

    public record Token(String text, String style) {
    }

    /* deklarierende Schluesselwoerter -> Blau (wie VS Code Dark+) */
    private static final String DECL_KEYWORDS =
            "public|private|protected|static|final|abstract|class|interface|enum|record|sealed|"
                    + "permits|extends|implements|import|package|void|int|long|short|byte|char|float|"
                    + "double|boolean|var|native|strictfp|transient|volatile|synchronized";

    /* Steuer-Schluesselwoerter -> Rosa */
    private static final String CTRL_KEYWORDS =
            "if|else|for|while|do|switch|case|default|return|break|continue|new|try|catch|"
                    + "finally|throw|throws|instanceof|this|super|assert|yield";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<COMMENT>//[^\n]*|/\\*.*?\\*/)"
                    + "|(?<TEXTBLOCK>\"\"\"[\\s\\S]*?\"\"\")"
                    + "|(?<STRING>\"(?:\\\\.|[^\"\\\\\n])*\")"
                    + "|(?<CHAR>'(?:\\\\.|[^'\\\\\n])')"
                    + "|(?<ANNOTATION>@\\w+)"
                    + "|(?<KWDECL>\\b(?:" + DECL_KEYWORDS + ")\\b)"
                    + "|(?<KWCTRL>\\b(?:" + CTRL_KEYWORDS + ")\\b)"
                    + "|(?<LITERAL>\\b(?:true|false|null)\\b)"
                    + "|(?<METHOD>\\b[a-z_][A-Za-z0-9_$]*(?=\\s*\\())"
                    + "|(?<TYPE>\\b[A-Z][A-Za-z0-9_$]*\\b)"
                    + "|(?<NUMBER>\\b\\d[\\d_]*(?:\\.\\d+)?(?:[eE][+-]?\\d+)?[fFdDlL]?\\b)",
            Pattern.DOTALL);

    private static final String[] GROUPS =
            {"COMMENT", "TEXTBLOCK", "STRING", "CHAR", "ANNOTATION", "KWDECL", "KWCTRL",
                    "LITERAL", "METHOD", "TYPE", "NUMBER"};

    private JavaSyntaxHighlighter() {
    }

    public static List<Token> tokens(String text) {
        List<Token> tokens = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return tokens;
        }
        Matcher matcher = PATTERN.matcher(text);
        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                tokens.add(new Token(text.substring(lastEnd, matcher.start()), null));
            }
            tokens.add(new Token(matcher.group(), styleClass(matcher)));
            lastEnd = matcher.end();
        }
        if (lastEnd < text.length()) {
            tokens.add(new Token(text.substring(lastEnd), null));
        }
        return tokens;
    }

    public static StyleSpans<Collection<String>> compute(String text) {
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        try {
            if (text != null && !text.isEmpty()) {
                for (Token token : tokens(text)) {
                    int length = token.text().length();
                    if (length == 0) {
                        continue;
                    }
                    Collection<String> style = token.style() == null
                            ? Collections.emptyList()
                            : Collections.singleton(token.style());
                    builder.add(style, length);
                }
            }
            return builder.create();
        } catch (Exception e) {
            return null;
        }
    }

    private static String styleClass(Matcher matcher) {
        for (String group : GROUPS) {
            if (matcher.group(group) != null) {
                return switch (group) {
                    case "KWDECL" -> "kw-decl";
                    case "KWCTRL" -> "kw-ctrl";
                    case "LITERAL" -> "literal";
                    case "METHOD" -> "method";
                    case "TYPE" -> "type";
                    default -> group.toLowerCase(Locale.ROOT);
                };
            }
        }
        return "";
    }
}
