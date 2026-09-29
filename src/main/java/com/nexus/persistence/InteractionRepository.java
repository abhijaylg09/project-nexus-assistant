package com.nexus.persistence;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class InteractionRepository {

    private final DatabaseManager dbManager;

    public InteractionRepository() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public void save(InteractionEntity entity) {
        String sql = """
            INSERT INTO interactions (user_input, assistant_reply, input_source, detected_mood, detected_gesture, extracted_topics, sentiment_score, latency_ms)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, entity.getUserInput());
            pstmt.setString(2, entity.getAssistantReply());
            pstmt.setString(3, entity.getInputSource());
            pstmt.setString(4, entity.getDetectedMood());
            pstmt.setString(5, entity.getDetectedGesture());
            pstmt.setString(6, entity.getExtractedTopics());
            pstmt.setDouble(7, entity.getSentimentScore());
            pstmt.setLong(8, entity.getLatencyMs());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[InteractionRepository] Failed to save interaction: " + e.getMessage());
        }
    }

    public List<InteractionEntity> getRecent(int limit) {
        List<InteractionEntity> list = new ArrayList<>();
        String sql = "SELECT * FROM interactions ORDER BY id DESC LIMIT ?;";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    InteractionEntity entity = new InteractionEntity();
                    entity.setId(rs.getLong("id"));
                    entity.setUserInput(rs.getString("user_input"));
                    entity.setAssistantReply(rs.getString("assistant_reply"));
                    entity.setInputSource(rs.getString("input_source"));
                    entity.setDetectedMood(rs.getString("detected_mood"));
                    entity.setDetectedGesture(rs.getString("detected_gesture"));
                    entity.setExtractedTopics(rs.getString("extracted_topics"));
                    entity.setSentimentScore(rs.getDouble("sentiment_score"));
                    entity.setLatencyMs(rs.getLong("latency_ms"));
                    list.add(entity);
                }
            }
        } catch (SQLException e) {
            System.err.println("[InteractionRepository] Failed to query interactions: " + e.getMessage());
        }
        return list;
    }

    public int getTotalCount() {
        String sql = "SELECT COUNT(*) FROM interactions;";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[InteractionRepository] Error counting interactions: " + e.getMessage());
        }
        return 0;
    }

    public double getAverageSentiment() {
        String sql = "SELECT AVG(sentiment_score) FROM (SELECT sentiment_score FROM interactions ORDER BY id DESC LIMIT 30);";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            System.err.println("[InteractionRepository] Error calculating sentiment: " + e.getMessage());
        }
        return 0.0;
    }
}
