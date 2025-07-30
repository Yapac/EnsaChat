package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerMain {
    public static final int PORT = 5000;
    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final UserAuth auth = new UserAuth();

    public static void main(String[] args) {
        System.out.println("Serveur en attente de connexions...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connexion entrante : " + clientSocket);
                ClientHandler handler = new ClientHandler(clientSocket, clients, auth);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}