package com.streamingplatform;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class InteractiveClient {
    public static void main(String[] args) {
        System.out.println(">>> CLIENT APP STARTED <<<");
        System.out.println("Instructions: Type 'TopicName|Message' and hit Enter.");
        System.out.println("Example: 'Sports|Goal scored!'");
        System.out.println("Type 'exit' to quit.");
        System.out.println("----------------------------------------------------");

        try (Socket socket = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            while (true) {
                System.out.print("You > "); // Prompt for input
                String input = scanner.nextLine();

                if ("exit".equalsIgnoreCase(input)) break;

                // Send what you typed to your Broker
                out.println(input); 
                System.out.println("(Sent to Broker)");
            }
        } catch (Exception e) {
            System.out.println("Could not connect! Is Main.java running?");
        }
    }
}