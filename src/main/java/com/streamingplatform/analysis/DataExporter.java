package com.streamingplatform.analysis;

import com.streamingplatform.persistence.DatabaseManager;
import com.streamingplatform.persistence.Neo4jManager;

import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Enhanced Data Exporter supporting CSV, JSON, and timestamped backups.
 * Exports data from both H2 (SQL) and Neo4j (Graph) databases.
 * Supports offline analysis and data archiving.
 */
public class DataExporter {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // =========================================================================
    // H2 SQL EXPORTS
    // =========================================================================

    /**
     * Export messages to CSV (original method for Neo4j import)
     */
    public static void exportCSV() {
        exportCSV("nodes.csv");
    }

    /**
     * Export messages to CSV with custom filename
     */
    public static void exportCSV(String filename) {
        try (Connection conn = DatabaseManager.getConnection();
                PrintWriter pw = new PrintWriter(filename);
                ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM messages ORDER BY id")) {

            pw.println("id,topic,producer_id,content,timestamp");
            while (rs.next()) {
                String content = escapeCSV(rs.getString("content"));
                String producerId = escapeCSV(rs.getString("producer_id"));
                pw.printf("%d,%s,%s,%s,%s%n",
                        rs.getInt("id"),
                        rs.getString("topic"),
                        producerId,
                        content,
                        rs.getTimestamp("timestamp"));
            }
            System.out.println("[Export] Created " + filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Export messages to JSON format
     */
    public static void exportJSON() {
        exportJSON("messages.json");
    }

    /**
     * Export messages to JSON with custom filename
     */
    public static void exportJSON(String filename) {
        try (Connection conn = DatabaseManager.getConnection();
                PrintWriter pw = new PrintWriter(filename);
                ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM messages ORDER BY id")) {

            pw.println("{");
            pw.println("  \"exportDate\": \"" + LocalDateTime.now() + "\",");
            pw.println("  \"messages\": [");

            boolean first = true;
            while (rs.next()) {
                if (!first)
                    pw.println(",");
                first = false;

                pw.print("    {");
                pw.printf("\"id\": %d, ", rs.getInt("id"));
                pw.printf("\"topic\": \"%s\", ", escapeJSON(rs.getString("topic")));
                pw.printf("\"producer_id\": \"%s\", ", escapeJSON(rs.getString("producer_id")));
                pw.printf("\"content\": \"%s\", ", escapeJSON(rs.getString("content")));
                pw.printf("\"timestamp\": \"%s\"", rs.getTimestamp("timestamp"));
                pw.print("}");
            }

            pw.println();
            pw.println("  ]");
            pw.println("}");

            System.out.println("[Export] Created " + filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    // NEO4J GRAPH EXPORTS
    // =========================================================================

    /**
     * Export Neo4j Topics to CSV
     */
    public static void exportNeo4jTopicsCSV() {
        exportNeo4jTopicsCSV("neo4j_topics.csv");
    }

    public static void exportNeo4jTopicsCSV(String filename) {
        try (Session session = Neo4jManager.getDriver().session();
                PrintWriter pw = new PrintWriter(filename)) {

            pw.println("name,messageCount,producerCount,consumerCount");

            Result result = session.run(
                    "MATCH (t:Topic) " +
                            "OPTIONAL MATCH (p:Producer)-[:PUBLISHES_TO]->(t) " +
                            "OPTIONAL MATCH (c:Consumer)-[:SUBSCRIBES_TO]->(t) " +
                            "RETURN t.name as name, t.messageCount as messageCount, " +
                            "COUNT(DISTINCT p) as producerCount, COUNT(DISTINCT c) as consumerCount " +
                            "ORDER BY messageCount DESC");

            while (result.hasNext()) {
                Record record = result.next();
                pw.printf("%s,%d,%d,%d%n",
                        escapeCSV(record.get("name").asString()),
                        record.get("messageCount").isNull() ? 0 : record.get("messageCount").asInt(),
                        record.get("producerCount").asInt(),
                        record.get("consumerCount").asInt());
            }

            System.out.println("[Neo4j Export] Created " + filename);
        } catch (Exception e) {
            System.err.println("[Neo4j Export] Failed: " + e.getMessage());
        }
    }

    /**
     * Export Neo4j Producers to CSV
     */
    public static void exportNeo4jProducersCSV() {
        exportNeo4jProducersCSV("neo4j_producers.csv");
    }

    public static void exportNeo4jProducersCSV(String filename) {
        try (Session session = Neo4jManager.getDriver().session();
                PrintWriter pw = new PrintWriter(filename)) {

            pw.println("producer_id,topicCount,totalMessages");

            Result result = session.run(
                    "MATCH (p:Producer)-[r:PUBLISHES_TO]->(t:Topic) " +
                            "RETURN p.id as producerId, COUNT(DISTINCT t) as topicCount, " +
                            "SUM(r.messageCount) as totalMessages " +
                            "ORDER BY topicCount DESC, totalMessages DESC");

            while (result.hasNext()) {
                Record record = result.next();
                pw.printf("%s,%d,%d%n",
                        escapeCSV(record.get("producerId").asString()),
                        record.get("topicCount").asInt(),
                        record.get("totalMessages").asInt());
            }

            System.out.println("[Neo4j Export] Created " + filename);
        } catch (Exception e) {
            System.err.println("[Neo4j Export] Failed: " + e.getMessage());
        }
    }

    /**
     * Export Neo4j Consumers to CSV
     */
    public static void exportNeo4jConsumersCSV() {
        exportNeo4jConsumersCSV("neo4j_consumers.csv");
    }

    public static void exportNeo4jConsumersCSV(String filename) {
        try (Session session = Neo4jManager.getDriver().session();
                PrintWriter pw = new PrintWriter(filename)) {

            pw.println("consumer_id,subscribedTopics");

            Result result = session.run(
                    "MATCH (c:Consumer)-[:SUBSCRIBES_TO]->(t:Topic) " +
                            "RETURN c.id as consumerId, COLLECT(t.name) as topics");

            while (result.hasNext()) {
                Record record = result.next();
                String topics = String.join(";", record.get("topics").asList(v -> v.asString()));
                pw.printf("%s,\"%s\"%n",
                        escapeCSV(record.get("consumerId").asString()),
                        topics);
            }

            System.out.println("[Neo4j Export] Created " + filename);
        } catch (Exception e) {
            System.err.println("[Neo4j Export] Failed: " + e.getMessage());
        }
    }

    /**
     * Export all Neo4j data to JSON
     */
    public static void exportNeo4jJSON() {
        exportNeo4jJSON("neo4j_data.json");
    }

    public static void exportNeo4jJSON(String filename) {
        try (Session session = Neo4jManager.getDriver().session();
                PrintWriter pw = new PrintWriter(filename)) {

            pw.println("{");
            pw.println("  \"exportDate\": \"" + LocalDateTime.now() + "\",");
            pw.println("  \"database\": \"JAVA_PROJECT\",");

            // Topics
            pw.println("  \"topics\": [");
            Result topicsResult = session
                    .run("MATCH (t:Topic) RETURN t.name as name, t.messageCount as count ORDER BY count DESC");
            boolean first = true;
            while (topicsResult.hasNext()) {
                if (!first)
                    pw.println(",");
                first = false;
                Record r = topicsResult.next();
                pw.printf("    {\"name\": \"%s\", \"messageCount\": %d}",
                        escapeJSON(r.get("name").asString()),
                        r.get("count").isNull() ? 0 : r.get("count").asInt());
            }
            pw.println("\n  ],");

            // Producers
            pw.println("  \"producers\": [");
            Result producersResult = session.run(
                    "MATCH (p:Producer)-[r:PUBLISHES_TO]->(t:Topic) " +
                            "RETURN p.id as id, COLLECT({topic: t.name, count: r.messageCount}) as topics");
            first = true;
            while (producersResult.hasNext()) {
                if (!first)
                    pw.println(",");
                first = false;
                Record r = producersResult.next();
                pw.printf("    {\"id\": \"%s\", \"publishesTo\": %s}",
                        escapeJSON(r.get("id").asString()),
                        r.get("topics").toString());
            }
            pw.println("\n  ],");

            // Consumers
            pw.println("  \"consumers\": [");
            Result consumersResult = session.run(
                    "MATCH (c:Consumer)-[:SUBSCRIBES_TO]->(t:Topic) " +
                            "RETURN c.id as id, COLLECT(t.name) as topics");
            first = true;
            while (consumersResult.hasNext()) {
                if (!first)
                    pw.println(",");
                first = false;
                Record r = consumersResult.next();
                pw.printf("    {\"id\": \"%s\", \"subscribesTo\": %s}",
                        escapeJSON(r.get("id").asString()),
                        r.get("topics").asList());
            }
            pw.println("\n  ]");

            pw.println("}");

            System.out.println("[Neo4j Export] Created " + filename);
        } catch (Exception e) {
            System.err.println("[Neo4j Export] Failed: " + e.getMessage());
        }
    }

    // =========================================================================
    // COMBINED BACKUPS (H2 + Neo4j)
    // =========================================================================

    /**
     * Create timestamped backup files (H2 CSV/JSON + Neo4j data)
     */
    public static void exportBackup() {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        // H2 backups
        String csvFile = "backup_" + timestamp + ".csv";
        String jsonFile = "backup_" + timestamp + ".json";
        exportCSV(csvFile);
        exportJSON(jsonFile);

        // Neo4j backups
        String neo4jTopics = "backup_neo4j_topics_" + timestamp + ".csv";
        String neo4jProducers = "backup_neo4j_producers_" + timestamp + ".csv";
        String neo4jConsumers = "backup_neo4j_consumers_" + timestamp + ".csv";
        String neo4jJson = "backup_neo4j_" + timestamp + ".json";

        exportNeo4jTopicsCSV(neo4jTopics);
        exportNeo4jProducersCSV(neo4jProducers);
        exportNeo4jConsumersCSV(neo4jConsumers);
        exportNeo4jJSON(neo4jJson);

        System.out.println("[Backup] Created full backup (H2 + Neo4j) with timestamp: " + timestamp);
    }

    /**
     * Export summary statistics from both databases
     */
    public static void exportStats() {
        try (Connection conn = DatabaseManager.getConnection();
                PrintWriter pw = new PrintWriter("stats.json")) {

            pw.println("{");
            pw.println("  \"exportDate\": \"" + LocalDateTime.now() + "\",");

            // H2 stats
            ResultSet rsTotal = conn.createStatement().executeQuery("SELECT COUNT(*) as total FROM messages");
            rsTotal.next();
            pw.println("  \"h2_totalMessages\": " + rsTotal.getInt("total") + ",");
            rsTotal.close();

            // H2 topic stats
            pw.println("  \"h2_topicStats\": [");
            ResultSet rsTopic = conn.createStatement().executeQuery(
                    "SELECT topic, COUNT(*) as count FROM messages GROUP BY topic ORDER BY count DESC");
            boolean first = true;
            while (rsTopic.next()) {
                if (!first)
                    pw.println(",");
                first = false;
                pw.printf("    {\"topic\": \"%s\", \"count\": %d}",
                        escapeJSON(rsTopic.getString("topic")),
                        rsTopic.getInt("count"));
            }
            rsTopic.close();
            pw.println("\n  ],");

            // Neo4j stats
            try (Session session = Neo4jManager.getDriver().session()) {
                Result neo4jStats = session.run(
                        "MATCH (t:Topic) RETURN COUNT(t) as topics " +
                                "UNION ALL MATCH (p:Producer) RETURN COUNT(p) as topics " +
                                "UNION ALL MATCH (c:Consumer) RETURN COUNT(c) as topics");

                int topics = 0, producers = 0, consumers = 0;
                if (neo4jStats.hasNext())
                    topics = neo4jStats.next().get("topics").asInt();
                if (neo4jStats.hasNext())
                    producers = neo4jStats.next().get("topics").asInt();
                if (neo4jStats.hasNext())
                    consumers = neo4jStats.next().get("topics").asInt();

                pw.println("  \"neo4j_topics\": " + topics + ",");
                pw.println("  \"neo4j_producers\": " + producers + ",");
                pw.println("  \"neo4j_consumers\": " + consumers);
            } catch (Exception e) {
                pw.println("  \"neo4j_error\": \"" + escapeJSON(e.getMessage()) + "\"");
            }

            pw.println("}");

            System.out.println("[Export] Created stats.json (H2 + Neo4j)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

    /**
     * Escape special characters for CSV
     */
    private static String escapeCSV(String value) {
        if (value == null)
            return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Escape special characters for JSON
     */
    private static String escapeJSON(String value) {
        if (value == null)
            return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}