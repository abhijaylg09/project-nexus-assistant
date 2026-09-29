package com.nexus.persistence;

import com.nexus.personalization.UserProfile;

import java.sql.*;
import java.time.LocalDateTime;

public class UserProfileRepository {

    private final DatabaseManager dbManager;

    public UserProfileRepository() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public UserProfile getProfile() {
        String sql = "SELECT * FROM user_profile WHERE id = 1;";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                UserProfile profile = new UserProfile();
                profile.setUserName(rs.getString("user_name"));
                profile.setBehavioralSummary(rs.getString("behavioral_summary"));
                profile.setPreferredTone(rs.getString("preferred_tone"));
                profile.setTopTopics(rs.getString("top_topics"));
                profile.setTotalInteractions(rs.getInt("total_interactions"));
                return profile;
            }
        } catch (SQLException e) {
            System.err.println("[UserProfileRepository] Error getting profile: " + e.getMessage());
        }
        return new UserProfile();
    }

    public void updateProfile(UserProfile profile) {
        String sql = """
            UPDATE user_profile
            SET user_name = ?, behavioral_summary = ?, preferred_tone = ?, top_topics = ?, total_interactions = ?, last_synthesized_at = CURRENT_TIMESTAMP
            WHERE id = 1;
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, profile.getUserName());
            pstmt.setString(2, profile.getBehavioralSummary());
            pstmt.setString(3, profile.getPreferredTone());
            pstmt.setString(4, profile.getTopTopics());
            pstmt.setInt(5, profile.getTotalInteractions());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[UserProfileRepository] Error updating profile: " + e.getMessage());
        }
    }
}
