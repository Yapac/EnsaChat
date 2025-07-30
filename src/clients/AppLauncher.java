package clients;

import java.io.IOException;

import clients.gui.ChatController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AppLauncher extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        switchToAuth();
        primaryStage.setTitle("EnsaChat");
        primaryStage.show();
    }

    public static void switchToAuth() throws IOException {
        FXMLLoader loader = new FXMLLoader(AppLauncher.class.getResource("fxml/auth.fxml"));
        Scene scene = new Scene(loader.load(), 900, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void switchToChat(String email, ChatClient client) throws Exception {
        FXMLLoader loader = new FXMLLoader(AppLauncher.class.getResource("fxml/chat.fxml"));
        Parent root = loader.load();

        ChatController controller = loader.getController();
        controller.initData(email, client);

        primaryStage.setScene(new Scene(root,900,600));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
