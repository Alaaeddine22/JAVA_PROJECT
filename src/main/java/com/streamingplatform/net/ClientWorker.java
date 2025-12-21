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
                // Protocol: PUBLISH <TOPIC> <PRODUCER_ID> <CONTENT>
                if (line.startsWith("PUBLISH")) {
                    String[] parts = line.split(" ", 4);
                    if (parts.length == 4) {
                        service.publish(parts[1], parts[2], parts[3]);
                        out.println("ACK");
                    }
                }
            }
        } catch (IOException e) {
            // Client disconnected
        }
    }
}