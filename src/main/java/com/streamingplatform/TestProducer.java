package com.streamingplatform;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

public class TestProducer {
    public static void main(String[] args) {
        System.out.println("--> Producer starting... sending messages to port 8080");

        String[] topics = {"UserLogins", "PaymentEvents", "ClickStream", "SystemLogs"};
        Random rand = new Random();

        try {
            // Connect to your running Broker
            Socket socket = new Socket("localhost", 8080);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Send 100 fake messages
            for (int i = 0; i < 100; i++) {
                String topic = topics[rand.nextInt(topics.length)];
                String message = "Event_ID_" + i + "_Value_" + rand.nextInt(1000);
                
                // Format depends on how your SocketServer parses data. 
                // Assuming standard "TOPIC|CONTENT" or similar. 
                // If your server expects JSON, change this line.
                String payload = topic + "|" + message; 
                
                out.println(payload); 
                System.out.println("Sent: " + payload);
                
                Thread.sleep(500); // Wait 0.5 seconds between messages
            }
            
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}