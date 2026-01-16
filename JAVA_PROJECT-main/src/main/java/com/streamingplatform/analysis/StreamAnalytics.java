package com.streamingplatform.analysis;

import java.util.List;

import com.streamingplatform.persistence.MessageDAO;

public class StreamAnalytics {
    private final MessageDAO dao = new MessageDAO();

    public void printStats(String topic) {
        List<String> messages = dao.getMessagesByTopic(topic);
        
        System.out.println("--- Stream Analysis for '" + topic + "' ---");

        // 1. Count total using Stream
        long total = messages.stream().count();
        System.out.println("Total Messages: " + total);

        // 2. Filter messages containing "Error" using Stream & Lambda
        long errorCount = messages.stream()
                .filter(msg -> msg.toLowerCase().contains("error"))
                .count();
        System.out.println("Error Messages: " + errorCount);

        // 3. Calculate average length
        double avgLength = messages.stream()
                .mapToInt(String::length)
                .average()
                .orElse(0.0);
        System.out.println("Avg Message Length: " + avgLength);
    }
}