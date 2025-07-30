package server;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Set;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private List<ClientHandler> clients;
    private UserAuth auth;
    private String email;

    public ClientHandler(Socket socket, List<ClientHandler> clients, UserAuth auth) {
        this.socket = socket;
        this.clients = clients;
        this.auth = auth;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }

    @Override
    public void run() {
        try {
            out.println("[INFO]: Bienvenue!");

            // Authentication loop
            String input;
            while ((input = in.readLine()) != null) {
                if (input.startsWith("REGISTER ")) {
                    email = input.substring(9).trim();
                    if (auth.register(email)) {
                        auth.login(email, this);  // log them in immediately
                        out.println("[INFO]: Inscription et connexion réussies. Bienvenue " + email);
                        broadcastSystemMessage(email + " s'est inscrit et connecté.");
                        sendPendingMessages();
                        sendUserList();
                        break;
                    } else {
                        out.println("[ERROR]: Email déjà enregistré.");
                    }
                } else if (input.startsWith("LOGIN ")) {
                    email = input.substring(6).trim();
                    if (auth.login(email, this)) {
                        out.println("[INFO]: Connexion réussie. Bienvenue " + email);
                        broadcastSystemMessage(email + " s'est connecté.");
                        sendPendingMessages();
                        sendUserList();
                        break;
                    } else {
                        out.println("[ERROR]: Email invalide ou déjà connecté.");
                    }
                } else {
                    out.println("[ERROR]: Commande invalide. Utilisez REGISTER <email> ou LOGIN <email>");
                }
            }

            // Main message loop
            while ((input = in.readLine()) != null) {
                if (input.startsWith("PRIVATE ")) {
                    // Format: PRIVATE recipient@example.com message...
                    int firstSpace = input.indexOf(' ');
                    int secondSpace = input.indexOf(' ', firstSpace + 1);
                    if (secondSpace > 0) {
                        String recipient = input.substring(firstSpace + 1, secondSpace);
                        String privateMsg = input.substring(secondSpace + 1);
                        String formatted = "[" + email + "]->[" + recipient + "]: " + privateMsg;

                        ClientHandler recipientHandler = auth.getClientHandler(recipient);
                        if (recipientHandler != null) {
                            recipientHandler.sendMessage(formatted);
                        } else {
                            // Store offline message
                            auth.addPendingMessage(recipient, formatted);
                        }

                        // Also show to sender
                        sendMessage(formatted);
                    } else {
                        sendMessage("[SYSTEM]: Mauvais format pour message privé. Utilisez PRIVATE <destinataire> <message>");
                    }
                } else if (input.equalsIgnoreCase("GET_USERS")) {
                    sendUserList();
                } else {
                    // Broadcast public message
                    String formatted = "[" + email + "]: " + input;
                    System.out.println(formatted);

                    for (ClientHandler client : clients) {
                        if (client != this) {
                            client.sendMessage(formatted);
                        }
                    }

                    // Store for offline users
                    for (String userEmail : auth.getRegisteredUsers()) {
                        if (!auth.isLoggedIn(userEmail) && !userEmail.equals(email)) {
                            auth.addPendingMessage(userEmail, formatted);
                        }
                    }
                }
            }

        } catch (IOException e) {
            System.out.println(email + " s’est déconnecté.");
        } finally {
            cleanup();
        }
    }

    private void sendPendingMessages() {
        List<String> pending = auth.retrievePendingMessages(email);
        if (!pending.isEmpty()) {
            out.println("[INFO]: Vous avez " + pending.size() + " message(s) non lus :");
            for (String msg : pending) {
                out.println(msg);
            }
        }
        out.println("END_OF_HISTORY");
    }

    private void broadcastSystemMessage(String msg) {
        String sysMsg = "[SYSTEM]: " + msg;
        for (ClientHandler client : clients) {
            if (client != this) {
                client.sendMessage(sysMsg);
                client.sendUserList();
            }
        }
    }

    
    private void sendUserList() {
        Set<String> allUsers = auth.getRegisteredUsers();
        StringBuilder listBuilder = new StringBuilder("USERLIST:");

        for (String user : allUsers) {
            boolean online = auth.isLoggedIn(user);
            listBuilder.append(user)
                    .append(online ? " (online)" : " (offline)")
                    .append(",");
        }

        // Remove trailing comma if present
        if (listBuilder.charAt(listBuilder.length() - 1) == ',') {
            listBuilder.setLength(listBuilder.length() - 1);
        }

        sendMessage(listBuilder.toString());
    }

    private void cleanup() {
        try {
            if (email != null) {
                auth.logout(email);
                broadcastSystemMessage(email + " s'est déconnecté.");
            }
            clients.remove(this);
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
