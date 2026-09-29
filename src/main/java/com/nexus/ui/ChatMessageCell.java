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
 * Message Bubble View component for N.E.X.U.S Chat Stream.
 * Supports rich text formatting, timestamp metadata, latency tags,
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

        Label metaLabel = new Label();
        metaLabel.getStyleClass().add("chat-meta");
        String timeStr = LocalTime.now().format(TIME_FMT);

        // Optional attached image preview
        if (attachedImageFile != null && attachedImageFile.exists()) {
            try {
                Image img = new Image(attachedImageFile.toURI().toString(), 280, 200, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(260);
                iv.setPreserveRatio(true);
                iv.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 242, 254, 0.4), 8, 0, 0, 0);");
                contentBox.getChildren().add(iv);
            } catch (Exception e) {}
        }

        Label textLabel = new Label(text);
        textLabel.setWrapText(true);

        if (isUser) {
            setAlignment(Pos.CENTER_RIGHT);
            contentBox.setAlignment(Pos.TOP_RIGHT);
            metaLabel.setText("YOU • " + timeStr);
            textLabel.getStyleClass().add("chat-bubble-user");
            contentBox.getChildren().addAll(metaLabel, textLabel);
            getChildren().add(contentBox);
        } else {
            setAlignment(Pos.CENTER_LEFT);
            contentBox.setAlignment(Pos.TOP_LEFT);

            String metaText = "N.E.X.U.S AI • " + timeStr;
            if (latencyMs > 0) metaText += " (" + latencyMs + "ms)";
            if (moodTag != null && !moodTag.isBlank()) metaText += " [Mood: " + moodTag + "]";
            metaLabel.setText(metaText);

            textLabel.getStyleClass().add("chat-bubble-assistant");
            contentBox.getChildren().addAll(metaLabel, textLabel);
            getChildren().add(contentBox);
        }
    }
}
