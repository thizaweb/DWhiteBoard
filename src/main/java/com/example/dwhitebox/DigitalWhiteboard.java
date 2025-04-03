package com.example.dwhitebox;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DigitalWhiteboard extends Application {

    private WhiteboardController controller; // Controller to manage the whiteboard logic

    @Override
    public void start(Stage primaryStage) {
        controller = new WhiteboardController(primaryStage); // Initialize the controller with the primary stage
        Scene scene = controller.createScene(); // Create the scene using the controller
        primaryStage.setScene(scene); // Set the scene to the primary stage
        primaryStage.setTitle("Interactive Digital Whiteboard"); // Set the title of the stage
        primaryStage.show(); // Show the stage
    }

    public static void main(String[] args) {
        launch(args); // Launch the JavaFX application
    }

    // Inner class to handle the whiteboard logic and UI components
    private static class WhiteboardController {
        private Stage primaryStage; // Primary stage of the application
        private Canvas canvas; // Canvas for drawing
        private GraphicsContext gc; // Graphics context for drawing on the canvas
        private Color currentColor = Color.BLACK; // Current drawing color
        private double currentLineWidth = 2.0; // Current line width for drawing
        private List<WhiteboardItem> whiteboardContent = new ArrayList<>(); // List to store whiteboard items

        public WhiteboardController(Stage primaryStage) {
            this.primaryStage = primaryStage;
            this.canvas = new Canvas(800, 600); // Initialize the canvas
            this.gc = canvas.getGraphicsContext2D(); // Get the graphics context
            this.gc.setLineWidth(currentLineWidth); // Set the initial line width
            this.gc.setStroke(currentColor); // Set the initial stroke color
        }

        // Creates the main scene of the application
        public Scene createScene() {
            BorderPane root = new BorderPane(); // Root layout
            root.setTop(createToolbar()); // Add toolbar to the top
            root.setCenter(createCanvasContainer()); // Add canvas container to the center
            return new Scene(root, 1000, 700); // Create and return the scene
        }

        // Creates the toolbar with controls
        private HBox createToolbar() {
            HBox toolbar = new HBox(10); // Horizontal box for toolbar
            toolbar.setPadding(new Insets(10)); // Set padding
            toolbar.setStyle("-fx-background-color: #d3d3d3;"); // Set background color

            ColorPicker colorPicker = new ColorPicker(currentColor); // Color picker for selecting color
            colorPicker.setOnAction(e -> currentColor = colorPicker.getValue()); // Set current color on change

            Spinner<Double> lineWidthSpinner = new Spinner<>(1.0, 10.0, currentLineWidth, 1.0); // Spinner for line width
            lineWidthSpinner.valueProperty().addListener((obs, oldValue, newValue) -> currentLineWidth = newValue); // Set line width on change
            lineWidthSpinner.setStyle("-fx-pref-width: 80px;"); // Set preferred width

            Button clearButton = new Button("Clear"); // Button to clear the canvas
            clearButton.setOnAction(e -> clearCanvas()); // Clear canvas on click

            Button saveButton = new Button("Save"); // Button to save the session
            saveButton.setOnAction(e -> saveSession()); // Save session on click

            Button loadImageButton = new Button("Load Image"); // Button to load image
            loadImageButton.setOnAction(e -> loadImage()); // Load image on click

            Button loadAudioButton = new Button("Load Audio"); // Button to load audio
            loadAudioButton.setOnAction(e -> loadAudio()); // Load audio on click

            Button loadVideoButton = new Button("Load Video"); // Button to load video
            loadVideoButton.setOnAction(e -> loadVideo()); // Load video on click

            Button addTextButton = new Button("Add Text"); // Button to add text
            addTextButton.setOnAction(e -> addText()); // Add text on click

            toolbar.getChildren().addAll(colorPicker, lineWidthSpinner, clearButton, saveButton, loadImageButton, loadAudioButton, loadVideoButton, addTextButton); // Add controls to toolbar
            styleButton(clearButton); // Style the buttons
            styleButton(saveButton);
            styleButton(loadImageButton);
            styleButton(loadAudioButton);
            styleButton(loadVideoButton);
            styleButton(addTextButton);
            return toolbar;
        }

        // Creates the canvas container
        private StackPane createCanvasContainer() {
            StackPane canvasContainer = new StackPane(canvas); // Stack pane for canvas
            canvas.setStyle("-fx-background-color: #d3d3d3;"); // Set canvas background color
            canvas.setOnMousePressed(e -> { // Handle mouse pressed event
                gc.beginPath();
                gc.moveTo(e.getX(), e.getY());
            });
            canvas.setOnMouseDragged(e -> { // Handle mouse dragged event
                gc.setLineWidth(currentLineWidth);
                gc.setStroke(currentColor);
                gc.lineTo(e.getX(), e.getY());
                gc.stroke();
                whiteboardContent.add(new DrawingItem(e.getX(), e.getY(), currentColor, currentLineWidth)); // Add drawing item to list
            });
            return canvasContainer;
        }

        // Clears the canvas
        private void clearCanvas() {
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight()); // Clear the canvas
            whiteboardContent.clear(); // Clear the whiteboard content
        }

        // Saves the current session
        private void saveSession() {
            FileChooser fileChooser = new FileChooser(); // File chooser for saving file
            fileChooser.setTitle("Save Session");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Whiteboard Files", "*.wb"));
            File file = fileChooser.showSaveDialog(primaryStage); // Show save dialog
            if (file != null) {
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) { // Output stream to save file
                    oos.writeObject(whiteboardContent); // Write whiteboard content to file
                } catch (IOException e) {
                    e.printStackTrace(); // Print stack trace if error occurs
                }
            }
        }

        // Loads an image onto the canvas
        private void loadImage() {
            FileChooser fileChooser = new FileChooser(); // File chooser for loading image
            fileChooser.setTitle("Load Image");
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            File file = fileChooser.showOpenDialog(primaryStage); // Show open dialog
            if (file != null) {
                javafx.scene.image.Image image = new javafx.scene.image.Image(file.toURI().toString()); // Create image from file
                gc.drawImage(image, 0, 0); // Draw image on canvas
                whiteboardContent.add(new ImageItem(image, 0, 0)); // Add image item to list
            }
        }

        // Loads an audio file and adds controls
        private void loadAudio() {
            FileChooser fileChooser = new FileChooser(); // File chooser for loading audio
            fileChooser.setTitle("Load Audio");
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav"));
            File file = fileChooser.showOpenDialog(primaryStage); // Show open dialog
            if (file != null) {
                Media media = new Media(file.toURI().toString()); // Create media from file
                MediaPlayer mediaPlayer = new MediaPlayer(media); // Create media player
                VBox vbox = createAudioControls(mediaPlayer); // Create audio controls
                ((StackPane) canvas.getParent()).getChildren().add(vbox); // Add controls to canvas container
                mediaPlayer.play(); // Play the audio
                whiteboardContent.add(new AudioItem(mediaPlayer, vbox)); // Add audio item to list
            }
        }

        // Creates audio playback controls
        private VBox createAudioControls(MediaPlayer mediaPlayer) {
            Button playPauseButton = new Button("▶"); // Play/pause button
            playPauseButton.setOnAction(e -> togglePlayPause(mediaPlayer, playPauseButton)); // Toggle play/pause on click
            Label audioLabel = new Label("Audio Playback"); // Label for audio controls
            VBox vbox = new VBox(10, audioLabel, playPauseButton); // Vertical box for controls
            vbox.setLayoutX(50); // Set layout x
            vbox.setLayoutY(50); // Set layout y
            styleButton(playPauseButton); // Style the button
            mediaPlayer.setOnEndOfMedia(() -> { // Handle end of media event
                mediaPlayer.seek(Duration.ZERO); // Reset playback
                mediaPlayer.pause(); // Pause the player
                playPauseButton.setText("▶"); // Set button text to play
            });
            return vbox;
        }

        // Loads a video file and adds controls
        private void loadVideo() {
            FileChooser fileChooser = new FileChooser(); // File chooser for loading video
            fileChooser.setTitle("Load Video");
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi"));
            File file = fileChooser.showOpenDialog(primaryStage); // Show open dialog
            if (file != null) {
                Media media = new Media(file.toURI().toString()); // Create media from file
                MediaPlayer mediaPlayer = new MediaPlayer(media); // Create media player
                MediaView mediaView = new MediaView(mediaPlayer); // Create media view
                VBox vbox = createVideoControls(mediaPlayer, mediaView); // Create video controls
                ((Pane) canvas.getParent()).getChildren().add(vbox); // Add controls to canvas container
                mediaPlayer.play(); // Play the video
                whiteboardContent.add(new VideoItem(mediaPlayer, 100, 100, vbox)); // Add video item to list
            }
        }
        // Create video playback controls.
        private VBox createVideoControls(MediaPlayer mediaPlayer,MediaView mediaView){
            VBox vbox = new VBox(mediaView);
            vbox.setLayoutX(100);
            vbox.setLayoutY(100);
            Button playPauseButton = new Button("▶");
            playPauseButton.setOnAction(e -> togglePlayPause(mediaPlayer, playPauseButton));
            vbox.getChildren().add(playPauseButton);
            styleButton(playPauseButton);
            mediaPlayer.setOnEndOfMedia(() -> {
                mediaPlayer.seek(Duration.ZERO);
                mediaPlayer.pause();
                playPauseButton.setText("▶");
            });
            return vbox;
        }

        // Adds text to the canvas
        private void addText() {
            TextInputDialog dialog = new TextInputDialog("Enter Text"); // Text input dialog
            dialog.setTitle("Add Text");
            dialog.setHeaderText(null);
            dialog.setContentText("Text:");
            dialog.showAndWait().ifPresent(text -> { // Show dialog and handle input
                gc.fillText(text, 100, 100); // Draw text on canvas
                whiteboardContent.add(new TextItem(text, 100, 100)); // Add text item to list
            });
        }

        // Toggles play/pause for media player
        private void togglePlayPause(MediaPlayer mediaPlayer, Button playPauseButton) {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) { // If playing
                mediaPlayer.pause(); // Pause the player
                playPauseButton.setText("▶"); // Set button text to play
            } else { // If paused
                mediaPlayer.play(); // Play the player
                playPauseButton.setText("⏸"); // Set button text to pause
            }
        }

        // Styles the buttons
        private void styleButton(Button button) {
            button.setStyle("-fx-background-color: #a3c1e5; -fx-text-fill: white; -fx-padding: 8px 15px; -fx-border-radius: 5px;"); // Set button style
            button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #3e8e41; -fx-text-fill: white; -fx-padding: 8px 15px; -fx-border-radius: 5px;")); // Set style on mouse enter
            button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #a3c1e5; -fx-text-fill: white; -fx-padding: 8px 15px; -fx-border-radius: 5px;")); // Set style on mouse exit
        }
    }

    // Serializable interface for whiteboard items
    private interface WhiteboardItem extends Serializable {}

    // Drawing item class
    private static class DrawingItem implements WhiteboardItem {
        double x, y;
        Color color;
        double lineWidth;
        DrawingItem(double x, double y, Color color, double lineWidth) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.lineWidth = lineWidth;
        }
    }

    // Image item class
    private static class ImageItem implements WhiteboardItem {
        javafx.scene.image.Image image;
        double x, y;
        ImageItem(javafx.scene.image.Image image, double x, double y) {
            this.image = image;
            this.x = x;
            this.y = y;
        }
    }

    // Audio item class
    private static class AudioItem implements WhiteboardItem {
        MediaPlayer mediaPlayer;
        VBox audioControl;
        AudioItem(MediaPlayer mediaPlayer, VBox audioControl) {
            this.mediaPlayer = mediaPlayer;
            this.audioControl = audioControl;
        }
    }

    // Video item class
    private static class VideoItem implements WhiteboardItem {
        MediaPlayer mediaPlayer;
        double x, y;
        VBox videoControl;
        VideoItem(MediaPlayer mediaPlayer, double x, double y, VBox videoControl) {
            this.mediaPlayer = mediaPlayer;
            this.x = x;
            this.y = y;
            this.videoControl = videoControl;
        }
    }

    // Text item class
    private static class TextItem implements WhiteboardItem {
        String text;
        double x, y;
        TextItem(String text, double x, double y) {
            this.text = text;
            this.x = x;
            this.y = y;
        }
    }
}