package com.streamingplatform.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.streamingplatform.service.BrokerService;

public class SocketServer {
    private final int port;
    private final BrokerService brokerService;
    // FIX 1: Defining a Thread Pool (here we chose 50 concurrent threads)
    private final ExecutorService threadPool = Executors.newFixedThreadPool(50); 

    public SocketServer(int port, BrokerService brokerService) { 
        this.port = port;
        this.brokerService = brokerService;
    }

    public void start() {
        // We will run the server loop in a separate thread so it doesn't block Main
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("[Server] Broker listening on port " + port);
                while (true) {
                    Socket client = serverSocket.accept();
                    // FIX 3: Submitting task to pool instead of "new Thread(...).start()"
                    threadPool.submit(new ClientWorker(client, brokerService));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}