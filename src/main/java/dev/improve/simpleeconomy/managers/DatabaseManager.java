package dev.improve.simpleeconomy.managers;

import dev.improve.simpleeconomy.SimpleEconomy;

import java.io.File;
import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private final SimpleEconomy plugin;
    private Connection connection;

    public DatabaseManager(SimpleEconomy plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "balances.db");
            String url = "jdbc:sqlite:" + dbFile;
            connection = DriverManager.getConnection(url);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS balances (" +
                        "uuid TEXT PRIMARY KEY," +
                        "balance REAL NOT NULL)");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean hasBalance(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT uuid FROM balances WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public double getBalance(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT balance FROM balances WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("balance");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return plugin.getConfig().getDouble("settings.start-balance", 0.0);
    }

    public void setBalance(UUID uuid, double amount) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO balances (uuid, balance) VALUES (?, ?) ON CONFLICT(uuid) DO UPDATE SET balance = ?")) {
            ps.setString(1, uuid.toString());
            ps.setDouble(2, amount);
            ps.setDouble(3, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<UUID, Double> getTopBalances(int limit) {
        Map<UUID, Double> top = new LinkedHashMap<>();
        String query = "SELECT uuid, balance FROM balances ORDER BY balance DESC LIMIT ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("Executing query: " + query + " with limit: " + limit); // Debug

                int count = 0;
                while (rs.next()) {
                    try {
                        String uuidString = rs.getString("uuid");
                        double balance = rs.getDouble("balance");

                        System.out.println("Found record: " + uuidString + " = " + balance); // Debug

                        UUID uuid = UUID.fromString(uuidString);
                        top.put(uuid, balance);
                        count++;
                    } catch (IllegalArgumentException e) {
                        System.err.println("Invalid UUID format: " + rs.getString("uuid"));
                        continue;
                    }
                }
                System.out.println("Total records found: " + count); // Debug
            }
        } catch (SQLException e) {
            System.err.println("SQL Error in getTopBalances: " + e.getMessage());
            e.printStackTrace();
        }
        return top;
    }
}
