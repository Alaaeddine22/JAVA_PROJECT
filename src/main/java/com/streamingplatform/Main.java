package com.streamingplatform;

import java.util.Scanner;

import com.streamingplatform.analysis.DataExporter;
import com.streamingplatform.net.SocketServer;
import com.streamingplatform.persistence.DatabaseManager;
import com.streamingplatform.persistence.MySQLManager;
import com.streamingplatform.persistence.Neo4jManager;
import com.streamingplatform.service.ArchiveService;
import com.streamingplatform.service.MySQLSyncService;
import com.streamingplatform.service.Neo4jSyncService;
import com.streamingplatform.service.Neo4jToMySQLSync;
import com.streamingplatform.ui.Dashboard;

public class Main {

    // Default retention period for message archiving (in minutes)
    private static final int DEFAULT_RETENTION_MINUTES = 5;

    public static void main(String[] args) {
        System.out.println("=== Starting Streaming Platform ===");

        // 1. Init H2 Database
        DatabaseManager.initializeDatabase();

        // 2. Init MySQL (XAMPP) for hybrid storage
        System.out.println("[Init] Connecting to MySQL (XAMPP)...");
        MySQLManager.initializeDatabase();

        // 3. Start Broker Server
        new SocketServer(8080).start();

        // 4. Start MySQL Sync Service
        MySQLSyncService mysqlSyncService = new MySQLSyncService();
        mysqlSyncService.start();

        // 5. Start Neo4j Sync Service (Topics, Producers, Consumers)
        Neo4jSyncService neo4jSyncService = new Neo4jSyncService();
        neo4jSyncService.start();

        // 6. Start Archive Service (Cleanup old messages)
        ArchiveService archiveService = new ArchiveService(DEFAULT_RETENTION_MINUTES);
        archiveService.start();

        // 7. Launch Dashboard UI
        Dashboard.launchApp();

        // 8. Console Commands
        printHelp();
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\n> ");
                String input = scanner.nextLine().trim().toLowerCase();

                switch (input) {
                    // H2 SQL Export Commands
                    case "export" -> DataExporter.exportCSV();
                    case "json" -> DataExporter.exportJSON();

                    // Neo4j Export Commands
                    case "neo4j-topics" -> DataExporter.exportNeo4jTopicsCSV();
                    case "neo4j-producers" -> DataExporter.exportNeo4jProducersCSV();
                    case "neo4j-consumers" -> DataExporter.exportNeo4jConsumersCSV();
                    case "neo4j-json" -> DataExporter.exportNeo4jJSON();

                    // Combined Backup (H2 + Neo4j)
                    case "backup" -> DataExporter.exportBackup();

                    // MySQL Stats
                    case "stats" -> System.out.println(mysqlSyncService.getTopProducerByTopicCount());

                    // Neo4j Query Commands
                    case "neo4j" -> System.out.println(neo4jSyncService.getNeo4jStats());
                    case "topics" -> System.out.println(neo4jSyncService.getTopics());

                    // Neo4j to MySQL Sync
                    case "sync-neo4j" -> Neo4jToMySQLSync.syncAll();
                    case "preview-neo4j" -> System.out.println(Neo4jToMySQLSync.previewNeo4jData());

                    // Maintenance Commands
                    case "cleanup" -> {
                        archiveService.cleanupNow();
                        System.out.println("[Manual] Cleanup triggered.");
                    }
                    case "help" -> printHelp();
                    case "exit" -> {
                        System.out.println("Shutting down...");
                        mysqlSyncService.stop();
                        neo4jSyncService.stop();
                        archiveService.stop();
                        MySQLManager.closeConnection();
                        Neo4jManager.closeConnection();
                        System.exit(0);
                    }
                    default -> System.out.println("Unknown command. Type 'help' for available commands.");
                }
            }
        }
    }

    private static void printHelp() {
        System.out.println("\n=== Available Commands ===");
        System.out.println("");
        System.out.println("  --- H2 SQL Export ---");
        System.out.println("  export          - Export H2 messages to CSV (nodes.csv)");
        System.out.println("  json            - Export H2 messages to JSON (messages.json)");
        System.out.println("");
        System.out.println("  --- Neo4j Graph Export ---");
        System.out.println("  neo4j-topics    - Export Topics from Neo4j to CSV");
        System.out.println("  neo4j-producers - Export Producers from Neo4j to CSV");
        System.out.println("  neo4j-consumers - Export Consumers from Neo4j to CSV");
        System.out.println("  neo4j-json      - Export all Neo4j data to JSON");
        System.out.println("");
        System.out.println("  --- Combined ---");
        System.out.println("  backup          - Full backup (H2 + Neo4j) with timestamp");
        System.out.println("");
        System.out.println("  --- Database Stats ---");
        System.out.println("  stats           - Which IP posts to most topics (MySQL)");
        System.out.println("  neo4j           - Which Producer posts to most topics (Neo4j)");
        System.out.println("  topics          - List all topics from Neo4j");
        System.out.println("");
        System.out.println("  --- Maintenance ---");
        System.out.println("  cleanup         - Delete old messages from H2");
        System.out.println("  help            - Show this help message");
        System.out.println("  exit            - Shutdown the platform");
    }
}