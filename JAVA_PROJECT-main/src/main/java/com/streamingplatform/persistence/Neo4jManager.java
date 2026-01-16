package com.streamingplatform.persistence;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import org.neo4j.driver.Record;

import java.util.HashMap;
import java.util.Map;

/**
 * Neo4j Database Manager for graph-based storage of Topics, Producers, and
 * Consumers.
 * Database: JAVA_PROJECT
 */
public class Neo4jManager {

    private static final String URI = "neo4j://127.0.0.1:7687";
    private static final String DATABASE = "neo4j";
    private static final String USER = "neo4j";
    private static final String PASSWORD = "ALAA2004@";

    private static Driver driver;

    /**
     * Get Neo4j driver (singleton)
     */
    public static Driver getDriver() {
        if (driver == null) {
            driver = GraphDatabase.driver(URI, AuthTokens.basic(USER, PASSWORD));
        }
        return driver;
    }

    /**
     * Initialize Neo4j database with constraints and indexes
     */
    public static void initializeDatabase() {
        try (Session session = getDriver().session()) {
            // Create constraints for unique nodes
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (t:Topic) REQUIRE t.name IS UNIQUE");
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (p:Producer) REQUIRE p.id IS UNIQUE");
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (c:Consumer) REQUIRE c.id IS UNIQUE");

            System.out.println("[Neo4j] Database initialized successfully.");
        } catch (Exception e) {
            System.err.println("[Neo4j] Failed to initialize: " + e.getMessage());
        }
    }

    /**
     * Create or update a Topic node
     */
    public static void upsertTopic(String topicName, int messageCount) {
        try (Session session = getDriver().session()) {
            Map<String, Object> params = new HashMap<>();
            params.put("name", topicName);
            params.put("messageCount", messageCount);

            session.run(
                    "MERGE (t:Topic {name: $name}) " +
                            "SET t.messageCount = $messageCount, t.lastUpdated = datetime()",
                    params);
        } catch (Exception e) {
            System.err.println("[Neo4j] Error upserting topic: " + e.getMessage());
        }
    }

    /**
     * Create or update a Producer node
     */
    public static void upsertProducer(String producerId) {
        try (Session session = getDriver().session()) {
            Map<String, Object> params = new HashMap<>();
            params.put("id", producerId);

            session.run(
                    "MERGE (p:Producer {id: $id}) " +
                            "SET p.lastSeen = datetime()",
                    params);
        } catch (Exception e) {
            System.err.println("[Neo4j] Error upserting producer: " + e.getMessage());
        }
    }

    /**
     * Create or update a Consumer node
     */
    public static void upsertConsumer(String consumerId) {
        try (Session session = getDriver().session()) {
            Map<String, Object> params = new HashMap<>();
            params.put("id", consumerId);

            session.run(
                    "MERGE (c:Consumer {id: $id}) " +
                            "SET c.lastSeen = datetime()",
                    params);
        } catch (Exception e) {
            System.err.println("[Neo4j] Error upserting consumer: " + e.getMessage());
        }
    }

    /**
     * Create PUBLISHES_TO relationship between Producer and Topic
     */
    public static void createPublishesRelation(String producerId, String topicName, int messageCount) {
        try (Session session = getDriver().session()) {
            Map<String, Object> params = new HashMap<>();
            params.put("producerId", producerId);
            params.put("topicName", topicName);
            params.put("messageCount", messageCount);

            session.run(
                    "MATCH (p:Producer {id: $producerId}) " +
                            "MATCH (t:Topic {name: $topicName}) " +
                            "MERGE (p)-[r:PUBLISHES_TO]->(t) " +
                            "SET r.messageCount = $messageCount, r.lastUpdated = datetime()",
                    params);
        } catch (Exception e) {
            System.err.println("[Neo4j] Error creating PUBLISHES_TO relation: " + e.getMessage());
        }
    }

    /**
     * Create SUBSCRIBES_TO relationship between Consumer and Topic
     */
    public static void createSubscribesRelation(String consumerId, String topicName) {
        try (Session session = getDriver().session()) {
            Map<String, Object> params = new HashMap<>();
            params.put("consumerId", consumerId);
            params.put("topicName", topicName);

            session.run(
                    "MATCH (c:Consumer {id: $consumerId}) " +
                            "MATCH (t:Topic {name: $topicName}) " +
                            "MERGE (c)-[r:SUBSCRIBES_TO]->(t) " +
                            "SET r.lastUpdated = datetime()",
                    params);
        } catch (Exception e) {
            System.err.println("[Neo4j] Error creating SUBSCRIBES_TO relation: " + e.getMessage());
        }
    }

    /**
     * Query: Which Producer posts to the most topics?
     */
    public static String getTopProducerByTopicCount() {
        StringBuilder result = new StringBuilder();
        result.append("\n=== Neo4j: Which Producer posts to the most topics? ===\n");

        try (Session session = getDriver().session()) {
            Result rs = session.run(
                    "MATCH (p:Producer)-[r:PUBLISHES_TO]->(t:Topic) " +
                            "RETURN p.id as producer, COUNT(DISTINCT t) as topicCount, SUM(r.messageCount) as totalMessages "
                            +
                            "ORDER BY topicCount DESC, totalMessages DESC " +
                            "LIMIT 10");

            result.append(String.format("%-20s | %-12s | %-15s%n", "Producer", "Topics", "Total Messages"));
            result.append("-".repeat(52)).append("\n");

            while (rs.hasNext()) {
                Record record = rs.next();
                result.append(String.format("%-20s | %-12d | %-15d%n",
                        record.get("producer").asString(),
                        record.get("topicCount").asInt(),
                        record.get("totalMessages").asInt()));
            }
        } catch (Exception e) {
            result.append("Error: Neo4j not available - ").append(e.getMessage());
        }

        return result.toString();
    }

    /**
     * Get all topics with message counts
     */
    public static String getAllTopics() {
        StringBuilder result = new StringBuilder();
        result.append("\n=== Neo4j: All Topics ===\n");

        try (Session session = getDriver().session()) {
            Result rs = session.run(
                    "MATCH (t:Topic) " +
                            "OPTIONAL MATCH (p:Producer)-[r:PUBLISHES_TO]->(t) " +
                            "RETURN t.name as topic, t.messageCount as count, COUNT(DISTINCT p) as producers " +
                            "ORDER BY count DESC");

            result.append(String.format("%-20s | %-12s | %-10s%n", "Topic", "Messages", "Producers"));
            result.append("-".repeat(46)).append("\n");

            while (rs.hasNext()) {
                Record record = rs.next();
                result.append(String.format("%-20s | %-12d | %-10d%n",
                        record.get("topic").asString(),
                        record.get("count").isNull() ? 0 : record.get("count").asInt(),
                        record.get("producers").asInt()));
            }
        } catch (Exception e) {
            result.append("Error: ").append(e.getMessage());
        }

        return result.toString();
    }

    /**
     * Close Neo4j connection
     */
    public static void closeConnection() {
        if (driver != null) {
            driver.close();
            driver = null;
        }
    }
}
