package com.nexus.persistence;

import com.nexus.core.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLite persistence connection lifecycle and schema setup.
 */
public class DatabaseManager {

    private static DatabaseManager instance;
    private final String dbUrl;

    private DatabaseManager() {
        this.dbUrl = AppConfig.getInstance().getSqliteUrl();
        initSchema();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    private void initSchema() {
        String createInteractionsTable = """
            CREATE TABLE IF NOT EXISTS interactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_input TEXT NOT NULL,
                assistant_reply TEXT NOT NULL,
                input_source TEXT,
                detected_mood TEXT,
                detected_gesture TEXT,
                extracted_topics TEXT,
                sentiment_score REAL,
                latency_ms INTEGER,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            );
        """;

        String createUserProfileTable = """
            CREATE TABLE IF NOT EXISTS user_profile (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                user_name TEXT DEFAULT 'User',
                behavioral_summary TEXT,
                preferred_tone TEXT DEFAULT 'Balanced',
                top_topics TEXT,
                total_interactions INTEGER DEFAULT 0,
                last_synthesized_at DATETIME DEFAULT CURRENT_TIMESTAMP
            );
        """;

        String initDefaultProfile = """
            INSERT OR IGNORE INTO user_profile (id, user_name, behavioral_summary, preferred_tone, top_topics, total_interactions)
            VALUES (1, 'Nexus User', 'Initial profile: Responsive user interested in multi-modal systems, engineering, and concise AI collaboration.', 'Technical & Direct', 'AI, Architecture, Multimodal', 0);
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createInteractionsTable);
            stmt.execute(createUserProfileTable);
            stmt.execute(initDefaultProfile);
            System.out.println("[DatabaseManager] SQLite database and schema initialized successfully.");
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Schema initialization failed: " + e.getMessage());
        }
    }
}
