package com.streamingplatform.analysis;

import com.streamingplatform.persistence.DatabaseManager;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;

public class DataExporter {
    public static void exportCSV() {
        try (Connection conn = DatabaseManager.getConnection();
             PrintWriter pw = new PrintWriter("nodes.csv");
             ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM messages")) {
            
            pw.println("id,topic,producer_id,content");
            while (rs.next()) {
                pw.printf("%d,%s,%s,%s%n", rs.getInt("id"), rs.getString("topic"), rs.getString("producer_id"), rs.getString("content"));
            }
            System.out.println("Exported nodes.csv for Neo4j.");
        } catch (Exception e) { e.printStackTrace(); }
    }
}