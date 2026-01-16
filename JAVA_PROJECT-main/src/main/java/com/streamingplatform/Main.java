package com.streamingplatform;

import java.util.Scanner;

import com.streamingplatform.analysis.DataExporter;
import com.streamingplatform.net.SocketServer;
import com.streamingplatform.persistence.DatabaseManager;
import com.streamingplatform.persistence.MessageDAO;
import com.streamingplatform.persistence.MySQLManager;
import com.streamingplatform.persistence.Neo4jManager;
import com.streamingplatform.service.BrokerService;
import com.streamingplatform.service.MySQLSyncService;
import com.streamingplatform.service.Neo4jSyncService;
import com.streamingplatform.service.Neo4jToMySQLSync;
import com.streamingplatform.ui.Dashboard;

public class Main {

    // Default retention period for message archiving (in minutes)
    // private static final int DEFAULT_RETENTION_MINUTES = 5;

    public static void main(String[] args) {
        System.out.println("=== Starting Streaming Platform ===");

        // 1. Init Databases
        DatabaseManager.initializeDatabase(); // H2
        
        System.out.println("[Init] Connecting to MySQL (WAMP)...");
        MySQLManager.initializeDatabase(); // WAMP MySQL

        // 2. WIRE DEPENDENCIES (This was the missing part)
        MessageDAO messageDAO = new MessageDAO();
        BrokerService brokerService = new BrokerService(messageDAO);

        // 3. Start Broker Server (Pass the service!)
        new SocketServer(8080, brokerService).start();

        // 4. Start Background Services
        MySQLSyncService mysqlSyncService = new MySQLSyncService();
        mysqlSyncService.start();

        Neo4jSyncService neo4jSyncService = new Neo4jSyncService();
        neo4jSyncService.start();

        // ArchiveService archiveService = new ArchiveService(DEFAULT_RETENTION_MINUTES);
        // archiveService.start();

        // 5. Launch Dashboard UI
        new Thread(() -> Dashboard.launchApp()).start();

        // 6. Console Commands
        printHelp();
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\n> ");
                String input = scanner.nextLine().trim().toLowerCase();

                switch (input) {
                    case "export" -> DataExporter.exportCSV();
                    case "json" -> DataExporter.exportJSON();
                    case "neo4j-topics" -> DataExporter.exportNeo4jTopicsCSV();
                    case "neo4j-producers" -> DataExporter.exportNeo4jProducersCSV();
                    case "neo4j-consumers" -> DataExporter.exportNeo4jConsumersCSV();
                    case "neo4j-json" -> DataExporter.exportNeo4jJSON();
                    case "backup" -> DataExporter.exportBackup();
                    case "stats" -> System.out.println(mysqlSyncService.getTopProducerByTopicCount());
                    case "neo4j" -> System.out.println(neo4jSyncService.getNeo4jStats());
                    case "topics" -> System.out.println(neo4jSyncService.getTopics());
                    case "sync-neo4j" -> Neo4jToMySQLSync.syncAll();
                    case "preview-neo4j" -> System.out.println(Neo4jToMySQLSync.previewNeo4jData());
                    
                    /* DISABLED FOR ANALYSIS IN NEO4J
                    case "cleanup" -> {
                        archiveService.cleanupNow();
                        System.out.println("[Manual] Cleanup triggered.");
                    }
                    */
                    case "help" -> printHelp();
                    case "exit" -> {
                        System.out.println("Shutting down...");
                        mysqlSyncService.stop();
                        neo4jSyncService.stop();
                        //archiveService.stop();
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
        System.out.println("  backup          - Full backup (H2 + Neo4j)");
        System.out.println("  sync-neo4j      - Sync Neo4j to MySQL");
        System.out.println("  stats           - Show stats");
        System.out.println("  exit            - Shutdown");
    }
}