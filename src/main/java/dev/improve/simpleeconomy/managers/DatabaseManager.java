package dev.improve.simpleeconomy.managers;

import dev.improve.simpleeconomy.SimpleEconomy;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings("CallToPrintStackTrace")
public class DatabaseManager {
    private final Map<UUID, Double> balances = new HashMap<>();
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

    public void getBalanceAsync(UUID uuid, Consumer<Double> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            double balance = getBalance(uuid);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                callback.accept(balance);
            });
        });
    }

    public void setBalance(UUID uuid, double amount) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO balances (uuid, balance) VALUES (?, ?) ON CONFLICT(uuid) DO UPDATE SET balance = ?")) {
            ps.setString(1, uuid.toString());
            ps.setDouble(2, amount);
            ps.setDouble(3, amount);
            ps.executeUpdate();
            balances.put(uuid, amount);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addBalance(UUID uuid, double amount) {
        double currentBalance = getBalance(uuid);
        setBalance(uuid, currentBalance + amount);
    }

    public void removeBalance(UUID uuid, double amount) {
        double currentBalance = getBalance(uuid);
        setBalance(uuid, Math.max(0, currentBalance - amount));
    }

    public boolean hasEnoughBalance(UUID uuid, double amount) {
        return getBalance(uuid) >= amount;
    }

    public Map<UUID, Double> getTopBalances(int limit) {
        Map<UUID, Double> top = new LinkedHashMap<>();
        String query = "SELECT uuid, balance FROM balances ORDER BY balance DESC LIMIT ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        String uuidString = rs.getString("uuid");
                        double balance = rs.getDouble("balance");
                        UUID uuid = UUID.fromString(uuidString);
                        top.put(uuid, balance);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return top;
    }

    public List<UUID> getAllPlayers() {
        List<UUID> players = new ArrayList<>();
        String query = "SELECT uuid FROM balances";

        try (PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    players.add(uuid);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return players;
    }

    public void deleteBalance(UUID uuid) {
        balances.remove(uuid);
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM balances WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public double getTotalEconomy() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT SUM(balance) as total FROM balances");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public int getPlayerCount() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) as count FROM balances");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public Map<UUID, Double> getBalances() {
        return balances;
    }
}