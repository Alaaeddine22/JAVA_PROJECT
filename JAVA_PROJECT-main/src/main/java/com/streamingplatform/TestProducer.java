package com.streamingplatform;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

public class TestProducer {
    public static void main(String[] args) {
        System.out.println("--> [Realistic Simulation] Starting 1,000 message burst...");

        // 1. More Topics for diversity
        String[] topics = {
            "UserLogins", "PaymentEvents", "ClickStream", "SystemLogs", 
            "SecurityAlerts", "InventoryUpdates", "UserAnalytics", "ErrorLogs", 
            "BillingEvents", "FrontendTelemetry", "DatabaseHealth", "EmailService", "AuthEvents"
        };

        // 2. Multiple Producer IDs to create a complex web in Neo4j
        String[] producerIds = {
            "Auth-Service-01", "Payment-Gateway-A", "Mobile-App-Android", 
            "Web-Client-JS", "Cron-Job-Aggregator", "Inventory-Manager", 
            "Security-Audit-Node", "Legacy-Mainframe-Bridge"
        };

        Random rand = new Random();

        try (Socket socket = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            for (int i = 0; i < 1000; i++) {
                String topic = topics[rand.nextInt(topics.length)];
                String pId = producerIds[rand.nextInt(producerIds.length)]; // Randomized Producer
                String message = "Event_ID_" + i + "_Value_" + rand.nextInt(1000);
                
                // Format: PUBLISH <TOPIC> <PRODUCER_ID> <CONTENT>
                // This matches the parsing logic in ClientWorker.java
                String payload = "PUBLISH " + topic + " " + pId + " " + message;
                
                out.println(payload); 
                
                // Only print every 100th message to keep the console clean for the backup command
                if (i % 100 == 0) {
                    System.out.println("Progress: " + i + "/1000 messages sent...");
                }

                // 3. Faster speed (10ms) so 1000 messages finish in ~10 seconds
                Thread.sleep(10); 
            }
            
            System.out.println("--> SUCCESS: 1,000 messages sent");
            
        } catch (Exception e) {
            System.err.println("Connection failed! Is Main.java running? " + e.getMessage());
        }
    }
}