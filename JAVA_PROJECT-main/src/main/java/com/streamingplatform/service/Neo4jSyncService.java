package com.streamingplatform.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Timer;
import java.util.TimerTask;

import com.streamingplatform.persistence.DatabaseManager;
import com.streamingplatform.persistence.Neo4jManager;

/**
 * Background service that syncs Topics, Producers, and Consumers from H2 to
 * Neo4j.
 * Creates graph relationships for analysis.
 */
public class Neo4jSyncService {

    private Timer syncTimer;
    private static final long SYNC_INTERVAL_MS = 500; // Sync every 5 seconds

    /**
     * Start the background sync service
     */
    public void start() {
        System.out.println("[Neo4jSync] Starting background sync service...");

        // Initialize Neo4j database
        Neo4jManager.initializeDatabase();

        syncTimer = new Timer(true); // Daemon thread
        syncTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                syncToNeo4j();
            }
        }, 1000, SYNC_INTERVAL_MS);
    }

    /**
     * Stop the sync service
     */
    public void stop() {
        if (syncTimer != null) {
            syncTimer.cancel();
            System.out.println("[Neo4jSync] Sync service stopped.");
        }
        Neo4jManager.closeConnection();
    }

    /**
     * Sync H2 data to Neo4j
     */
    private void syncToNeo4j() {
        try {
            Connection h2Conn = DatabaseManager.getConnection();

            // 1. Sync Topics
            syncTopics(h2Conn);

            // 2. Sync Producers and their relationships
            syncProducers(h2Conn);

            // 3. Sync Consumers (create sample consumers based on topics)
            syncConsumers();

        } catch (Exception e) {
            // Silently handle if Neo4j is not available
            // System.err.println("[Neo4jSync] Sync failed: " + e.getMessage());
        }
    }

    /**
     * Sync Topics from H2 to Neo4j
     */
    private void syncTopics(Connection h2Conn) throws Exception {
        String query = "SELECT topic, COUNT(*) as cnt FROM messages GROUP BY topic";
        ResultSet rs = h2Conn.createStatement().executeQuery(query);

        int synced = 0;
        while (rs.next()) {
            String topicName = rs.getString("topic");
            int messageCount = rs.getInt("cnt");
            Neo4jManager.upsertTopic(topicName, messageCount);
            synced++;
        }
        rs.close();

        if (synced > 0) {
            System.out.println("[Neo4jSync] Synced " + synced + " topics.");
        }
    }

    /**
     * Sync Producers and their PUBLISHES_TO relationships
     */
    private void syncProducers(Connection h2Conn) throws Exception {
        // Get unique producers
        String producerQuery = "SELECT DISTINCT producer_id FROM messages WHERE producer_id IS NOT NULL";
        ResultSet rsProducers = h2Conn.createStatement().executeQuery(producerQuery);

        while (rsProducers.next()) {
            String producerId = rsProducers.getString("producer_id");
            Neo4jManager.upsertProducer(producerId);
        }
        rsProducers.close();

        // Get producer-topic relationships with counts
        String relQuery = """
                    SELECT producer_id, topic, COUNT(*) as msg_count
                    FROM messages
                    WHERE producer_id IS NOT NULL
                    GROUP BY producer_id, topic
                """;
        ResultSet rsRel = h2Conn.createStatement().executeQuery(relQuery);

        int synced = 0;
        while (rsRel.next()) {
            String producerId = rsRel.getString("producer_id");
            String topicName = rsRel.getString("topic");
            int messageCount = rsRel.getInt("msg_count");

            Neo4jManager.createPublishesRelation(producerId, topicName, messageCount);
            synced++;
        }
        rsRel.close();

        if (synced > 0) {
            System.out.println("[Neo4jSync] Synced " + synced + " producer-topic relationships.");
        }
    }

    /**
     * Create sample Consumers that subscribe to topics
     */
    private void syncConsumers() {
        // Create sample consumers
        String[] consumers = { "Fraud-Detection-Service", "RealTime-Analytics-Engine", "ML-Recommendation-System",
    "Data-Lake-Ingestor", "Elastic-Search-Indexer", "Security-Audit-Manager",
    "Customer-Notification-Hub", "Global-Billing-System", "Inventory-Replenishment-Bot",
    "Shipping-Tracking-Service", "Marketing-Automation-Tool", "Email-Service-Provider",
    "User-Profile-Manager", "Legacy-System-Bridge", "Cold-Storage-Archiver",
    "Spark-Streaming-Analytics", "Operational-Dashboard-UI", "Executive-Reporting-Tool",
    "Slack-Alert-Integration", "Webhook-Dispatcher-Service", "Mobile-Push-Notification",
    "Partner-Sync-Service", "Compliance-Verification-Node", "DDoS-Mitigation-Service",
    "Log-Aggregator-Splunk", "Database-Backup-Manager", "Performance-Monitoring-Node",
    "Order-Fulfillment-Service", "Refund-Processing-Unit", "Support-Ticket-AutoRouter" };
    
        String[] subscribedTopics = { "UserLogins", "PaymentEvents", "ClickStream", "SystemLogs", "SecurityAlerts",
        "InventoryUpdates", "UserAnalytics", "ErrorLogs", "BillingEvents", "FrontendTelemetry",
        "DatabaseHealth", "EmailService", "AuthEvents", "OrderProcessing", "ShippingUpdates",
        "RefundRequests", "CustomerFeedback", "PromotionalEmails", "PasswordResets", "ApiGatewayLogs",
        "MobileAppEvents", "DesktopAppEvents", "PartnerIntegration", "CloudSyncEvents", "BackupStatus",
        "CacheInvalidations", "SessionExpirations", "SearchQueries", "ProductViews", "CartAdditions",
        "CheckoutStarted", "PaymentSuccess", "PaymentFailure", "CouponApplied", "ReviewSubmitted",
        "AccountCreated", "AccountDeleted", "NewsletterSub", "NewsletterUnsub", "SupportTickets",
        "LiveChatLogs", "InternalAudit", "ComplianceLogs", "FraudDetection", "MachineLearningInference" };

        for (int i = 0; i < consumers.length; i++) {
            Neo4jManager.upsertConsumer(consumers[i]);

            // Each consumer subscribes to some topics
            if (i < subscribedTopics.length) {
                Neo4jManager.createSubscribesRelation(consumers[i], subscribedTopics[i]);
            }
            // Some consumers subscribe to multiple topics
            if (i == 0) { // Dashboard subscribes to all
                for (String topic : subscribedTopics) {
                    Neo4jManager.createSubscribesRelation(consumers[i], topic);
                }
            }
        }
    }

    /**
     * Get Neo4j statistics
     */
    public String getNeo4jStats() {
        return Neo4jManager.getTopProducerByTopicCount();
    }

    /**
     * Get all topics from Neo4j
     */
    public String getTopics() {
        return Neo4jManager.getAllTopics();
    }
}
