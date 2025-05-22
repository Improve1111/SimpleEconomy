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
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT uuid, balance FROM balances ORDER BY balance DESC LIMIT ?")) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                top.put(uuid, rs.getDouble("balance"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return top;
    }
}
