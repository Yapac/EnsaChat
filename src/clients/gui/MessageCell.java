package clients.gui;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class MessageCell extends ListCell<ChatMessage> {

    private final String currentUserEmail;

    public MessageCell(String currentUserEmail) {
        this.currentUserEmail = currentUserEmail;
    }

    @Override
    protected void updateItem(ChatMessage chatMessage, boolean empty) {
        super.updateItem(chatMessage, empty);

        if (empty || chatMessage == null) {
            setGraphic(null);
            setText(null);
            return;
        }

        String message = chatMessage.content;

        // Only play sound for new incoming messages
        if (chatMessage.isNew) {
            String sender = extractSender(message);
            if (sender != null && !sender.equalsIgnoreCase(currentUserEmail)) {
                playNotificationSound();
                showVisualToast(message);
            }
        }

        HBox container = new HBox();
        container.setPadding(new Insets(5));

        // SYSTEM/INFO/ERROR messages
        if (message.startsWith("[SYSTEM]: ")) {
            container.getChildren().add(styledLabel(message.substring(9), "-fx-font-style: italic; -fx-text-fill: #888;"));
            container.setAlignment(Pos.CENTER);
            setGraphic(container);
            return;
        } else if (message.startsWith("[INFO]: ")) {
            container.getChildren().add(styledLabel(message.substring(8), "-fx-font-size: 13; -fx-text-fill: #2e7d32;"));
            container.setAlignment(Pos.CENTER);
            setGraphic(container);
            return;
        } else if (message.startsWith("[ERROR]: ")) {
            container.getChildren().add(styledLabel(message.substring(9), "-fx-text-fill: red;"));
            container.setAlignment(Pos.CENTER);
            setGraphic(container);
            return;
        }

        // Parse sender/content
        String sender;
        String content;
        if (message.matches("\\[.+]->\\[.+]: .+")) {
            int arrowIndex = message.indexOf("]->[");
            sender = message.substring(1, arrowIndex);
            content = message.substring(message.indexOf("]: ") + 3);
        } else if (message.startsWith("[") && message.contains("]: ")) {
            int sep = message.indexOf("]: ");
            sender = message.substring(1, sep);
            content = message.substring(sep + 3);
        } else {
            setText(message);
            setGraphic(null);
            return;
        }

        boolean isCurrentUser = sender.equalsIgnoreCase(currentUserEmail);

        Label senderLabel = new Label(sender);
        senderLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #666;");

        VBox messageBox = new VBox(senderLabel);
        messageBox.setSpacing(3);

        // === FILES ===
        if (content.startsWith("[FILE] ")) {
            String fileUrl = content.substring(7).trim().replace(" ", "%20");
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1).replace("%20", " ");
            String bubbleStyle = "-fx-background-radius: 15; -fx-padding: 10;" +
                    (isCurrentUser ? "-fx-background-color: #0078d7; -fx-text-fill: white;"
                                   : "-fx-background-color: #e4e6eb; -fx-text-fill: black;");

            if (fileUrl.matches(".*\\.(png|jpg|jpeg|gif)")) {
                try {
                    Image img = new Image(fileUrl, 200, 0, true, true);
                    if (img.isError()) throw new Exception("Image error");

                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(200);
                    iv.setPreserveRatio(true);

                    Label name = new Label("🖼 " + fileName);
                    name.setStyle("-fx-font-size: 12;" + (isCurrentUser ? "-fx-text-fill: white;" : "-fx-text-fill: black;"));

                    VBox wrapper = new VBox(name, iv);
                    wrapper.setSpacing(5);
                    wrapper.setStyle(bubbleStyle);
                    messageBox.getChildren().add(wrapper);
                } catch (Exception e) {
                    messageBox.getChildren().add(styledLabel("Erreur image", "-fx-text-fill: red;"));
                }

            } else if (fileUrl.matches(".*\\.(mp4|m4v|mov)")) {
                try {
                    MediaPlayer player = new MediaPlayer(new Media(fileUrl));
                    MediaView view = new MediaView(player);
                    view.setFitWidth(300);
                    view.setPreserveRatio(true);

                    Label name = new Label("🎬 " + fileName);
                    name.setStyle("-fx-font-size: 12;" + (isCurrentUser ? "-fx-text-fill: white;" : "-fx-text-fill: black;"));

                    HBox controls = controlBar(player);
                    VBox wrapper = new VBox(name, view, controls);
                    wrapper.setSpacing(5);
                    wrapper.setStyle(bubbleStyle);
                    messageBox.getChildren().add(wrapper);
                } catch (Exception e) {
                    messageBox.getChildren().add(styledLabel("Erreur vidéo", "-fx-text-fill: red;"));
                }

            } else if (fileUrl.matches(".*\\.(mp3|wav|aac)")) {
                try {
                    MediaPlayer player = new MediaPlayer(new Media(fileUrl));
                    Label name = new Label("🎵 " + fileName);
                    name.setStyle("-fx-font-size: 12;" + (isCurrentUser ? "-fx-text-fill: white;" : "-fx-text-fill: black;"));

                    HBox controls = controlBar(player);
                    VBox wrapper = new VBox(name, controls);
                    wrapper.setSpacing(5);
                    wrapper.setStyle(bubbleStyle);
                    messageBox.getChildren().add(wrapper);
                } catch (Exception e) {
                    messageBox.getChildren().add(styledLabel("Erreur audio", "-fx-text-fill: red;"));
                }
            } else {
                messageBox.getChildren().add(styledLabel("[Fichier non pris en charge]: " + fileUrl, "-fx-text-fill: orange;"));
            }

        } else {
            // === TEXT ===
            Label messageLabel = new Label(content);
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(400);
            messageLabel.setStyle("-fx-padding: 10; -fx-font-size: 14; -fx-background-radius: 15;" +
                    (isCurrentUser ? "-fx-background-color: #0078d7; -fx-text-fill: white;"
                                   : "-fx-background-color: #e4e6eb; -fx-text-fill: black;"));
            messageBox.getChildren().add(messageLabel);
        }

        container.setAlignment(isCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        container.getChildren().add(messageBox);
        setGraphic(container);
    }

    private Label styledLabel(String text, String style) {
        Label label = new Label(text);
        label.setStyle(style + "-fx-padding: 5 10;");
        return label;
    }

    private HBox controlBar(MediaPlayer player) {
        Button play = new Button("▶️"); play.setOnAction(e -> player.play());
        Button pause = new Button("⏸"); pause.setOnAction(e -> player.pause());
        Button stop = new Button("⏹"); stop.setOnAction(e -> player.stop());

        HBox bar = new HBox(10, play, pause, stop);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private String extractSender(String message) {
        if (message.matches("\\[.+]->\\[.+]: .+")) {
            return message.substring(1, message.indexOf("]->["));
        } else if (message.startsWith("[") && message.contains("]: ")) {
            return message.substring(1, message.indexOf("]: "));
        }
        return null;
    }

    private void playNotificationSound() {
        try {
            String soundPath = getClass().getResource("/clients/resources/notification.mp3").toExternalForm();
            MediaPlayer player = new MediaPlayer(new Media(soundPath));
            player.setVolume(0.5);
            player.play();
        } catch (Exception e) {
            System.err.println("Failed to play sound: " + e.getMessage());
        }
    }

    private void showVisualToast(String message) {
        Label toast = new Label(message);
        toast.setStyle("-fx-background-color: black; -fx-text-fill: white; -fx-padding: 10; -fx-background-radius: 5;");
        Stage toastStage = new Stage(StageStyle.TRANSPARENT);
        toastStage.setScene(new Scene(new StackPane(toast), null));
        toastStage.initOwner(getScene().getWindow());
        toastStage.setAlwaysOnTop(true);
        toastStage.show();

        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(e -> toastStage.close());
        delay.play();
    }
}
