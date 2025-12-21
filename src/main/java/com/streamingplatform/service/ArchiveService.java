package com.streamingplatform.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

import com.streamingplatform.persistence.DatabaseManager;

/**
 * Archive/Cleanup service that deletes messages older than X minutes from H2.
 * Prevents memory and storage exhaustion after long runtimes.
 */
public class ArchiveService {

    private Timer cleanupTimer;
    private int retentionMinutes;
    private static final long CLEANUP_INTERVAL_MS = 60000; // Run every minute

    /**
     * Create archive service with specified retention period
     * 
     * @param retentionMinutes Messages older than this will be deleted
     */
    public ArchiveService(int retentionMinutes) {
        this.retentionMinutes = retentionMinutes;
    }

    /**
     * Start the cleanup service
     */
    public void start() {
        System.out.println("[Archive] Starting cleanup service (retention: " + retentionMinutes + " minutes)...");

        cleanupTimer = new Timer(true); // Daemon thread
        cleanupTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                cleanup();
            }
        }, CLEANUP_INTERVAL_MS, CLEANUP_INTERVAL_MS); // Start after 1 minute, run every minute
    }

    /**
     * Stop the cleanup service
     */
    public void stop() {
        if (cleanupTimer != null) {
            cleanupTimer.cancel();
            System.out.println("[Archive] Cleanup service stopped.");
        }
    }

    /**
     * Delete messages older than retention period
     */
    private void cleanup() {
        try {
            Connection conn = DatabaseManager.getConnection();

            // Delete messages older than X minutes
            String deleteSQL = "DELETE FROM messages WHERE timestamp < DATEADD('MINUTE', ?, CURRENT_TIMESTAMP)";

            PreparedStatement ps = conn.prepareStatement(deleteSQL);
            ps.setInt(1, -retentionMinutes); // Negative value for past time

            int deleted = ps.executeUpdate();
            ps.close();

            if (deleted > 0) {
                System.out.println(
                        "[Archive] Deleted " + deleted + " old messages (older than " + retentionMinutes + " min).");
            }

        } catch (SQLException e) {
            System.err.println("[Archive] Cleanup failed: " + e.getMessage());
        }
    }

    /**
     * Manually trigger cleanup (for testing)
     */
    public void cleanupNow() {
        cleanup();
    }

    /**
     * Update retention period at runtime
     * 
     * @param minutes New retention period in minutes
     */
    public void setRetentionMinutes(int minutes) {
        this.retentionMinutes = minutes;
        System.out.println("[Archive] Retention period updated to " + minutes + " minutes.");
    }

    /**
     * Get current retention setting
     */
    public int getRetentionMinutes() {
        return retentionMinutes;
    }
}
