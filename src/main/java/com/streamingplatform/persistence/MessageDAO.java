package com.streamingplatform.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {

    // Save a message to the database
    @SuppressWarnings("CallToPrintStackTrace")
    public void saveMessage(String topic, String producerId, String content) {
        String sql = "INSERT INTO messages (topic, producer_id, content) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, topic);
            ps.setString(2, producerId);
            ps.setString(3, content);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Fetch all messages for a topic
    @SuppressWarnings("CallToPrintStackTrace")
    public List<String> getMessagesByTopic(String topic) {
        List<String> messages = new ArrayList<>();
        String sql = "SELECT content FROM messages WHERE topic = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, topic);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                messages.add(rs.getString("content"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }
}
