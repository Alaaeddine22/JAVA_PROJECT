package com.streamingplatform.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.streamingplatform.service.BrokerService;

public class SocketServer {
    private final int port;
    private final BrokerService brokerService;

    public SocketServer(int port) {
        this.port = port;
        this.brokerService = new BrokerService(); // Initialize Service
    }

    public void start() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("[Server] Broker listening on port " + port);
                while (true) {
                    Socket client = serverSocket.accept();
                    // FIXED: Now uses ClientWorker correctly
                    new Thread(new ClientWorker(client, brokerService)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
