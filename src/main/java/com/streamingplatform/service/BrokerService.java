package com.streamingplatform.service;

import java.util.List;

import com.streamingplatform.persistence.MessageDAO;

public class BrokerService {
    private final MessageDAO messageDAO = new MessageDAO();

    // Receives message from Network -> Saves to DB
    public void publish(String topic, String producerId, String content) {
        messageDAO.saveMessage(topic, producerId, content);
        System.out.println("[Broker] Persisted: " + topic + " -> " + content);
    }

    public List<String> consume(String topic) {
        return messageDAO.getMessagesByTopic(topic);
    }
}