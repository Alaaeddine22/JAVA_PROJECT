module com.streamingplatform {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.h2database;
    requires mysql.connector.j;
    requires org.neo4j.driver;

    // Allow JavaFX to see UI classes
    opens com.streamingplatform.ui to javafx.graphics, javafx.fxml;

    // Export your main package
    exports com.streamingplatform;
}