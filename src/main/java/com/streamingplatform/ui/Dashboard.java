package com.streamingplatform.ui;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Timer;
import java.util.TimerTask;

import com.streamingplatform.persistence.DatabaseManager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

public class Dashboard extends Application {
    private final ObservableList<MessageData> data = FXCollections.observableArrayList();

    public static void launchApp() {
        new Thread(() -> Application.launch(Dashboard.class)).start();
    }

    @Override
    public void start(Stage stage) {
        TableView<MessageData> table = new TableView<>();
        
        TableColumn<MessageData, String> colTopic = new TableColumn<>("Topic");
        colTopic.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().topic));
        
        TableColumn<MessageData, String> colContent = new TableColumn<>("Content");
        colContent.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().content));
        
        table.setItems(data);
        table.getColumns().addAll(colTopic, colContent);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Scene scene = new Scene(table, 600, 400);
        stage.setTitle("Real-Time Streaming Dashboard");
        stage.setScene(scene);
        stage.show();

        // Auto-refresh every 2 seconds
        new Timer(true).scheduleAtFixedRate(new TimerTask() {
            public void run() { refresh(); }
        }, 0, 2000);
    }

    private void refresh() {
        try (Connection conn = DatabaseManager.getConnection();
             ResultSet rs = conn.createStatement().executeQuery("SELECT topic, content FROM messages ORDER BY id DESC LIMIT 50")) {
             
            ObservableList<MessageData> snapshot = FXCollections.observableArrayList();
            while (rs.next()) snapshot.add(new MessageData(rs.getString("topic"), rs.getString("content")));
            
            Platform.runLater(() -> { data.clear(); data.addAll(snapshot); });
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static class MessageData {
        String topic, content;
        public MessageData(String t, String c) { this.topic = t; this.content = c; }
    }
}