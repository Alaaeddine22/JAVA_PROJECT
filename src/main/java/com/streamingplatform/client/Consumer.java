package com.streamingplatform.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Consumer {
    public void consume(String topic) {
        try (Socket socket = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            // Send a custom CONSUME command (You'll need to update ClientWorker to handle this later if you want real consumption)
            // For now, we will just use it to test connectivity
            out.println("CONSUME " + topic);
            
            String response = in.readLine();
            System.out.println("[Consumer] Received: " + response);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}