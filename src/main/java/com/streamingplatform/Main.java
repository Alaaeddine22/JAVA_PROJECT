package com.streamingplatform;

import java.util.Scanner;

import com.streamingplatform.analysis.DataExporter;
import com.streamingplatform.net.SocketServer;
import com.streamingplatform.persistence.DatabaseManager;
import com.streamingplatform.ui.Dashboard;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Starting Streaming Platform ===");

        // 1. Init DB
        DatabaseManager.initializeDatabase();

        // 2. Start Broker
        new SocketServer(8080).start();

        // 3. Launch UI
        Dashboard.launchApp();
        
        try (// 4. Console Commands
        Scanner scanner = new Scanner(System.in)) {
            while(true) {
                System.out.println("Type 'export' for Neo4j CSV, or 'exit':");
                String input = scanner.nextLine();
                if(input.equals("export")) DataExporter.exportCSV();
                if(input.equals("exit")) System.exit(0);
            }
        }
    }
}