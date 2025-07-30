package server.database;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class UserDatabase {
    private static final String DB_URL = "jdbc:sqlite:ensachatapp.db";

    public UserDatabase() {
        createUsersTable();
    }

    private void createUsersTable() {
        String sql = "CREATE TABLE IF NOT EXISTS users ("
                   + "email TEXT PRIMARY KEY NOT NULL"
                   + ");";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Register new user email (returns true if added, false if already exists)
    public boolean registerUser(String email) {
        String checkSql = "SELECT email FROM users WHERE email = ?";
        String insertSql = "INSERT INTO users(email) VALUES(?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement checkStmt = conn.prepareStatement(checkSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

            checkStmt.setString(1, email);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                // User already registered
                return false;
            } else {
                insertStmt.setString(1, email);
                insertStmt.executeUpdate();
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Check if user exists
    public boolean userExists(String email) {
        String sql = "SELECT email FROM users WHERE email = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            return rs.next();

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
        public Set<String> getAllUsers() {
        Set<String> users = new HashSet<>();
        String sql = "SELECT email FROM users";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(rs.getString("email"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
}
