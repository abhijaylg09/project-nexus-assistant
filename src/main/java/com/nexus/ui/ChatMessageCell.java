package com.nexus.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * High-tech Glassmorphic Message View component for N.E.X.U.S Chat Stream.
 * Features glowing mood badges, avatar identifiers, timestamp metadata,
 * and multimodal attached image preview frames.
 */
public class ChatMessageCell extends HBox {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ChatMessageCell(String text, boolean isUser, String moodTag, long latencyMs) {
        this(text, null, isUser, moodTag, latencyMs);
    }

    public ChatMessageCell(String text, File attachedImageFile, boolean isUser, String moodTag, long latencyMs) {
        setSpacing(10);
        setPadding(new Insets(4, 10, 4, 10));

        VBox contentBox = new VBox(6);
        contentBox.setMaxWidth(550);

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

        Label textLabel = new Label(text);
        textLabel.setWrapText(true);

        if (isUser) {
            setAlignment(Pos.CENTER_RIGHT);
            contentBox.setAlignment(Pos.TOP_RIGHT);

            Label metaLabel = new Label("👤 YOU • " + timeStr);
            metaLabel.getStyleClass().add("chat-meta");

            textLabel.getStyleClass().add("chat-bubble-user");
            contentBox.getChildren().addAll(metaLabel, textLabel);
            getChildren().add(contentBox);
        } else {
            setAlignment(Pos.CENTER_LEFT);
            contentBox.setAlignment(Pos.TOP_LEFT);

            HBox metaRow = new HBox(6);
            metaRow.setAlignment(Pos.CENTER_LEFT);

            Label aiLabel = new Label("⚡ N.E.X.U.S AI • " + timeStr);
            aiLabel.getStyleClass().add("chat-meta");
            metaRow.getChildren().add(aiLabel);

            if (moodTag != null && !moodTag.isBlank()) {
                Label moodBadge = new Label("● " + moodTag);
                String moodStyle = "-fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 1px 7px; -fx-background-radius: 10px; ";
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

            textLabel.getStyleClass().add("chat-bubble-assistant");
            contentBox.getChildren().addAll(metaRow, textLabel);
            getChildren().add(contentBox);
        }
    }
}
