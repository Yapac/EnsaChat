 package clients.gui;

import clients.ChatClient;
import clients.AppLauncher;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import clients.database.MessageDatabase;

public class ChatController {

    @FXML private TextField inputField;
    @FXML private Button sendButton;
    @FXML private Button fileButton;
    @FXML private Label userEmailLabel;
    @FXML private ListView<String> userListView;
    @FXML private VBox userSidebar;
    @FXML private TabPane chatTabs;
    @FXML private ListView<ChatMessage> globalMessageList;

    private ChatClient client;
    private String userEmail;
    private final Map<String, ListView<ChatMessage>> privateChats = new HashMap<>();
    private final MessageDatabase db = new MessageDatabase();

    @FXML
    public void initialize() {
        inputField.setOnAction(event -> handleSend());
        fileButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Media File");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image and Media Files", "*.png", "*.jpg", "*.gif", "*.mp4", "*.mp3", "*.wav")
            );
            File file = fileChooser.showOpenDialog(sendButton.getScene().getWindow());
            if (file != null) {
                sendFile(file);
            }
        });
        Platform.runLater(() -> {
            List<String> globalHistory = db.getMessagesBetween(userEmail, "GLOBAL");
            for (String oldMsg : globalHistory) {
                globalMessageList.getItems().add(new ChatMessage(oldMsg, false)); // no sound
            }
        });
    }

    public void initData(String userEmail, ChatClient client) {
        this.userEmail = userEmail;
        this.client = client;

        userEmailLabel.setText("Connecté en tant que : " + userEmail);

        userListView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String email = item.split(" ")[0];
                    boolean isOnline = item.contains("(online)");

                    Label emailLabel = new Label(email);
                    emailLabel.setStyle("-fx-font-size: 14px;");

                    Circle statusCircle = new Circle(6);
                    statusCircle.setFill(isOnline ? Color.LIMEGREEN : Color.CRIMSON);

                    HBox hbox = new HBox(8, statusCircle, emailLabel);
                    hbox.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(hbox);
                }
            }
        });

        userListView.setOnMouseClicked(event -> {
            String selected = userListView.getSelectionModel().getSelectedItem();
            if (selected != null && selected.contains("@")) {
                String target = selected.split(" ")[0];
                openPrivateTab(target);
                Tab tab = getTabByTitle(target);
                if (tab != null) {
                    chatTabs.getSelectionModel().select(tab);
                }
            }
        });

        globalMessageList.setCellFactory(list -> new MessageCell(userEmail));

        new Thread(() -> {
            try {
                String msg;
                while ((msg = client.readMessage()) != null) {
                    final String finalMsg = msg;

                    if (finalMsg.equals("END_OF_HISTORY")) {
                        Platform.runLater(() -> globalMessageList.getItems().add(new ChatMessage("[INFO]: --- Fin des messages non lus ---", false)));
                        break;
                    }

                    if (finalMsg.startsWith("USERLIST:")) {
                        final String[] users = finalMsg.substring(9).split(",");
                        Platform.runLater(() -> userListView.getItems().setAll(users));
                        continue;
                    }

                    Platform.runLater(() -> globalMessageList.getItems().add(new ChatMessage(finalMsg, true)));
                }

                while ((msg = client.readMessage()) != null) {
                    final String finalMsg = msg;

                    if (finalMsg.startsWith("USERLIST:")) {
                        final String[] users = finalMsg.substring(9).split(",");
                        Platform.runLater(() -> userListView.getItems().setAll(users));
                        continue;
                    }

                    if (finalMsg.matches("\\[.+]->\\[.+]: .+")) {
                        String[] parts = finalMsg.split("]->\\[");
                        String from = parts[0].substring(1); // Remove opening [
                        String to = parts[1].split("]")[0];  // Get the recipient

                        String target = from.equals(userEmail) ? to : from;

                        Platform.runLater(() -> {
                            // Don’t process your own message received back from server
                            if (from.equals(userEmail)) return;

                            boolean tabAlreadyOpen = privateChats.containsKey(target);
                            openPrivateTab(target);

                            if (tabAlreadyOpen) {
                                ListView<ChatMessage> privateList = privateChats.get(target);
                                if (privateList != null) {
                                    privateList.getItems().add(new ChatMessage(finalMsg, true));
                                }
                            }
                        });


                    } else {
                        Platform.runLater(() -> globalMessageList.getItems().add( new ChatMessage(finalMsg,true)));
                    }

                }

            } catch (Exception e) {
                Platform.runLater(() -> globalMessageList.getItems().add(new ChatMessage("[SYSTEM]: Déconnecté du serveur.", false)));
            }
        }).start();
    }

    private void sendFile(File file) {
    new Thread(() -> {
        try {
            String fileMessage = "[FILE] " + file.toURI().toString();
            Tab selectedTab = chatTabs.getSelectionModel().getSelectedItem();
            String tabTitle = selectedTab.getText();

            if (tabTitle.equals("Global Chat")) {
                client.sendMessage(fileMessage);
                Platform.runLater(() -> {
                    globalMessageList.getItems().add(new ChatMessage("[" + userEmail + "]: " + fileMessage, true));
                    db.saveMessage(userEmail, "GLOBAL", fileMessage);
                });
            } else {
                client.sendMessage("PRIVATE " + tabTitle + " " + fileMessage);
                Platform.runLater(() -> {
                    ListView<ChatMessage> list = privateChats.get(tabTitle);
                    if (list != null) {
                        list.getItems().add(new ChatMessage("[" + userEmail + "]: " + fileMessage, true));
                    }
                    db.saveMessage(userEmail, tabTitle, fileMessage);
                    
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }).start();
}

    @FXML
    private void handleSend() {
        String message = inputField.getText().trim();
        if (message.isEmpty()) return;

        Tab selectedTab = chatTabs.getSelectionModel().getSelectedItem();
        String tabTitle = selectedTab.getText();

        if (tabTitle.equals("Global Chat")) {
            client.sendMessage(message);
            globalMessageList.getItems().add(new ChatMessage("[" + userEmail + "]: " + message, true));
            db.saveMessage(userEmail, "GLOBAL", message);
        } else {
            client.sendMessage("PRIVATE " + tabTitle + " " + message);
            ListView<ChatMessage> list = privateChats.get(tabTitle);
            if (list != null) {
                list.getItems().add(new ChatMessage("[" + userEmail + "]: " + message, true));
            }
            db.saveMessage(userEmail, tabTitle, message);
        }

        inputField.clear();
    }


    @FXML
    private void handleLogout() {
        try {
            client.close();
            AppLauncher.switchToAuth();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openPrivateTab(String targetEmail) {
        if (privateChats.containsKey(targetEmail)) return;

        // Create a ListView for ChatMessage, not String
        ListView<ChatMessage> listView = new ListView<>();
        listView.setCellFactory(list -> new MessageCell(userEmail));

        // Load history and convert each String message to a ChatMessage with isNew = false
        List<String> history = db.getMessagesBetween(userEmail, targetEmail);
        for (String msg : history) {
            listView.getItems().add(new ChatMessage(msg, false));  // old messages, no sound
        }

        BorderPane contentPane = new BorderPane(listView);
        Tab tab = new Tab(targetEmail, contentPane);
        tab.setClosable(true);

        tab.setOnClosed(event -> privateChats.remove(targetEmail));

        privateChats.put(targetEmail, listView);
        chatTabs.getTabs().add(tab);
    }


    private Tab getTabByTitle(String title) {
        for (Tab tab : chatTabs.getTabs()) {
            if (tab.getText().equals(title)) return tab;
        }
        return null;
    }
}