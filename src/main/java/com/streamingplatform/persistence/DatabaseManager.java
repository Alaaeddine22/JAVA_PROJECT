package com.streamingplatform.persistence;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:h2:./streamingservice_db";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    private static Connection connection;

    // Get connection (singleton)
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
        }
        return connection;
    }

    // Initialize DB using schema.sql
    @SuppressWarnings("CallToPrintStackTrace")
    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            String schema = new String(Files.readAllBytes(Paths.get("src/main/resources/schema.sql")));
            stmt.execute(schema);

            System.out.println("Database initialized successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
