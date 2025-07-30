package server;

import server.database.UserDatabase;

import java.util.*;

public class UserAuth {
    private final Map<String, ClientHandler> loggedInUsers = new HashMap<>();
    private final Map<String, List<String>> pendingMessages = new HashMap<>();
    private final UserDatabase db = new UserDatabase(); // SQLite database handler

    public synchronized boolean register(String email) {
        return db.registerUser(email); // Persist to DB
    }

    public synchronized boolean login(String email, ClientHandler client) {
        if (!db.userExists(email)) return false;
        if (loggedInUsers.containsKey(email)) return false;
        loggedInUsers.put(email, client);
        return true;
    }

    public synchronized void logout(String email) {
        loggedInUsers.remove(email);
    }

    public synchronized boolean isRegistered(String email) {
        return db.userExists(email); // Check DB
    }

    public synchronized boolean isLoggedIn(String email) {
        return loggedInUsers.containsKey(email);
    }

    public synchronized ClientHandler getClientHandler(String email) {
        return loggedInUsers.get(email);
    }

    public synchronized void addPendingMessage(String email, String message) {
        pendingMessages.computeIfAbsent(email, k -> new ArrayList<>()).add(message);
    }

    public synchronized List<String> retrievePendingMessages(String email) {
        List<String> messages = pendingMessages.getOrDefault(email, new ArrayList<>());
        pendingMessages.remove(email);
        return messages;
    }

    public synchronized Set<String> getLoggedInUsers() {
        return new HashSet<>(loggedInUsers.keySet());
    }
    public synchronized Set<String> getRegisteredUsers() {
        return db.getAllUsers();
    }
}
