package com.streamingplatform.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import com.streamingplatform.persistence.MySQLManager;
import com.streamingplatform.persistence.Neo4jManager;

/**
 * Service to sync data FROM Neo4j TO MySQL.
 * Reads Topics, Producers, Consumers, Partitions, Consumer Groups from Neo4j
 * and writes to MySQL tables.
 */
public class Neo4jToMySQLSync {

    /**
     * Sync all Neo4j data to MySQL
     */
    public static void syncAll() {
        System.out.println("[Neo4j→MySQL] Starting full sync...");

        try {
            // Clear existing MySQL data first
            clearMySQLTables();

            // Sync all entities
            int topics = syncTopics();
            int producers = syncProducers();
            int consumers = syncConsumers();
            int partitions = syncPartitions();
            int groups = syncConsumerGroups();
            int relations = syncProducerTopicStats();

            System.out.println("[Neo4j→MySQL] Sync completed!");
            System.out.println("  Topics: " + topics);
            System.out.println("  Producers: " + producers);
            System.out.println("  Consumers: " + consumers);
            System.out.println("  Partitions: " + partitions);
            System.out.println("  Consumer Groups: " + groups);
            System.out.println("  Producer-Topic Relations: " + relations);

        } catch (Exception e) {
            System.err.println("[Neo4j→MySQL] Sync failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Clear existing MySQL tables before sync
     */
    private static void clearMySQLTables() throws Exception {
        Connection mysqlConn = MySQLManager.getConnection();
        Statement stmt = mysqlConn.createStatement();

        stmt.executeUpdate("DELETE FROM ip_topic_stats");
        stmt.executeUpdate("DELETE FROM topics");
        stmt.executeUpdate("DELETE FROM producers");
        stmt.executeUpdate("DELETE FROM consumers");
        stmt.executeUpdate("DELETE FROM partitions");
        stmt.executeUpdate("DELETE FROM consumer_groups");

        stmt.close();
        System.out.println("[Neo4j→MySQL] Cleared existing MySQL data.");
    }

    /**
     * Sync Topics from Neo4j to MySQL
     */
    private static int syncTopics() throws Exception {
        Connection mysqlConn = MySQLManager.getConnection();
        String insertSQL = "INSERT INTO topics (name, message_count) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE message_count = VALUES(message_count)";
        PreparedStatement ps = mysqlConn.prepareStatement(insertSQL);

        int count = 0;
        try (Session session = Neo4jManager.getDriver().session()) {
            Result result = session.run("MATCH (t:Topic) RETURN t.name as name, t.messageCount as messageCount");
            while (result.hasNext()) {
                Record record = result.next();
                ps.setString(1, record.get("name").asString());
                ps.setInt(2, record.get("messageCount").isNull() ? 0 : record.get("messageCount").asInt());
                ps.executeUpdate();
                count++;
            }
        }
        ps.close();
        return count;
    }

    /**
     * Sync Producers from Neo4j to MySQL
     */
    private static int syncProducers() throws Exception {
        Connection mysqlConn = MySQLManager.getConnection();
        String insertSQL = "INSERT INTO producers (producer_id) VALUES (?) ON DUPLICATE KEY UPDATE producer_id = VALUES(producer_id)";
        PreparedStatement ps = mysqlConn.prepareStatement(insertSQL);

        int count = 0;
        try (Session session = Neo4jManager.getDriver().session()) {
            Result result = session.run("MATCH (p:producer) RETURN p.id as producerId");
            while (result.hasNext()) {
                Record record = result.next();
                ps.setString(1, record.get("producerId").asString());
                ps.executeUpdate();
                count++;
            }
        }
        ps.close();
        return count;
    }

    /**
     * Sync Consumers from Neo4j to MySQL
     */
    private static int syncConsumers() throws Exception {
        Connection mysqlConn = MySQLManager.getConnection();
        String insertSQL = "INSERT INTO consumers (consumer_id, consumer_group) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE consumer_group = VALUES(consumer_group)";
        PreparedStatement ps = mysqlConn.prepareStatement(insertSQL);

        int count = 0;
        try (Session session = Neo4jManager.getDriver().session()) {
            // Try to get consumer with group relationship
            Result result = session.run(
                    "MATCH (c:consumer) " +
                            "OPTIONAL MATCH (c)-[:BELONGS_TO]->(g:consumer_group) " +
                            "RETURN c.id as consumerId, g.id as groupId");
            while (result.hasNext()) {
                Record record = result.next();
                ps.setString(1, record.get("consumerId").asString());
                ps.setString(2, record.get("groupId").isNull() ? null : record.get("groupId").asString());
                ps.executeUpdate();
                count++;
            }
        }
        ps.close();
        return count;
    }

    /**
     * Sync Partitions from Neo4j to MySQL
     */
    private static int syncPartitions() throws Exception {
        Connection mysqlConn = MySQLManager.getConnection();
        String insertSQL = "INSERT INTO partitions (partition_id, topic, leader, replicas) VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE topic = VALUES(topic), leader = VALUES(leader), replicas = VALUES(replicas)";
        PreparedStatement ps = mysqlConn.prepareStatement(insertSQL);

        int count = 0;
        try (Session session = Neo4jManager.getDriver().session()) {
            Result result = session.run(
                    "MATCH (p:partition) " +
                            "OPTIONAL MATCH (p)-[:BELONGS_TO]->(t:Topic) " +
                            "RETURN p.id as partitionId, t.name as topic, p.leader as leader, p.replicas as replicas");
            while (result.hasNext()) {
                Record record = result.next();
                ps.setString(1, record.get("partitionId").asString());
                ps.setString(2, record.get("topic").isNull() ? "" : record.get("topic").asString());
                ps.setString(3, record.get("leader").isNull() ? "" : record.get("leader").asString());
                ps.setInt(4, record.get("replicas").isNull() ? 1 : record.get("replicas").asInt());
                ps.executeUpdate();
                count++;
            }
        }
        ps.close();
        return count;
    }

    /**
     * Sync Consumer Groups from Neo4j to MySQL
     */
    private static int syncConsumerGroups() throws Exception {
        Connection mysqlConn = MySQLManager.getConnection();
        String insertSQL = "INSERT INTO consumer_groups (group_id, member_count) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE member_count = VALUES(member_count)";
        PreparedStatement ps = mysqlConn.prepareStatement(insertSQL);

        int count = 0;
        try (Session session = Neo4jManager.getDriver().session()) {
            Result result = session.run(
                    "MATCH (g:consumer_group) " +
                            "OPTIONAL MATCH (c:consumer)-[:BELONGS_TO]->(g) " +
                            "RETURN g.id as groupId, COUNT(c) as memberCount");
            while (result.hasNext()) {
                Record record = result.next();
                ps.setString(1, record.get("groupId").asString());
                ps.setInt(2, (int) record.get("memberCount").asLong());
                ps.executeUpdate();
                count++;
            }
        }
        ps.close();
        return count;
    }

    /**
     * Sync Producer-Topic relationships from Neo4j to MySQL
     */
    private static int syncProducerTopicStats() throws Exception {
        Connection mysqlConn = MySQLManager.getConnection();
        String insertSQL = "INSERT INTO ip_topic_stats (producer_id, topic, message_count) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE message_count = VALUES(message_count)";
        PreparedStatement ps = mysqlConn.prepareStatement(insertSQL);

        int count = 0;
        try (Session session = Neo4jManager.getDriver().session()) {
            Result result = session.run(
                    "MATCH (p:producer)-[r:PUBLISHES_TO]->(t:Topic) " +
                            "RETURN p.id as producerId, t.name as topic, r.messageCount as messageCount");
            while (result.hasNext()) {
                Record record = result.next();
                ps.setString(1, record.get("producerId").asString());
                ps.setString(2, record.get("topic").asString());
                ps.setInt(3, record.get("messageCount").isNull() ? 0 : record.get("messageCount").asInt());
                ps.executeUpdate();
                count++;
            }
        }
        ps.close();
        return count;
    }

    /**
     * Display current Neo4j data that will be synced
     */
    public static String previewNeo4jData() {
        StringBuilder result = new StringBuilder();
        result.append("\n=== Neo4j Data Preview (will be synced to MySQL) ===\n\n");

        try (Session session = Neo4jManager.getDriver().session()) {
            // Count entities
            Result countResult = session.run(
                    "MATCH (t:Topic) WITH COUNT(t) as topics " +
                            "OPTIONAL MATCH (p:producer) WITH topics, COUNT(p) as producers " +
                            "OPTIONAL MATCH (c:consumer) WITH topics, producers, COUNT(c) as consumers " +
                            "OPTIONAL MATCH (pt:partition) WITH topics, producers, consumers, COUNT(pt) as partitions "
                            +
                            "OPTIONAL MATCH (g:consumer_group) " +
                            "RETURN topics, producers, consumers, partitions, COUNT(g) as groups");

            if (countResult.hasNext()) {
                Record r = countResult.next();
                result.append("Entity Counts:\n");
                result.append("  Topics: ").append(r.get("topics").asInt()).append("\n");
                result.append("  Producers: ").append(r.get("producers").asInt()).append("\n");
                result.append("  Consumers: ").append(r.get("consumers").asInt()).append("\n");
                result.append("  Partitions: ").append(r.get("partitions").asInt()).append("\n");
                result.append("  Consumer Groups: ").append(r.get("groups").asInt()).append("\n");
            }

            // Sample Topics
            result.append("\nSample Topics (first 10):\n");
            Result topics = session.run("MATCH (t:Topic) RETURN t.name as name LIMIT 10");
            while (topics.hasNext()) {
                result.append("  - ").append(topics.next().get("name").asString()).append("\n");
            }

            // Sample Producers
            result.append("\nSample Producers (first 10):\n");
            Result producers = session.run("MATCH (p:producer) RETURN p.id as id LIMIT 10");
            while (producers.hasNext()) {
                result.append("  - ").append(producers.next().get("id").asString()).append("\n");
            }

            // Sample Consumers
            result.append("\nSample Consumers (first 10):\n");
            Result consumers = session.run("MATCH (c:consumer) RETURN c.id as id LIMIT 10");
            while (consumers.hasNext()) {
                result.append("  - ").append(consumers.next().get("id").asString()).append("\n");
            }

        } catch (Exception e) {
            result.append("Error reading Neo4j: ").append(e.getMessage());
        }

        return result.toString();
    }
}
