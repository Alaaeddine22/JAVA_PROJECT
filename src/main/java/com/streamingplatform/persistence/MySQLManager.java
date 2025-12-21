package com.streamingplatform.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages MySQL (XAMPP) database connections for hybrid storage.
 * Syncs topic relationships from H2 to MySQL for advanced queries.
 */
public class MySQLManager {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/";
    private static final String DB_NAME = "streamingplatform";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // Default XAMPP password

    private static Connection connection;

    /**
     * Get MySQL connection (singleton pattern)
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Load MySQL driver
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(DB_URL + DB_NAME, USER, PASSWORD);
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL Driver not found", e);
            }
        }
        return connection;
    }

    /**
     * Initialize MySQL database and tables
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public static void initializeDatabase() {
        try {
            // First connect without database to create it
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection initConn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
            Statement stmt = initConn.createStatement();

            // Create database if not exists
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            stmt.close();
            initConn.close();

            // Now connect to the database and create tables
            Connection conn = getConnection();
            Statement tableStmt = conn.createStatement();

            // Table for tracking topics
            tableStmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS topics (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            name VARCHAR(255) UNIQUE NOT NULL,
                            message_count INT DEFAULT 0,
                            last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                        )
                    """);

            // Table for tracking IP/Producer to Topic relationships
            tableStmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS ip_topic_stats (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            producer_id VARCHAR(255) NOT NULL,
                            topic VARCHAR(255) NOT NULL,
                            message_count INT DEFAULT 1,
                            last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            UNIQUE KEY unique_producer_topic (producer_id, topic)
                        )
                    """);

            // Table for Producers
            tableStmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS producers (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            producer_id VARCHAR(255) UNIQUE NOT NULL,
                            last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                        )
                    """);

            // Table for Consumers
            tableStmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS consumers (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            consumer_id VARCHAR(255) UNIQUE NOT NULL,
                            consumer_group VARCHAR(255),
                            last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                        )
                    """);

            // Table for Partitions
            tableStmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS partitions (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            partition_id VARCHAR(255) UNIQUE NOT NULL,
                            topic VARCHAR(255) NOT NULL,
                            leader VARCHAR(255),
                            replicas INT DEFAULT 1,
                            last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                        )
                    """);

            // Table for Consumer Groups
            tableStmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS consumer_groups (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            group_id VARCHAR(255) UNIQUE NOT NULL,
                            member_count INT DEFAULT 0,
                            last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                        )
                    """);

            tableStmt.close();
            System.out.println("[MySQL] Database initialized successfully.");

        } catch (Exception e) {
            System.err.println("[MySQL] Failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Close MySQL connection
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
