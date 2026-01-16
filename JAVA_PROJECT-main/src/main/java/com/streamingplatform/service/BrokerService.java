package com.streamingplatform.service;

import java.util.List;

import com.streamingplatform.persistence.MessageDAO;

public class BrokerService {
    private final MessageDAO messageDAO;

    // FIX: Constructor Injection
    public BrokerService(MessageDAO messageDAO) {
        this.messageDAO = messageDAO;
    }

    public void publish(String topic, String producerId, String content) {
        messageDAO.saveMessage(topic, producerId, content);
        System.out.println("[Broker] Persisted: " + topic + " -> " + content);

    }

    public List<String> consume(String topic) {
        return messageDAO.getMessagesByTopic(topic);
    }
}