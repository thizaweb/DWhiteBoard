module com.example.dwhitebox {
    requires javafx.controls;
    requires javafx.media;
    requires javafx.swing;
    requires javafx.fxml;


    opens com.example.dwhitebox to javafx.fxml;
    exports com.example.dwhitebox;
}