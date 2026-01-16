package com.streamingplatform.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.streamingplatform.service.BrokerService;

public class ClientWorker implements Runnable {
    private final Socket socket;
    private final BrokerService service;

    public ClientWorker(Socket socket, BrokerService service) {
        this.socket = socket;
        this.service = service;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) continue;

                // We went for better splitting to handle multiple spaces
                String[] parts = trimmedLine.split("\\s+", 4);
                if (parts.length == 0) continue;

                String command = parts[0].toUpperCase();

                if (command.equals("PUBLISH")) {
                    // --- PUBLISH LOGIC (Matches your InteractiveClient) ---
                    if (parts.length == 4) {
                        service.publish(parts[1], parts[2], parts[3]);
                        out.println("ACK");
                    } else {
                        out.println("ERROR: Usage: PUBLISH <TOPIC> <PRODUCER_ID> <CONTENT>");
                    }

                } else if (command.equals("CONSUME")) {
                    // --- FIX: PULL LOGIC ---
                    if (parts.length >= 2) {
                        String topic = parts[1];
                        var messages = service.consume(topic); 
                        
                        if (messages.isEmpty()) {
                            out.println("EMPTY");
                        } else {
                            for (String msg : messages) {
                                out.println("MSG " + msg);
                            }
                            out.println("END_OF_BATCH");
                        }
                    } else {
                        out.println("ERROR: Usage: CONSUME <TOPIC>");
                    }

                } else {
                    out.println("ERROR: Unknown command");
                }
            }
        } catch (IOException e) {
            System.out.println("[Server] Client disconnected: " + socket.getInetAddress());
        }
    }
}