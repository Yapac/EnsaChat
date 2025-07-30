package clients.gui;

import clients.ChatClient;
import clients.AppLauncher;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AuthController {

    @FXML private TextField emailField;
    @FXML private Label statusLabel;

    private ChatClient client;
    private boolean loggedIn = false; // Prevent multiple UI switches

    private final ContextMenu suggestionsPopup = new ContextMenu();
    private final List<String> commonDomains = Arrays.asList(
            "gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "icloud.com"
    );

    public void initialize() {
        client = new ChatClient("localhost", 5000);
        if (!client.connect()) {
            statusLabel.setText("Impossible de se connecter au serveur.");
        }

        // Add listener for autocomplete suggestions
        emailField.addEventHandler(KeyEvent.KEY_RELEASED, e -> showSuggestions(emailField.getText()));
    }

    private void showSuggestions(String text) {
        // Don't show suggestions if text is empty or already contains '@'
        if (text.isEmpty() || text.contains("@")) {
            suggestionsPopup.hide();
            return;
        }

        List<MenuItem> items = commonDomains.stream()
            .map(domain -> {
                String suggestion = text + "@" + domain;
                MenuItem item = new MenuItem(suggestion);
                item.setOnAction(ev -> {
                    emailField.setText(suggestion);
                    suggestionsPopup.hide();
                });
                return item;
            })
            .collect(Collectors.toList());

        suggestionsPopup.getItems().setAll(items);

        if (!items.isEmpty()) {
            suggestionsPopup.show(emailField,
                emailField.localToScreen(0, emailField.getHeight()).getX(),
                emailField.localToScreen(0, emailField.getHeight()).getY());
        } else {
            suggestionsPopup.hide();
        }
    }

    @FXML
    private void handleRegister() {
        processAuth("REGISTER");
    }

    @FXML
    private void handleLogin() {
        processAuth("LOGIN");
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$");
    }

    private void processAuth(String command) {
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            statusLabel.setText("Email requis.");
            return;
        }

        if (command.equals("REGISTER") && !isValidEmail(email)) {
            statusLabel.setText("Email invalide.");
            return;
        }

        statusLabel.setText("Connexion en cours...");
        client.sendMessage(command + " " + email);

        new Thread(() -> {
            try {
                String response;
                while ((response = client.readMessage()) != null) {
                    final String resp = response;

                    if (!loggedIn && (
                            resp.contains("Connexion réussie") ||
                            resp.contains("Inscription et connexion réussies") ||
                            resp.contains("Inscription réussie")
                    )) {
                        loggedIn = true;
                        Platform.runLater(() -> {
                            try {
                                AppLauncher.switchToChat(email, client);
                            } catch (Exception e) {
                                e.printStackTrace();
                                statusLabel.setText("Erreur lors du chargement du chat.");
                            }
                        });
                        break;
                    }

                    if (resp.startsWith("[ERROR]:")) {
                        Platform.runLater(() -> statusLabel.setText(resp.substring(9).trim()));
                        break;
                    }

                    if (resp.startsWith("[INFO]:")) {
                        Platform.runLater(() -> statusLabel.setText(resp.substring(8).trim()));
                    } else {
                        Platform.runLater(() -> statusLabel.setText(resp));
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Erreur de connexion au serveur."));
            }
        }).start();
    }
}
