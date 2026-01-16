package com.streamingplatform.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Consumer {
    public static void main(String[] args) {
        System.out.println("=== Consumer Client Started ===");
        
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter Topic to Subscribe (e.g., news): ");
            String topic = scanner.nextLine();

            // Connect to Broker
            try (Socket socket = new Socket("localhost", 8080);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                System.out.println("Connected. Polling topic: " + topic);
                System.out.println("------------------------------------------------");

                // POLLING LOOP
                while (true) {
                    // 1. Send Pull Request
                    out.println("CONSUME " + topic);

                    // 2. Read Response
                    String line;
                    boolean hasData = false;
                    while ((line = in.readLine()) != null) {
                        if (line.equals("END_OF_BATCH") || line.equals("EMPTY") || line.startsWith("ERROR")) {
                            break; 
                        }
                        if (line.startsWith("MSG")) {
                            // Print the message content
                            System.out.println("Received: " + line.substring(4)); 
                            hasData = true;
                        }
                    }

                    if (hasData) {
                        System.out.println("--- Batch Complete ---");
                    } else {
                        System.out.print("."); // Loading dots
                    }

                    // 3. Wait 2 seconds before checking again
                    Thread.sleep(2000); 
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}