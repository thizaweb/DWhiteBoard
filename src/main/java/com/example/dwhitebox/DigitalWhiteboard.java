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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DigitalWhiteboard extends Application {

    private WhiteboardController controller;

    @Override
    public void start(Stage primaryStage) {
        controller = new WhiteboardController(primaryStage);
        Scene scene = controller.createScene();
        primaryStage.setScene(scene);
        primaryStage.setTitle("Interactive Digital Whiteboard");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static class WhiteboardController {
        private Stage primaryStage;
        private Canvas canvas;
        private GraphicsContext gc;
        private Color currentColor = Color.BLACK;
        private double currentLineWidth = 2.0;
        private List<WhiteboardItem> whiteboardContent = new ArrayList<>();

        public WhiteboardController(Stage primaryStage) {
            this.primaryStage = primaryStage;
            this.canvas = new Canvas(800, 600);
            this.gc = canvas.getGraphicsContext2D();
            this.gc.setLineWidth(currentLineWidth);
            this.gc.setStroke(currentColor);
            loadSessionOnStart(); // Attempt to load saved session on startup
        }

        public Scene createScene() {
            BorderPane root = new BorderPane();
            root.setTop(createToolbar());
            root.setCenter(createCanvasContainer());
            return new Scene(root, 1000, 700);
        }

        private HBox createToolbar() {
            HBox toolbar = new HBox(10);
            toolbar.setPadding(new Insets(10));
            toolbar.setStyle("-fx-background-color: #d3d3d3;");

            ColorPicker colorPicker = new ColorPicker(currentColor);
            colorPicker.setOnAction(e -> currentColor = colorPicker.getValue());

            Spinner<Double> lineWidthSpinner = new Spinner<>(1.0, 10.0, currentLineWidth, 1.0);
            lineWidthSpinner.valueProperty().addListener((obs, oldValue, newValue) -> currentLineWidth = newValue);
            lineWidthSpinner.setStyle("-fx-pref-width: 80px;");

            Button clearButton = new Button("Clear");
            clearButton.setOnAction(e -> clearCanvas());

            Button saveButton = new Button("Save");
            saveButton.setOnAction(e -> saveSession());

            Button loadImageButton = new Button("Load Image");
            loadImageButton.setOnAction(e -> loadImage());

            Button loadAudioButton = new Button("Load Audio");
            loadAudioButton.setOnAction(e -> loadAudio());

            Button loadVideoButton = new Button("Load Video");
            loadVideoButton.setOnAction(e -> loadVideo());

            Button addTextButton = new Button("Add Text");
            addTextButton.setOnAction(e -> addText());

            toolbar.getChildren().addAll(colorPicker, lineWidthSpinner, clearButton, saveButton, loadImageButton, loadAudioButton, loadVideoButton, addTextButton);
            styleButton(clearButton);
            styleButton(saveButton);
            styleButton(loadImageButton);
            styleButton(loadAudioButton);
            styleButton(loadVideoButton);
            styleButton(addTextButton);
            return toolbar;
        }

        private StackPane createCanvasContainer() {
            StackPane canvasContainer = new StackPane(canvas);
            canvas.setStyle("-fx-background-color: white;");
            canvas.setOnMousePressed(e -> {
                gc.beginPath();
                gc.moveTo(e.getX(), e.getY());
            });
            canvas.setOnMouseDragged(e -> {
                gc.setLineWidth(currentLineWidth);
                gc.setStroke(currentColor);
                gc.lineTo(e.getX(), e.getY());
                gc.stroke();
                whiteboardContent.add(new DrawingItem(e.getX(), e.getY(), currentColor, currentLineWidth));
            });
            return canvasContainer;
        }

        private void clearCanvas() {
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            whiteboardContent.clear();
        }

        private void saveSession() {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Session");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Whiteboard Files", "*.wb"));
            File file = fileChooser.showSaveDialog(primaryStage);
            if (file != null) {
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                    oos.writeObject(whiteboardContent);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void loadSessionOnStart() {
            // Attempt to load a default session file on startup if it exists
            File defaultSaveFile = new File("whiteboard_session.wb");
            if (defaultSaveFile.exists()) {
                loadSessionFromFile(defaultSaveFile);
            }
        }

        private void loadSessionFromFile(File file) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                whiteboardContent = (List<WhiteboardItem>) ois.readObject();
                redrawCanvas();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                // Optionally show an alert to the user indicating load failure
            }
        }

        private void redrawCanvas() {
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            StackPane canvasContainer = (StackPane) canvas.getParent();
            List<javafx.scene.Node> mediaNodesToRemove = new ArrayList<>();
            for (javafx.scene.Node node : canvasContainer.getChildren()) {
                if (node instanceof VBox && (node.getId() != null && (node.getId().startsWith("audioControl") || node.getId().startsWith("videoControl")))) {
                    mediaNodesToRemove.add(node);
                }
            }
            canvasContainer.getChildren().removeAll(mediaNodesToRemove);

            for (WhiteboardItem item : whiteboardContent) {
                if (item instanceof DrawingItem) {
                    DrawingItem di = (DrawingItem) item;
                    gc.setStroke(di.color);
                    gc.setLineWidth(di.lineWidth);
                    gc.lineTo(di.x, di.y);
                    gc.stroke();
                    gc.beginPath(); // Important to reset the path for the next drawing
                    gc.moveTo(di.x, di.y); // Move to the last point for continuous drawing
                } else if (item instanceof ImageItem) {
                    ImageItem ii = (ImageItem) item;
                    gc.drawImage(ii.image, ii.x, ii.y);
                } else if (item instanceof AudioItem) {
                    AudioItem ai = (AudioItem) item;
                    Media media = new Media(ai.mediaSource);
                    MediaPlayer mediaPlayer = new MediaPlayer(media);
                    File audioFile = new File(ai.mediaSource);
                    VBox vbox = createAudioControls(mediaPlayer, audioFile.getName()); // Pass the filename
                    vbox.setLayoutX(50);
                    vbox.setLayoutY(50);
                    canvasContainer.getChildren().add(vbox);
                    ai.setAudioControl(vbox); // Store the created control
                } else if (item instanceof VideoItem) {
                    VideoItem vi = (VideoItem) item;
                    Media media = new Media(vi.mediaSource);
                    MediaPlayer mediaPlayer = new MediaPlayer(media);
                    MediaView mediaView = new MediaView(mediaPlayer);
                    // Set initial size for the MediaView
                    mediaView.setFitWidth(200);
                    mediaView.setFitHeight(150);
                    VBox vbox = createVideoControls(mediaPlayer, mediaView);
                    vbox.setLayoutX(vi.x);
                    vbox.setLayoutY(vi.y);
                    canvasContainer.getChildren().add(vbox);
                    vi.setVideoControl(vbox); // Store the created control
                } else if (item instanceof TextItem) {
                    TextItem ti = (TextItem) item;
                    gc.fillText(ti.text, ti.x, ti.y);
                }
            }
            // After redrawing, if there were drawing items, ensure the path is reset
            gc.beginPath();
        }


        private void loadImage() {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load Image");
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                javafx.scene.image.Image image = new javafx.scene.image.Image(file.toURI().toString());
                gc.drawImage(image, 0, 0);
                whiteboardContent.add(new ImageItem(image, 0, 0));
            }
        }

        private void loadAudio() {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load Audio");
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav"));
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                Media media = new Media(file.toURI().toString());
                MediaPlayer mediaPlayer = new MediaPlayer(media);
                AudioItem audioItem = new AudioItem(mediaPlayer.getMedia().getSource());
                whiteboardContent.add(audioItem);
                VBox vbox = createAudioControls(mediaPlayer, file.getName()); // Pass the filename
                vbox.setLayoutX(100);
                vbox.setLayoutY(100);
                ((StackPane) canvas.getParent()).getChildren().add(vbox);
                audioItem.setAudioControl(vbox);
                mediaPlayer.play();
            }
        }

        private VBox createAudioControls(MediaPlayer mediaPlayer, String audioFileName) {
            Button playPauseButton = new Button("⏸");
            playPauseButton.setOnAction(e -> togglePlayPause(mediaPlayer, playPauseButton));
            Label audioLabel = new Label(audioFileName); // Use the passed filename
            VBox vbox = new VBox(10, audioLabel, playPauseButton);
            vbox.setLayoutX(50);
            vbox.setLayoutY(50);
            vbox.setId("audioControl-" + System.currentTimeMillis()); // Unique ID for removal
            styleButton(playPauseButton);
            mediaPlayer.setOnEndOfMedia(() -> {
                mediaPlayer.seek(Duration.ZERO);
                mediaPlayer.pause();
                playPauseButton.setText("▶");
            });
            return vbox;
        }

        private void loadVideo() {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load Video");
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi"));
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                Media media = new Media(file.toURI().toString());
                MediaPlayer mediaPlayer = new MediaPlayer(media);
                VideoItem videoItem = new VideoItem(mediaPlayer.getMedia().getSource(), 100, 100);
                whiteboardContent.add(videoItem);
                MediaView mediaView = new MediaView(mediaPlayer);
                // Set initial size for the MediaView
                mediaView.setFitWidth(500);
                mediaView.setFitHeight(500);
                VBox vbox = createVideoControls(mediaPlayer, mediaView);
                vbox.setLayoutX(100);
                vbox.setLayoutY(100);
                vbox.setId("videoControl-" + System.currentTimeMillis()); // Unique ID for removal
                ((Pane) canvas.getParent()).getChildren().add(vbox);
                videoItem.setVideoControl(vbox);
                mediaPlayer.play();
            }
        }

        private VBox createVideoControls(MediaPlayer mediaPlayer, MediaView mediaView) {
            VBox vbox = new VBox(mediaView);
            vbox.setLayoutX(100);
            vbox.setLayoutY(100);
            Button playPauseButton = new Button("⏸");
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

        private void addText() {
            TextInputDialog dialog = new TextInputDialog("Enter Text");
            dialog.setTitle("Add Text");
            dialog.setHeaderText(null);
            dialog.setContentText("Text:");
            dialog.showAndWait().ifPresent(text -> {
                gc.fillText(text, 100, 100);
                whiteboardContent.add(new TextItem(text, 100, 100));
            });
        }

        private void togglePlayPause(MediaPlayer mediaPlayer, Button playPauseButton) {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                playPauseButton.setText("▶");
            } else {
                mediaPlayer.play();
                playPauseButton.setText("⏸");
            }
        }

        private void styleButton(Button button) {
            button.setStyle("-fx-background-color: #a3c1e5; -fx-text-fill: white; -fx-padding: 8px 15px; -fx-border-radius: 5px;");
            button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #3e8e41; -fx-text-fill: white; -fx-padding: 8px 15px; -fx-border-radius: 5px;"));
            button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #a3c1e5; -fx-text-fill: white; -fx-padding: 8px 15px; -fx-border-radius: 5px;"));
        }
    }

    private interface WhiteboardItem extends Serializable {
    }

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

    private static class ImageItem implements WhiteboardItem {
        javafx.scene.image.Image image;
        double x, y;

        // Image is Serializable in JavaFX
        ImageItem(javafx.scene.image.Image image, double x, double y) {
            this.image = image;
            this.x = x;
            this.y = y;
        }
    }

    private static class AudioItem implements WhiteboardItem {
        String mediaSource;
        private transient VBox audioControl; // Will not be serialized

        AudioItem(String mediaSource) {
            this.mediaSource = mediaSource;
        }

        // Setter for audioControl after it's recreated on load
        public void setAudioControl(VBox audioControl) {
            this.audioControl = audioControl;
        }

        // Getter for mediaSource if needed
        public String getMediaSource() {
            return mediaSource;
        }
    }

    private static class VideoItem implements WhiteboardItem {
        String mediaSource;
        double x, y;
        private transient VBox videoControl; // Will not be serialized

        VideoItem(String mediaSource, double x, double y) {
            this.mediaSource = mediaSource;
            this.x = x;
            this.y = y;
        }

        // Setter for videoControl after it's recreated on load
        public void setVideoControl(VBox videoControl) {
            this.videoControl = videoControl;
        }

        // Getter for mediaSource if needed
        public String getMediaSource() {
            return mediaSource;
        }
    }

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
