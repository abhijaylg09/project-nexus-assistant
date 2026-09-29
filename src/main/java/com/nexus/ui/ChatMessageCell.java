package com.nexus.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Message Bubble View component for N.E.X.U.S Chat Stream.
 */
public class ChatMessageCell extends HBox {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ChatMessageCell(String text, boolean isUser, String moodTag, long latencyMs) {
        setSpacing(10);
        setPadding(new Insets(4, 10, 4, 10));

        VBox contentBox = new VBox(4);
        contentBox.setMaxWidth(550);

        Label metaLabel = new Label();
        metaLabel.getStyleClass().add("chat-meta");
        String timeStr = LocalTime.now().format(TIME_FMT);

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
