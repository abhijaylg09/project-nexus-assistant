package com.nexus.ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * ChatGPT-Grade Glassmorphic Message View component for Project N.E.X.U.S.
 * 
 * Features:
 *  - Native Markdown Parser: Headings (###), Bullet points, Numbered lists, Blockquotes
 *  - Syntax Code Blocks with monospace font and 1-Click "📋 Copy Code" buttons
 *  - 1-Click "📋 Copy Response" button on every assistant answer
 *  - Animated "Thinking..." pulse indicator for live query generation
 *  - Multimodal image attachments and perceptual telemetry badges
 */
public class ChatMessageCell extends HBox {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ChatMessageCell(String text, boolean isUser, String moodTag, long latencyMs) {
        this(text, null, isUser, moodTag, latencyMs);
    }

    public ChatMessageCell(String text, File attachedImageFile, boolean isUser, String moodTag, long latencyMs) {
        setSpacing(10);
        setPadding(new Insets(6, 12, 6, 12));

        VBox contentBox = new VBox(8);
        contentBox.setMaxWidth(620);

        String timeStr = LocalTime.now().format(TIME_FMT);

        // Optional attached image preview
        if (attachedImageFile != null && attachedImageFile.exists()) {
            try {
                Image img = new Image(attachedImageFile.toURI().toString(), 280, 200, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(260);
                iv.setPreserveRatio(true);
                iv.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 242, 254, 0.5), 10, 0, 0, 0); -fx-border-radius: 8px;");
                contentBox.getChildren().add(iv);
            } catch (Exception ignored) {}
        }

        if (isUser) {
            // ── USER MESSAGE ──
            setAlignment(Pos.CENTER_RIGHT);
            contentBox.setAlignment(Pos.TOP_RIGHT);

            Label metaLabel = new Label("👤 YOU • " + timeStr);
            metaLabel.getStyleClass().add("chat-meta");

            Label textLabel = new Label(text);
            textLabel.setWrapText(true);
            textLabel.getStyleClass().add("chat-bubble-user");

            contentBox.getChildren().addAll(metaLabel, textLabel);
            getChildren().add(contentBox);

        } else {
            // ── ASSISTANT / CHATGPT-STYLE MESSAGE ──
            setAlignment(Pos.CENTER_LEFT);
            contentBox.setAlignment(Pos.TOP_LEFT);

            // 1. Meta Header Bar
            HBox metaRow = new HBox(8);
            metaRow.setAlignment(Pos.CENTER_LEFT);

            Label aiLabel = new Label("⚡ N.E.X.U.S AI • " + timeStr);
            aiLabel.getStyleClass().add("chat-meta");
            metaRow.getChildren().add(aiLabel);

            if (moodTag != null && !moodTag.isBlank()) {
                Label moodBadge = new Label("● " + moodTag);
                String moodStyle = "-fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 2px 8px; -fx-background-radius: 10px; ";
                if (moodTag.contains("HAPPY")) {
                    moodStyle += "-fx-background-color: rgba(16, 185, 129, 0.22); -fx-text-fill: #34d399; -fx-border-color: #10b981; -fx-border-radius: 10px;";
                } else if (moodTag.contains("STRESSED")) {
                    moodStyle += "-fx-background-color: rgba(244, 63, 94, 0.22); -fx-text-fill: #fb7185; -fx-border-color: #f43f5e; -fx-border-radius: 10px;";
                } else if (moodTag.contains("SURPRISED")) {
                    moodStyle += "-fx-background-color: rgba(245, 158, 11, 0.22); -fx-text-fill: #fbbf24; -fx-border-color: #f59e0b; -fx-border-radius: 10px;";
                } else {
                    moodStyle += "-fx-background-color: rgba(0, 242, 254, 0.20); -fx-text-fill: #38bdf8; -fx-border-color: #00f2fe; -fx-border-radius: 10px;";
                }
                moodBadge.setStyle(moodStyle);
                metaRow.getChildren().add(moodBadge);
            }

            if (latencyMs > 0) {
                Label latLabel = new Label(latencyMs + "ms");
                latLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #64748b; -fx-font-family: Consolas;");
                metaRow.getChildren().add(latLabel);
            }

            // 2. Main Markdown Rendered Container
            VBox bodyBox = renderMarkdownContent(text);
            bodyBox.getStyleClass().add("chat-bubble-assistant");

            // 3. Footer Action Toolbar (Copy Response Button)
            HBox footerRow = new HBox(8);
            footerRow.setAlignment(Pos.CENTER_LEFT);
            footerRow.setPadding(new Insets(2, 0, 0, 4));

            Button copyFullBtn = new Button("📋 Copy");
            copyFullBtn.getStyleClass().add("hud-button-secondary");
            copyFullBtn.setStyle("-fx-font-size: 9px; -fx-padding: 2px 7px; -fx-background-radius: 6px; -fx-text-fill: #94a3b8;");
            copyFullBtn.setOnAction(e -> {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent cc = new ClipboardContent();
                cc.putString(text);
                clipboard.setContent(cc);
                copyFullBtn.setText("✓ Copied!");
                copyFullBtn.setStyle("-fx-font-size: 9px; -fx-padding: 2px 7px; -fx-background-radius: 6px; -fx-text-fill: #34d399; -fx-border-color: #10b981;");

                Timeline revert = new Timeline(new KeyFrame(Duration.seconds(2), ev -> {
                    copyFullBtn.setText("📋 Copy");
                    copyFullBtn.setStyle("-fx-font-size: 9px; -fx-padding: 2px 7px; -fx-background-radius: 6px; -fx-text-fill: #94a3b8;");
                }));
                revert.play();
            });

            footerRow.getChildren().add(copyFullBtn);

            contentBox.getChildren().addAll(metaRow, bodyBox, footerRow);
            getChildren().add(contentBox);
        }
    }

    /**
     * Creates an animated "Thinking..." bubble for immediate user feedback.
     */
    public static ChatMessageCell createThinkingCell() {
        ChatMessageCell cell = new ChatMessageCell("", false, "THINKING", 0);
        cell.getChildren().clear();

        VBox box = new VBox(6);
        box.setMaxWidth(400);

        HBox thinkingBubble = new HBox(10);
        thinkingBubble.setAlignment(Pos.CENTER_LEFT);
        thinkingBubble.setStyle("-fx-background-color: rgba(15, 23, 42, 0.85); -fx-border-color: #00f2fe; -fx-border-radius: 14px; -fx-background-radius: 14px; -fx-padding: 10px 16px;");

        Label pulseDot = new Label("⚡");
        pulseDot.setStyle("-fx-font-size: 14px; -fx-text-fill: #00f2fe;");

        Label thinkingText = new Label("N.E.X.U.S is thinking...");
        thinkingText.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 12px; -fx-font-weight: bold;");

        thinkingBubble.getChildren().addAll(pulseDot, thinkingText);

        // Smooth breathing pulse animation
        Timeline pulseTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(thinkingBubble.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(600), new KeyValue(thinkingBubble.opacityProperty(), 0.35)),
                new KeyFrame(Duration.millis(1200), new KeyValue(thinkingBubble.opacityProperty(), 1.0))
        );
        pulseTimeline.setCycleCount(Animation.INDEFINITE);
        pulseTimeline.play();

        box.getChildren().add(thinkingBubble);
        cell.getChildren().add(box);
        return cell;
    }

    /**
     * Parses raw Markdown into structured JavaFX nodes (Code Blocks, Headings, Lists, Paragraphs).
     */
    private VBox renderMarkdownContent(String rawMarkdown) {
        VBox container = new VBox(6);
        container.setFillWidth(true);

        if (rawMarkdown == null || rawMarkdown.isBlank()) {
            Label empty = new Label("(No response content)");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic;");
            container.getChildren().add(empty);
            return container;
        }

        String[] lines = rawMarkdown.split("\\r?\\n");
        boolean inCodeBlock = false;
        String codeLanguage = "CODE";
        StringBuilder codeBuffer = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // Code block delimiter
            if (line.trim().startsWith("```")) {
                if (inCodeBlock) {
                    // Close code block
                    container.getChildren().add(createCodeBlockNode(codeLanguage, codeBuffer.toString().trim()));
                    codeBuffer.setLength(0);
                    inCodeBlock = false;
                } else {
                    // Open code block
                    inCodeBlock = true;
                    String lang = line.trim().substring(3).trim();
                    codeLanguage = (lang.isEmpty()) ? "CODE" : lang.toUpperCase();
                }
                continue;
            }

            if (inCodeBlock) {
                codeBuffer.append(line).append("\n");
                continue;
            }

            // Headings
            String trimmed = line.trim();
            if (trimmed.startsWith("### ")) {
                Label h3 = new Label(cleanInlineMarkdown(trimmed.substring(4)));
                h3.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #00f2fe; -fx-padding: 4px 0 2px 0;");
                h3.setWrapText(true);
                container.getChildren().add(h3);
            } else if (trimmed.startsWith("## ")) {
                Label h2 = new Label(cleanInlineMarkdown(trimmed.substring(3)));
                h2.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-padding: 6px 0 2px 0;");
                h2.setWrapText(true);
                container.getChildren().add(h2);
            } else if (trimmed.startsWith("# ")) {
                Label h1 = new Label(cleanInlineMarkdown(trimmed.substring(2)));
                h1.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ffffff; -fx-padding: 8px 0 4px 0;");
                h1.setWrapText(true);
                container.getChildren().add(h1);
            } else if (trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("• ")) {
                // Bullet list item
                String bulletText = trimmed.substring(2).trim();
                HBox bulletRow = new HBox(6);
                bulletRow.setAlignment(Pos.TOP_LEFT);
                bulletRow.setPadding(new Insets(1, 0, 1, 10));

                Label dot = new Label("●");
                dot.setStyle("-fx-font-size: 7px; -fx-text-fill: #00f2fe; -fx-padding: 4px 0 0 0;");

                Label text = new Label(cleanInlineMarkdown(bulletText));
                text.setWrapText(true);
                text.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 12px; -fx-line-spacing: 2px;");
                HBox.setHgrow(text, Priority.ALWAYS);

                bulletRow.getChildren().addAll(dot, text);
                container.getChildren().add(bulletRow);
            } else if (trimmed.matches("^\\d+\\.\\s+.*")) {
                // Numbered list item
                int dotIdx = trimmed.indexOf('.');
                String num = trimmed.substring(0, dotIdx + 1);
                String body = trimmed.substring(dotIdx + 1).trim();

                HBox numRow = new HBox(6);
                numRow.setAlignment(Pos.TOP_LEFT);
                numRow.setPadding(new Insets(1, 0, 1, 10));

                Label numLabel = new Label(num);
                numLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

                Label text = new Label(cleanInlineMarkdown(body));
                text.setWrapText(true);
                text.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 12px; -fx-line-spacing: 2px;");
                HBox.setHgrow(text, Priority.ALWAYS);

                numRow.getChildren().addAll(numLabel, text);
                container.getChildren().add(numRow);
            } else if (trimmed.startsWith("> ")) {
                // Blockquote
                Label quote = new Label(cleanInlineMarkdown(trimmed.substring(2)));
                quote.setWrapText(true);
                quote.setStyle("-fx-background-color: rgba(0, 242, 254, 0.08); -fx-border-color: #00f2fe; -fx-border-width: 0 0 0 3px; -fx-padding: 4px 10px; -fx-text-fill: #cbd5e1; -fx-font-style: italic;");
                container.getChildren().add(quote);
            } else if (!trimmed.isEmpty()) {
                // Normal paragraph
                Label p = new Label(cleanInlineMarkdown(trimmed));
                p.setWrapText(true);
                p.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 12px; -fx-line-spacing: 3px;");
                container.getChildren().add(p);
            }
        }

        // Catch unclosed code block if stream finished mid-block
        if (inCodeBlock && codeBuffer.length() > 0) {
            container.getChildren().add(createCodeBlockNode(codeLanguage, codeBuffer.toString().trim()));
        }

        return container;
    }

    /**
     * Creates a syntax code block container with a header bar and "📋 Copy Code" button.
     */
    private Node createCodeBlockNode(String language, String code) {
        VBox codeBox = new VBox(0);
        codeBox.setStyle("-fx-background-color: #090e1a; -fx-border-color: rgba(56, 189, 248, 0.40); -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 2);");
        codeBox.setPadding(new Insets(0));

        // Code block header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(5, 12, 5, 12));
        header.setStyle("-fx-background-color: #111827; -fx-background-radius: 8px 8px 0 0; -fx-border-color: rgba(56, 189, 248, 0.25); -fx-border-width: 0 0 1px 0;");

        Label langLabel = new Label(language.toUpperCase());
        langLabel.setStyle("-fx-text-fill: #00f2fe; -fx-font-size: 10px; -fx-font-weight: bold; -fx-font-family: Consolas;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button copyBtn = new Button("📋 Copy Code");
        copyBtn.getStyleClass().add("hud-button-secondary");
        copyBtn.setStyle("-fx-font-size: 9px; -fx-padding: 2px 8px; -fx-background-radius: 4px;");
        copyBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent cc = new ClipboardContent();
            cc.putString(code);
            clipboard.setContent(cc);
            copyBtn.setText("✓ Copied!");
            copyBtn.setStyle("-fx-font-size: 9px; -fx-padding: 2px 8px; -fx-background-radius: 4px; -fx-text-fill: #34d399; -fx-border-color: #10b981;");

            Timeline revert = new Timeline(new KeyFrame(Duration.seconds(2), ev -> {
                copyBtn.setText("📋 Copy Code");
                copyBtn.setStyle("-fx-font-size: 9px; -fx-padding: 2px 8px; -fx-background-radius: 4px;");
            }));
            revert.play();
        });

        header.getChildren().addAll(langLabel, spacer, copyBtn);

        // Code content area
        TextArea codeArea = new TextArea(code);
        codeArea.setEditable(false);
        codeArea.setWrapText(false);
        int lineCount = Math.max(1, code.split("\\r?\\n").length);
        codeArea.setPrefRowCount(Math.min(lineCount, 16));
        codeArea.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-control-inner-background: #090e1a; " +
                "-fx-text-fill: #38bdf8; " +
                "-fx-font-family: Consolas, 'Fira Code', 'Courier New', monospace; " +
                "-fx-font-size: 11px; " +
                "-fx-border-color: transparent; " +
                "-fx-padding: 6px 10px;"
        );

        codeBox.getChildren().addAll(header, codeArea);
        return codeBox;
    }

    /**
     * Cleans inline bold / italic / code markers for clean reading.
     */
    private String cleanInlineMarkdown(String text) {
        if (text == null) return "";
        // Replace **bold** with bold indicators or clean formatting
        return text.replace("**", "").replace("__", "").replace("`", "");
    }
}
