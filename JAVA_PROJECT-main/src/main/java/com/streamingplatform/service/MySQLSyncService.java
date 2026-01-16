package com.streamingplatform.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

import com.streamingplatform.persistence.DatabaseManager;
import com.streamingplatform.persistence.MySQLManager;

/**
 * Background service that syncs topic relationships from H2 to MySQL.
 * Enables advanced queries like "Which IP posts to the most topics?"
 */
public class MySQLSyncService {

    private Timer syncTimer;
    private static final long SYNC_INTERVAL_MS = 5000; // Sync every 5 seconds

    /**
     * Start the background sync service
     */
    public void start() {
        System.out.println("[MySQLSync] Starting background sync service...");

        syncTimer = new Timer(true); // Daemon thread
        syncTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                syncToMySQL();
            }
        }, 0, SYNC_INTERVAL_MS);
    }

    /**
     * Stop the sync service
     */
    public void stop() {
        if (syncTimer != null) {
            syncTimer.cancel();
            System.out.println("[MySQLSync] Sync service stopped.");
        }
    }

    /**
     * Sync H2 data to MySQL
     */
    private void syncToMySQL() {
        try {
            Connection h2Conn = DatabaseManager.getConnection();
            Connection mysqlConn = MySQLManager.getConnection();

            // Get producer-topic stats from H2
            String h2Query = """
                        SELECT producer_id, topic, COUNT(*) as msg_count
                        FROM messages
                        WHERE producer_id IS NOT NULL
                        GROUP BY producer_id, topic
                    """;

            ResultSet rs = h2Conn.createStatement().executeQuery(h2Query);

            // Upsert into MySQL
            String upsertSQL = """
                        INSERT INTO ip_topic_stats (producer_id, topic, message_count)
                        VALUES (?, ?, ?)
                        ON DUPLICATE KEY UPDATE message_count = VALUES(message_count), last_seen = CURRENT_TIMESTAMP
                    """;

            PreparedStatement ps = mysqlConn.prepareStatement(upsertSQL);

            int synced = 0;
            while (rs.next()) {
                ps.setString(1, rs.getString("producer_id"));
                ps.setString(2, rs.getString("topic"));
                ps.setInt(3, rs.getInt("msg_count"));
                ps.executeUpdate();
                synced++;
            }

            rs.close();
            ps.close();

            // Also sync topic counts
            syncTopicCounts(h2Conn, mysqlConn);

            if (synced > 0) {
                System.out.println("[MySQLSync] Synced " + synced + " producer-topic records.");
            }

        } catch (SQLException e) {
            // Silently handle if MySQL is not available
            // System.err.println("[MySQLSync] Sync failed: " + e.getMessage());
        }
    }

    /**
     * Sync topic message counts to MySQL
     */
    private void syncTopicCounts(Connection h2Conn, Connection mysqlConn) throws SQLException {
        String h2Query = "SELECT topic, COUNT(*) as cnt FROM messages GROUP BY topic";
        ResultSet rs = h2Conn.createStatement().executeQuery(h2Query);

        String upsertSQL = """
                    INSERT INTO topics (name, message_count) VALUES (?, ?)
                    ON DUPLICATE KEY UPDATE message_count = VALUES(message_count)
                """;

        PreparedStatement ps = mysqlConn.prepareStatement(upsertSQL);

        while (rs.next()) {
            ps.setString(1, rs.getString("topic"));
            ps.setInt(2, rs.getInt("cnt"));
            ps.executeUpdate();
        }

        rs.close();
        ps.close();
    }

    /**
     * Query: Which IP/Producer posts to the most topics?
     * 
     * @return Formatted result string
     */
    public String getTopProducerByTopicCount() {
        StringBuilder result = new StringBuilder();
        result.append("\n=== Which IP/Producer posts to the most topics? ===\n");

        try {
            Connection mysqlConn = MySQLManager.getConnection();

            String query = """
                        SELECT producer_id, COUNT(DISTINCT topic) as topic_count, SUM(message_count) as total_messages
                        FROM ip_topic_stats
                        GROUP BY producer_id
                        ORDER BY topic_count DESC, total_messages DESC
                        LIMIT 10
                    """;

            ResultSet rs = mysqlConn.createStatement().executeQuery(query);

            result.append(String.format("%-20s | %-12s | %-15s%n", "Producer/IP", "Topics", "Total Messages"));
            result.append("-".repeat(52)).append("\n");

            while (rs.next()) {
                result.append(String.format("%-20s | %-12d | %-15d%n",
                        rs.getString("producer_id"),
                        rs.getInt("topic_count"),
                        rs.getInt("total_messages")));
            }

            rs.close();

        } catch (SQLException e) {
            result.append("Error: MySQL not available - ").append(e.getMessage());
        }

        return result.toString();
    }
}
