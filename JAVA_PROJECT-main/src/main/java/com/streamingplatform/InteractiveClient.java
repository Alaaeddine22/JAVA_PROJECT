package com.streamingplatform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class InteractiveClient {

    public static void main(String[] args) {
        System.out.println("=== Interactive Producer Client ===");
        System.out.println("Connects to Broker at localhost:8080");
        System.out.println("Format: PUBLISH <TOPIC> <PRODUCER_ID> <CONTENT>");
        System.out.println("Example: PUBLISH news reporter1 Breaking News!");
        System.out.println("Type 'exit' to quit.");
        System.out.println("------------------------------------------------");

        try (Socket socket = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected! Start typing commands:");

            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine();

                if ("exit".equalsIgnoreCase(input.trim())) {
                    break;
                }

                // Send to Server
                out.println(input);

                // Read Server Response (ACK or ERROR)
                String response = in.readLine();
                System.out.println("[Broker Response]: " + response);
            }

        } catch (IOException e) {
            System.err.println("Connection Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}