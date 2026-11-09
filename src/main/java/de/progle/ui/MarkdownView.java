package de.progle.ui;

import de.progle.ui.JavaSyntaxHighlighter.Token;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.List;

public final class MarkdownView {

    private MarkdownView() {
    }

    public static VBox build(String markdown) {
        VBox box = new VBox(7);
        box.setMinWidth(0);
        if (markdown == null || markdown.isBlank()) {
            return box;
        }
        String[] lines = markdown.replace("\r\n", "\n").split("\n", -1);
        StringBuilder codeBuffer = new StringBuilder();
        boolean inCode = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("```")) {
                if (inCode) {
                    box.getChildren().add(codeBlock(codeBuffer.toString()));
                    codeBuffer.setLength(0);
                }
                inCode = !inCode;
                continue;
            }
            if (inCode) {
                codeBuffer.append(line).append('\n');
                continue;
            }
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.startsWith("### ")) {
                box.getChildren().add(headline(trimmed.substring(4), "md-h3"));
            } else if (trimmed.startsWith("## ")) {
                box.getChildren().add(headline(trimmed.substring(3), "md-h2"));
            } else if (trimmed.startsWith("# ")) {
                box.getChildren().add(headline(trimmed.substring(2), "md-h1"));
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                box.getChildren().add(bullet(trimmed.substring(2)));
            } else if (trimmed.startsWith("|")) {
                box.getChildren().add(plainLine(trimmed.replace("|", "  "), "md-code-line"));
            } else {
                box.getChildren().add(inlineFlow(trimmed, "md-p"));
            }
        }
        if (codeBuffer.length() > 0) {
            box.getChildren().add(codeBlock(codeBuffer.toString()));
        }
        return box;
    }

    private static Node headline(String content, String styleClass) {
        TextFlow flow = inlineFlow(content, styleClass);
        for (Node child : flow.getChildren()) {
            if (child instanceof Text textNode) {
                textNode.wrappingWidthProperty().unbind();
            }
        }
        return flow;
    }

    private static Node bullet(String text) {
        TextFlow flow = inlineFlow(text, "md-p");
        return flow;
    }

    private static Node plainLine(String content, String styleClass) {
        TextFlow flow = new TextFlow();
        flow.setMinWidth(0);
        Text textNode = new Text(content);
        textNode.getStyleClass().add(styleClass);
        flow.getChildren().add(textNode);
        bindWrapping(flow, 0);
        return flow;
    }

    private static TextFlow inlineFlow(String text, String baseClass) {
        TextFlow flow = new TextFlow();
        flow.setMinWidth(0);
        String[] boldParts = text.split("\\*\\*");
        for (int i = 0; i < boldParts.length; i++) {
            String part = boldParts[i];
            boolean bold = i % 2 == 1 && !part.isEmpty();
            String[] codeParts = part.split("`", -1);
            for (int j = 0; j < codeParts.length; j++) {
                String segment = codeParts[j];
                if (segment.isEmpty()) {
                    continue;
                }
                Text textNode = new Text(segment);
                if (j % 2 == 1) {
                    textNode.getStyleClass().add("md-code-inline");
                } else if (bold) {
                    textNode.getStyleClass().add("md-bold");
                } else {
                    textNode.getStyleClass().add(baseClass);
                }
                flow.getChildren().add(textNode);
            }
        }
        bindWrapping(flow, 0);
        return flow;
    }

    private static Node codeBlock(String code) {
        TextFlow flow = new TextFlow();
        flow.setMinWidth(0);
        String[] lines = code.replace("\r\n", "\n").split("\n", -1);
        for (String line : lines) {
            if (line.isEmpty()) {
                flow.getChildren().add(new Text("\n"));
                continue;
            }
            List<Token> tokens = JavaSyntaxHighlighter.tokens(line);
            for (Token token : tokens) {
                Text text = new Text(token.text());
                text.getStyleClass().add("md-code");
                if (token.style() != null) {
                    text.getStyleClass().add(token.style());
                }
                flow.getChildren().add(text);
            }
            flow.getChildren().add(new Text("\n"));
        }
        bindWrapping(flow, 24);
        return flow;
    }

    private static void bindWrapping(TextFlow flow, double margin) {
        for (Node child : flow.getChildren()) {
            if (child instanceof Text text) {
                text.wrappingWidthProperty().bind(flow.widthProperty().subtract(margin));
            }
        }
    }
}
