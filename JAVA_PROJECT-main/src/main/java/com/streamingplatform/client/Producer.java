package com.streamingplatform.client;

import java.io.PrintWriter;
import java.net.Socket;

public class Producer {
    public void send(String topic, String content) {
        // Connects to the server we defined above
        try (Socket socket = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            
            // The protocol we chose: PUBLISH <TOPIC> <PRODUCER_ID> <CONTENT>
            String command = String.format("PUBLISH %s %s %s", topic, "Client-1", content);
            out.println(command);
            System.out.println("[Producer] Sent: " + content);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}